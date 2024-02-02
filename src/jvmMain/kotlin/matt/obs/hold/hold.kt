@file:OptIn(InternalSerializationApi::class)

package matt.obs.hold

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.serializer
import matt.lang.anno.Open
import matt.lang.err
import matt.obs.hold.custom.CustomDecoder
import matt.obs.hold.custom.CustomSerializer
import matt.obs.hold.custom.ElementDecoder
import matt.obs.hold.extra.MetaProp
import matt.obs.prop.typed.AbstractTypedObsList
import matt.obs.prop.typed.AbstractTypedObsSet
import matt.obs.prop.typed.TypedBindableProperty
import matt.obs.prop.typed.TypedSerializableElement
import matt.prim.str.elementsToString
import kotlin.reflect.KClass


class MyCustomDecoder<T : TypedObservableHolder>(
    getNewInstance: () -> T,
) : CustomDecoder<T> {
    var obj = getNewInstance()
    val observables = obj.namedObservables()
    var gotReplacement = false
    override fun finishDecoding(): T = obj
}


abstract class ElementDecoderImpl<T : TypedObservableHolder, V>(
    @Open override val key: String,
    serializer: KSerializer<V>,
    @Open override val isOptional: Boolean,
) : ElementDecoder<V, T, MyCustomDecoder<T>>(serializer)

@OptIn(InternalSerializationApi::class)
open class TypedObsHolderSerializer<T : TypedObservableHolder>(
    private val cls: KClass<out T>,
) : CustomSerializer<T, MyCustomDecoder<T>, ElementDecoderImpl<T, *>>() {

    @Suppress("UNCHECKED_CAST")
    protected fun newInstance(): T {
        val constructors = cls.java.constructors
        val goodConstructor = constructors.firstOrNull {
            it.parameterCount == 0
        } ?: error(
            "Cannot find no-arg constructor for $cls, constructors have ${
                constructors.map { it.parameterCount }.elementsToString()
            } parameters"
        )
        return goodConstructor.newInstance() as T
    }

    private val exampleInstance by lazy {
        newInstance()
    }


    final override fun newDecoder() = MyCustomDecoder<T>(
        getNewInstance = { newInstance() }
    )

    internal inner class MyCustomElement<V>(
        override val key: String,
        override val serializer: KSerializer<V>,
        override val isOptional: Boolean,
    ) : ElementDecoderImpl<T, V>(key = key, serializer = serializer, isOptional = isOptional) {

        override fun handleLoadedValue(
            v: V,
            customDecoder: MyCustomDecoder<T>
        ) {
            val observable = customDecoder.observables[key]
            @Suppress("UNCHECKED_CAST")
            (observable as TypedSerializableElement<V>).setFromEncoded(v)
        }

        override fun getCurrentValue(obj: T): V {
            @Suppress("UNCHECKED_CAST")
            return obj.namedObservables()[key]!!.provideEncodable() as V
        }

        override fun shouldEncode(v: V): Boolean {
            return true /*we are encoding defaults here, yes*/
        }

        override fun handleNotFound(customDecoder: MyCustomDecoder<T>) {
            error("I am pretty sure this should never happen with this current implementation")
        }

    }


    final    override val serialName = cls.qualifiedName!!
    final override val elements by lazy {
        buildList {
            addAll(metaProps)
            val metaKeys = metaProps.map { it.key }
            exampleInstance.namedObservables().forEach {

                if (it.key in metaKeys) {
                    err("property can not have the name ${it.key}, which is used as a meta property")
                }

                when (val theValue = it.value) {
                    is TypedBindableProperty<*> -> {
                        val cls = theValue.cls
                        val ser = serializer(
                            cls,
                            listOf(),
                            theValue.nullable
                        )
                        add(
                            MyCustomElement(
                                key = it.key, serializer = ser, isOptional = true
                            )
                        )
                    }

                    is AbstractTypedObsList<*>  -> {
                        val cls = theValue.elementCls
                        val elementSerializer =  serializer(
                            cls,
                            run {
                                check(cls.typeParameters.isEmpty())
                                listOf()
                            },
                            isNullable = theValue.nullableElements
                        )
                        add(
                            MyCustomElement(
                                key = it.key,
                                serializer = ListSerializer(elementSerializer),
                                isOptional = true
                            )
                        )
                    }

                    is AbstractTypedObsSet<*>   -> {
                        val cls = theValue.elementCls
                        val elementSer =serializer(
                            cls,
                            run {
                                check(cls.typeParameters.isEmpty())
                                listOf()
                            },
                            isNullable = theValue.nullableElements
                        )
                        add(
                            MyCustomElement(
                                key = it.key, serializer = SetSerializer(elementSer), isOptional = true
                            )
                        )
                    }
                }
            }
        }
    }

    internal open val metaProps: List<MetaProp<T, *>> = listOf()
    private val indexedMetaProps by lazy {
        metaProps.withIndex().associate { it.index to it.value }
    }


}


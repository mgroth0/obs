@file:OptIn(InternalSerializationApi::class)

package matt.obs.hold.j

import kotlinx.serialization.InternalSerializationApi
import matt.obs.common.ElementDecoderImpl
import matt.obs.common.MetaProp
import matt.obs.common.MyCustomDecoderAndEncoder
import matt.obs.hold.TypedObservableHolder
import matt.obs.hold.custom.CustomSerializer
import matt.prim.str.elementsToString
import kotlin.reflect.KClass
import kotlin.reflect.cast


open class TypedObsHolderSerializer<T : TypedObservableHolder>(
    private val cls: KClass<out T>
) : CustomSerializer<T, MyCustomDecoderAndEncoder<T>, ElementDecoderImpl<*>>() {


    protected fun newInstance(): T {
        val constructors = cls.java.constructors
        val goodConstructor =
            constructors.firstOrNull {
                it.parameterCount == 0
            } ?: error(
                "Cannot find no-arg constructor for $cls, constructors have ${
                    constructors.map { it.parameterCount }.elementsToString()
                } parameters"
            )
        return cls.cast(goodConstructor.newInstance())
    }

    private val exampleInstance by lazy {
        newInstance()
    }


    final override fun newDecoder() =
        run {
            val instance = newInstance()
            MyCustomDecoderAndEncoder<T>(
                instance
            ) { metaProps(it) }
        }

    final override fun newEncoder(value: T): MyCustomDecoderAndEncoder<T> = MyCustomDecoderAndEncoder(value) { metaProps(it) }

    final override val serialName = cls.qualifiedName!!


    internal open fun metaProps(t: MyCustomDecoderAndEncoder<T>): List<MetaProp<T, *>> = listOf()
    /*private val indexedMetaProps by lazy {
        metaProps.withIndex().associate { it.index to it.value }
    }*/
}







package matt.obs.prop.typed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import matt.collect.mapToSet
import matt.obs.col.BasicOCollection
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.oset.BasicObservableSet
import matt.obs.col.oset.MutableObsSet
import matt.obs.col.oset.ObsSet
import matt.obs.common.MObservable
import matt.obs.common.MyCustomElement
import matt.obs.prop.writable.BindableProperty
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass
import kotlin.reflect.cast

/*
@OptIn(ExperimentalSerializationApi::class)
class TypedBindablePropertySerializer<T: Any>(private val dataSerializer: KSerializer<T>) :
    KSerializer<TypedBindableProperty<T>> {

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: TypedBindableProperty<T>
    ) {
        dataSerializer.serialize(encoder, value.value)
    }

    override fun deserialize(decoder: Decoder): TypedBindableProperty<T> {
        val value = dataSerializer.deserialize(decoder)
        val valueDescriptor = dataSerializer.descriptor
        val cls =
            with(systemClassGetter()) {
                val serialName = valueDescriptor.serialName.removeSuffix("?")




? at the end of a serialDescriptor means it is nullable


                JvmQualifiedClassName(serialName).get() ?: error("Could not get class for serial name: $serialName")
            }
        return TypedBindableProperty<T>(
            cls = cls,
            nullable = valueDescriptor.isNullable,
            value = value
        )
    }
}


object StringTypedBindablePropertySerializer: KSerializer<MaybeNullableTypedBindableProperty<String>> {
    override val descriptor = serialDescriptor<String>()

    override fun deserialize(decoder: Decoder): MaybeNullableTypedBindableProperty<String> = typedBindableProperty(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: MaybeNullableTypedBindableProperty<String>
    ) {
        encoder.encodeString(value.value)
    }
}
class NullableTypedBindablePropertySerializer<T>(
    private val nonNullValueSer: KSerializer<T & Any>
): KSerializer<MaybeNullableTypedBindableProperty<T>> {
    override val descriptor = nonNullValueSer.descriptor

    override fun deserialize(decoder: Decoder): MaybeNullableTypedBindableProperty<T> =
        typedBindableProperty<T & Any>(
            decoder.decodeNullableSerializableValue(nonNullValueSer)
        )

    override fun serialize(
        encoder: Encoder,
        value: MaybeNullableTypedBindableProperty<T>
    ) {
        encoder.encodeNullableSerializableValue(nonNullValueSer, value.value)
    }
}*/

class TypedBindablePropertySerializer<T>(
    private val valueSer: KSerializer<T>
): KSerializer<TypedBindableProperty<T>> {
    override val descriptor = valueSer.descriptor

    override fun deserialize(decoder: Decoder): TypedBindableProperty<T> =
        run {
            val v = decoder.decodeSerializableValue(valueSer)
            NotCastingTypedBindableProperty(
                value = v,
                serializer = valueSer
            )
        }


    override fun serialize(
        encoder: Encoder,
        value: TypedBindableProperty<T>
    ) {
        encoder.encodeSerializableValue(valueSer, value.value)
    }
}

inline fun <reified T> typedBindableProperty(value: T): TypedBindableProperty<T> =
    CastingTypedBindableProperty(
        serializer<T>(),
        value,
        /*nullable = null is T,*/
        cast = { it as T }
    )
/*
inline fun <reified T: Any> nonNullabletypedBindableProperty(value: T) =
    NonNullableTypedBindableProperty(serializer<T>(), value) {
        it as T
    }
inline fun <reified T: Any> nullableTypedBindableProperty(value: T?) =
    NullableTypedBindableProperty(serializer<T>(), value) {
        if (it == null) it
        else it as T
    }
*/


abstract class TypedBindableProperty<T>(
    val serializer: KSerializer<T>,
    value: T
): BindableProperty<T>(value)

class NotCastingTypedBindableProperty<T>(
    serializer: KSerializer<T>,
    value: T
) : TypedBindableProperty<T>(serializer, value)


class CastingTypedBindableProperty<T>(
    serializer: KSerializer<T>,
    value: T,
    val cast: (Any?) -> T
) : TypedBindableProperty<T>(serializer, value), TypedSerializableElement<T> {

    /*override fun provideEncodable(): NonNull<T> = NonNull(value)*/
    override fun provideEncodable(): TypedNullable<T & Any> =
        when (val v = value) {
            null -> Null
            else -> NonNull(v)
        }

    override fun setFromEncoded(loadedValue: Any?) {
        value = cast(loadedValue)
    }

    override fun customElement(key: String) =
        MyCustomElement(
            key = key,
            serializer = serializer,
            isOptional = true,
            convertCastCurrentValue = cast,
            observable = this
        )
}

/*

class NonNullableTypedBindableProperty<T: Any>(
    serializer: KSerializer<T>,
    value: T,
    private val cast: (Any?) -> T
) : TypedBindableProperty<T>(serializer, value) {

    override val nullable = false

    override fun provideEncodable(): NonNull<T> = NonNull(value)


    override fun setFromEncoded(loadedValue: Any?) {
        value = cast(loadedValue)
    }
}
*/

sealed interface TypedNullable<out T: Any>
@JvmInline
value class NonNull<T: Any>(val value: T): TypedNullable<T>
data object Null: TypedNullable<Nothing>

/*class NullableTypedBindableProperty<T: Any>(
    serializer: KSerializer<T>,
    value: T?,
    private val cast: (Any?) -> T?
) : TypedBindableProperty<T?>(serializer, value) {

    override val nullable = true

    override fun provideEncodable(): TypedNullable<T> =
        when (val v = value) {
            null -> Null
            else -> NonNull(v)
        }


    override fun setFromEncoded(loadedValue: Any?) {
        if (loadedValue == null) {
            value = null
        }
        value = cast(loadedValue)
    }
}*/


class TypedMutableObsList<E: Any>(
    elementCls: KClass<E>,
    elementSer: KSerializer<E>,
    nullableElements: Boolean,
    list: MutableObsList<E>
) :
    AbstractTypedObsList<E>(
            elementCls = elementCls,
            nullableElements = nullableElements,
            list = list,
            elementSer = elementSer
        ),
        MutableObsList<E> by list

class TypedImmutableObsList<E: Any>(
    elementCls: KClass<E>,
    elementSer: KSerializer<E>,
    nullableElements: Boolean,
    list: ImmutableObsList<E>
) : AbstractTypedObsList<E>(
        elementCls = elementCls,
        nullableElements = nullableElements,
        list = list,
        elementSer = elementSer
    ),
    ImmutableObsList<E> by list

sealed class AbstractTypedObsList<E: Any>(
    elementCls: KClass<E>,
    elementSer: KSerializer<E>,
    nullableElements: Boolean,
    private val list: ImmutableObsList<E>
) : AbstractTypedObsCollection<E, List<E>>(elementCls = elementCls, nullableElements = nullableElements, col = list, elementSer = elementSer) {

    final override val colSer by lazy {
        ListSerializer(elementSer)
    }


    final override fun setFromEncoded(loadedValue: Any?/*Collection<E>*/) {
        (list as MutableObsList<E>).apply {
            clear()
            (loadedValue as Collection<*>).forEach {
                add(this@AbstractTypedObsList.elementCls.cast(it))
            }
        } /*.setAll(loadedValue)*/
    }

    final override fun customElement(key: String): MyCustomElement<List<E>> =
        MyCustomElement(
            key = key,
            serializer = ListSerializer(elementSer),
            isOptional = true,
            /*cls = cls,*/
            convertCastCurrentValue = {
                (it as List<*>).map {
                    elementCls.cast(it)
                }
            },
            observable = this
        )
    final override fun provideEncodable() = NonNull(list)
}


class TypedMutableObsSet<E: Any>(
    elementCls: KClass<E>,
    elementSer: KSerializer<E>,
    nullableElements: Boolean,
    set: MutableObsSet<E>
) : AbstractTypedObsSet<E>(
        elementCls = elementCls,
        nullableElements = nullableElements,
        set = set,
        elementSer = elementSer
    ),
    MutableObsSet<E> by set

class TypedImmutableObsSet<E: Any>(
    elementCls: KClass<E>,
    elementSer: KSerializer<E>,
    nullableElements: Boolean,
    set: ObsSet<E>
) : AbstractTypedObsSet<E>(
        elementCls = elementCls,
        nullableElements = nullableElements,
        set = set,
        elementSer = elementSer
    ),
    ObsSet<E> by set


sealed class AbstractTypedObsSet<E: Any>(
    elementCls: KClass<E>,
    elementSer: KSerializer<E>,
    nullableElements: Boolean,
    private val set: ObsSet<E>
) : AbstractTypedObsCollection<E, Set<E>>(elementCls = elementCls, nullableElements = nullableElements, col = set, elementSer = elementSer) {

    final override val colSer by lazy {
        SetSerializer(elementSer)
    }


    final override fun setFromEncoded(loadedValue: Any?/*Collection<E>*/) {
        (set as BasicObservableSet<E>).apply {
            clear()
            (loadedValue as Collection<*>).forEach {
                add(this@AbstractTypedObsSet.elementCls.cast(it))
            }
        } /*setAll(loadedValue)*/
    }

    final override fun customElement(key: String): MyCustomElement<Set<E>> =
        MyCustomElement(
            key = key,
            serializer = SetSerializer(elementSer),
            isOptional = true,
            /*cls = cls,*/
            convertCastCurrentValue = {
                (it as Set<*>).mapToSet {
                    elementCls.cast(it)
                }
                /*cls.cast(it)*/
            },
            observable = this
        )
    final override fun provideEncodable() = NonNull(set)
}


sealed class AbstractTypedObsCollection<E: Any, C: Collection<E>>(
    val elementCls: KClass<E>,
    val elementSer: KSerializer<E>,
    val nullableElements: Boolean,
    protected val col: BasicOCollection<E, *, *, *>
) : TypedSerializableElement<C> {

    protected abstract val colSer: KSerializer<*>
}


sealed interface TypedSerializableElement<V> : MObservable {

    fun provideEncodable(): TypedNullable<V & Any>

    fun setFromEncoded(loadedValue: Any?)

    fun customElement(key: String): MyCustomElement<V>
}




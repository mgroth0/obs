@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

package matt.obs.prop.typed

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import matt.lang.nametoclass.bootStrapClassForNameCache
import matt.lang.setall.setAll
import matt.obs.MObservable
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.prop.BindableProperty
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
class TypedBindablePropertySerializer<T>(private val dataSerializer: KSerializer<T>) :
    KSerializer<TypedBindableProperty<T>> {

    companion object {
        private var classCache = bootStrapClassForNameCache()
        fun clearClassCache() {
            classCache = bootStrapClassForNameCache()
        }
    }

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun serialize(encoder: Encoder, value: TypedBindableProperty<T>) {
        dataSerializer.serialize(encoder, value.value)
    }

    override fun deserialize(decoder: Decoder): TypedBindableProperty<T> {
        val value = dataSerializer.deserialize(decoder)
        val valueDescriptor = dataSerializer.descriptor
        val cls = classCache[valueDescriptor.serialName]
        return TypedBindableProperty(
            cls = cls,
            nullable = valueDescriptor.isNullable,
            value = value
        )
    }
}

inline fun <reified T> typedBindableProperty(value: T) = TypedBindableProperty(T::class, null is T, value)

@OptIn(InternalSerializationApi::class)
@Serializable(with = TypedBindablePropertySerializer::class)
class TypedBindableProperty<T>(val cls: KClass<*>, val nullable: Boolean, value: T) : BindableProperty<T>(value),
    TypedSerializableElement {

    private val ser by lazy {
        cls.serializer()
    }

    override fun encode(
        encoder: CompositeEncoder,
        descriptor: SerialDescriptor,
        index: Int
    ) {
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableElement(
            descriptor = descriptor,
            index = index,
            serializer = ser as SerializationStrategy<T>,
            value = value
        )
    }

    override fun decode(decoder: CompositeDecoder, descriptor: SerialDescriptor, index: Int) {
        @Suppress("UNCHECKED_CAST")
        val loadedValue = decoder.decodeSerializableElement(
            descriptor = descriptor,
            index = index,
            deserializer = ser as DeserializationStrategy<T>
        )
        value = loadedValue
    }
}


class TypedMutableObsList<E>(elementCls: KClass<*>, nullableElements: Boolean, list: MutableObsList<E>) :
    AbstractTypedObsList<E>(
        elementCls = elementCls,
        nullableElements = nullableElements,
        list = list
    ), MutableObsList<E> by list

class TypedImmutableObsList<E>(
    elementCls: KClass<*>,
    nullableElements: Boolean,
    list: ImmutableObsList<E>
) : AbstractTypedObsList<E>(
    elementCls = elementCls,
    nullableElements = nullableElements,
    list = list
), ImmutableObsList<E> by list

@OptIn(InternalSerializationApi::class)
sealed class AbstractTypedObsList<E>(
    val elementCls: KClass<*>,
    val nullableElements: Boolean,
    private val list: ImmutableObsList<E>
) : TypedSerializableElement {
    private val elementSer by lazy {
        elementCls.serializer()
    }

    private val listSer by lazy {
        ListSerializer(elementSer)
    }

    override fun encode(encoder: CompositeEncoder, descriptor: SerialDescriptor, index: Int) {
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableElement(
            descriptor = descriptor,
            index = index,
            serializer = listSer as SerializationStrategy<List<E>>,
            value = list
        )
    }

    override fun decode(decoder: CompositeDecoder, descriptor: SerialDescriptor, index: Int) {
        @Suppress("UNCHECKED_CAST")
        val loadedValue = decoder.decodeSerializableElement(
            descriptor = descriptor,
            index = index,
            deserializer = listSer as DeserializationStrategy<List<E>>
        )
        (list as MutableObsList<E>).setAll(loadedValue)
    }
}


sealed interface TypedSerializableElement : MObservable {
    fun encode(
        encoder: CompositeEncoder,
        descriptor: SerialDescriptor,
        index: Int
    )

    fun decode(decoder: CompositeDecoder, descriptor: SerialDescriptor, index: Int)
}




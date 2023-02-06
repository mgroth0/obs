@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

package matt.obs.prop.typed

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import matt.obs.prop.BindableProperty
import matt.reflect.classForName
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class TypedBindablePropertySerializer<T>(private val dataSerializer: KSerializer<T>):
  KSerializer<TypedBindableProperty<T>> {
  override val descriptor: SerialDescriptor = dataSerializer.descriptor

  override fun serialize(encoder: Encoder, value: TypedBindableProperty<T>) {
	/*@Suppress("UNCHECKED_CAST")*/
	/*(encoder as JsonEncoder).encodeJsonElement(
	  dataSerializer.serialize(value.value)
	  Json.encodeToJsonElement(dataSerializer, value.value)
	  *//*Json.encodeToJsonElement((value.cls).serializer() as KSerializer<T>, value.value)*//*
	)*/

	dataSerializer.serialize(encoder, value.value)
  }

  override fun deserialize(decoder: Decoder): TypedBindableProperty<T> {
	/*val jsonDecoder = decoder as JsonDecoder*/
	/*val element = jsonDecoder.decodeJsonElement()*/
	val value = dataSerializer.deserialize(decoder)

	val valueDescriptor = dataSerializer.descriptor

	val cls = classForName(valueDescriptor.serialName)

//	println("value=${value}")
//	println("valueDescriptor.serialName=${valueDescriptor.serialName}")
//	println("cls=${cls}")
	val goodClass = cls as KClass<*>

	return TypedBindableProperty(
	  cls = goodClass,
	  nullable = valueDescriptor.isNullable,
	  value = value
	)
  }
}

inline fun <reified T> typedBindableProperty(value: T) = TypedBindableProperty(T::class, null is T, value)

@OptIn(InternalSerializationApi::class)
@Serializable(with = TypedBindablePropertySerializer::class)
class TypedBindableProperty<T>(val cls: KClass<*>, val nullable: Boolean, value: T): BindableProperty<T>(value) {
  fun encode(encoder: CompositeEncoder, index: Int) {
	val ser = cls.serializer()
	@Suppress("UNCHECKED_CAST")
	encoder.encodeSerializableElement<T>(
	  descriptor = ser.descriptor,
	  index = index,
	  serializer = ser as SerializationStrategy<T>,
	  value = value
	)
  }

  fun decode(decoder: CompositeDecoder, index: Int) {
	val ser = cls.serializer()
	@Suppress("UNCHECKED_CAST") val loadedValue = decoder.decodeSerializableElement<T>(
	  descriptor = ser.descriptor,
	  index = index,
	  deserializer = ser as DeserializationStrategy<T>
	)
	value = loadedValue
  }
}
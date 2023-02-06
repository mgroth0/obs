@file:OptIn(InternalSerializationApi::class)

package matt.obs.hold

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import matt.lang.err
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
open class TypedObsHolderSerializer<T: TypedObservableHolder>(
  private val cls: KClass<T>
): KSerializer<T> {


  /*
	override fun deserialize(jsonObject: JsonObject): T {
	  return cls.createInstance().apply {
		namedObservables().forEach { (k, v) ->
		  jsonObject[k]?.let { */
  /*null should be JsonNull here, so nullables should be ok*//*

		  v::value.set(Json.decodeFromJsonElement(v.cls.serializer(), it))
		}
	  }
	}
  }
*/

  private val exampleInstance by lazy {
	cls.createInstance()
  }

  override val descriptor: SerialDescriptor by lazy {
	buildClassSerialDescriptor(cls.qualifiedName!!) {
	  exampleInstance.namedObservables().forEach {
		element(
		  elementName = it.key,
		  descriptor = it.value.cls.serializer().descriptor,
		  isOptional = true
		)
	  }
	}
  }

  override fun deserialize(decoder: Decoder): T {
	return cls.createInstance().apply {

	  decoder.decodeStructure(descriptor) {
		val observables = namedObservables()
		while (true) {
		  val index = decodeElementIndex(descriptor)
		  when {
			index >= 0            -> {
			  val key = decodeStringElement(String::class.serializer().descriptor, index = index)
			  val prop = observables[key]!!
			  prop.decode(this, index = index + 1)
			}

			index == DECODE_DONE  -> break /*Input is over*/
			index == UNKNOWN_NAME -> err("unknown name?")
			else                  -> error("Unexpected index: $index")
		  }
		}

	  }


	  /*
			namedObservables().forEach { (k, v) ->
			  jsonObject[k]?.let { *//*null should be JsonNull here, so nullables should be ok*//*
		  v::value.set(Json.decodeFromJsonElement(v.cls.serializer(), it))
		}
	  }*/


	}
  }

  override fun serialize(encoder: Encoder, value: T) {
	encoder.encodeStructure(descriptor) {
	  var i = 0
	  value.namedObservables().entries.forEach { it ->
		encodeStringElement(descriptor = String::class.serializer().descriptor, index = i++, value = it.key)
		it.value.encode(this, index = i++)
	  }
	}

  }


}
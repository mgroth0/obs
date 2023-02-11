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
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.serializer
import matt.lang.err
import matt.log.warn.warn
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
open class TypedObsHolderSerializer<T: TypedObservableHolder>(
  private val cls: KClass<out T>,
  private val classVersion: Int
): KSerializer<T> {

  val classVersionKey = "classVersion"

  @Suppress("UNCHECKED_CAST")
  private fun newInstance(): T = cls.java.constructors[0].newInstance() as T

  private val exampleInstance by lazy {
	newInstance()
  }

  override val descriptor: SerialDescriptor by lazy {
	buildClassSerialDescriptor(cls.qualifiedName!!) {
	  element(
		elementName = classVersionKey,
		descriptor = Int::class.serializer().descriptor,
		isOptional = true
	  )
	  exampleInstance.namedObservables().forEach {
		if (it.key == classVersionKey) {
		  err("property can not have the name $classVersionKey")
		}
		element(
		  elementName = it.key,
		  descriptor = it.value.cls.serializer().descriptor,
		  isOptional = true
		)
	  }
	}
  }

  override fun deserialize(decoder: Decoder): T {

	var obj = newInstance()
	var checkedClassVersion = false

	var stopLoading = false

	val compositeDecoder = decoder.beginStructure(descriptor)

	val observables = obj.namedObservables()
	while (true) {
	  val index = compositeDecoder.decodeElementIndex(descriptor)
	  when {

		index == 0            -> {
		  checkedClassVersion = true
		  val loadedClassVersion = compositeDecoder.decodeIntElement(descriptor, index)
		  if (loadedClassVersion != classVersion) {
			obj = newInstance().apply {
			  warn("class version not compatible! ($loadedClassVersion!=$classVersion)")
			  wasResetBecauseSerializedDataWasWrongClassVersion = true
			}
			stopLoading = true
			/*break*/
		  }
		}

		index >= 1            -> {

		  if (stopLoading) {

			(compositeDecoder as JsonDecoder).decodeJsonElement()

		  } else {
			val key = descriptor.getElementName(index)
			val prop = observables[key]!!
			prop.decode(compositeDecoder, descriptor, index = index)
		  }


		}


		index == DECODE_DONE  -> break
		index == UNKNOWN_NAME -> err("unknown name?")
		else                  -> error("Unexpected index: $index")
	  }
	}

	/*if (!stopLoading) {*/
	compositeDecoder.endStructure(descriptor)
	/*}*/


	if (!checkedClassVersion) {
	  warn("did not check class version!")
	  obj = newInstance().apply {
		wasResetBecauseSerializedDataWasWrongClassVersion = true
	  }
	}
	return obj
  }

  override fun serialize(encoder: Encoder, value: T) {
	println("serializing with classVersion=$classVersion")
	encoder.encodeStructure(descriptor) {
	  var i = 0
	  encodeIntElement(descriptor, i++, classVersion)
	  value.namedObservables().entries.forEach {
		it.value.encode(this, descriptor, index = i++)
	  }
	}

  }


}
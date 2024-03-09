package matt.obs.hold.custom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import matt.lang.anno.Open
import matt.lang.common.err

abstract class CustomSerializer<T, D : CustomDecoder<T>, E : ElementDecoder<*, T, D>> : KSerializer<T> {
    abstract val serialName: String
    final override val descriptor: SerialDescriptor by lazy {
        buildClassSerialDescriptor(serialName) {

            elements.forEach {
                element(
                    elementName = it.key,
                    descriptor = it.serializer.descriptor,
                    isOptional = it.isOptional
                )
            }
        }
    }

    abstract val elements: List<E>


    final override fun deserialize(decoder: Decoder): T {

        val customDecoder = newDecoder()
        val compositeDecoder = decoder.beginStructure(descriptor)
        val elementsNotFound = elements.toMutableList()
        while (true) {
            val index = compositeDecoder.decodeElementIndex(descriptor)
            when {
                index >= 0                             -> {
                    val element = elements[index]
                    require(element in elementsNotFound)
                    elementsNotFound.remove(element)
                    element.load(descriptor, compositeDecoder, index, customDecoder)
                }

                index == CompositeDecoder.DECODE_DONE  -> break
                index == CompositeDecoder.UNKNOWN_NAME -> err("unknown name?")
                else                                   -> error("Unexpected index: $index")
            }
        }

        elementsNotFound.forEach {
            it.handleNotFound(customDecoder)
        }

        compositeDecoder.endStructure(descriptor)

        return customDecoder.finishDecoding()
    }

    abstract fun newDecoder(): D


    final override fun serialize(
        encoder: Encoder,
        value: T
    ) {
        encoder.encodeStructure(descriptor) {
            var i = 0
            elements.forEach {
                it.save(
                    descriptor = descriptor,
                    index = i++,
                    encoder = this,
                    obj = value
                )
            }
        }
    }
}


interface Element<V, D> {
    val key: String
    val serializer: KSerializer<V>

    /*not sure if this even matters, since I am handling questions regarding this manually (handleNotFound,shouldEncode)*/
    val isOptional: Boolean
}

abstract class ElementDecoder<V, T, D : CustomDecoder<T>>(
    @Open override val serializer: KSerializer<V>
) : Element<V, D> {
    fun load(
        descriptor: SerialDescriptor,
        decoder: CompositeDecoder,
        index: Int,
        customDecoder: D
    ) {
        val loadedValue = decoder.decodeSerializableElement(descriptor, index, serializer)
        handleLoadedValue(loadedValue, customDecoder)
    }

    abstract fun shouldEncode(v: V): Boolean

    fun save(
        descriptor: SerialDescriptor,
        encoder: CompositeEncoder,
        index: Int,
        obj: T
    ) {
        val currentValue = getCurrentValue(obj)
        if (shouldEncode(currentValue)) encoder.encodeSerializableElement(
            descriptor = descriptor,
            index = index,
            serializer = serializer,
            value = currentValue
        )
    }

    abstract fun handleLoadedValue(
        v: V,
        customDecoder: D
    )

    abstract fun getCurrentValue(obj: T): V

    abstract fun handleNotFound(customDecoder: D)
}

interface CustomDecoder<T> {
    fun finishDecoding(): T
}

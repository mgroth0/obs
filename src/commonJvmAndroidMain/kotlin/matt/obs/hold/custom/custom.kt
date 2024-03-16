package matt.obs.hold.custom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import matt.lang.common.err
import matt.obs.common.CustomDecoderAndEncoder
import matt.obs.common.custom.ElementDecoder

abstract class CustomSerializer<T, D : CustomDecoderAndEncoder<T, E>, E : ElementDecoder<*>> :
    KSerializer<T> {
    abstract val serialName: String
    final override val descriptor: SerialDescriptor by lazy {
        buildClassSerialDescriptor(serialName) {
            newDecoder().elements.forEach {
                element(
                    elementName = it.key,
                    descriptor = it.serializer.descriptor,
                    isOptional = it.isOptional
                )
            }
        }
    }




    final override fun deserialize(decoder: Decoder): T {

        val customDecoder = newDecoder()
        val compositeDecoder = decoder.beginStructure(descriptor)
        val elements = customDecoder.elements
        val elementsNotFound = elements.toMutableList()
        while (true) {
            val index = compositeDecoder.decodeElementIndex(descriptor)
            when {
                index >= 0                             -> {
                    val element = elements[index]
                    require(element in elementsNotFound)
                    elementsNotFound.remove(element)
                    element.load(descriptor, compositeDecoder, index)
                }

                index == CompositeDecoder.DECODE_DONE  -> break
                index == CompositeDecoder.UNKNOWN_NAME -> err("unknown name?")
                else                                   -> error("Unexpected index: $index")
            }
        }

        elementsNotFound.forEach {
            it.handleNotFound()
        }

        compositeDecoder.endStructure(descriptor)

        return customDecoder.finishDecoding()
    }


    abstract fun newDecoder(): D
    abstract fun newEncoder(value: T): D


    final override fun serialize(
        encoder: Encoder,
        value: T
    ) {
        val customEncoder = newEncoder(value)
        encoder.encodeStructure(descriptor) {
            var i = 0
            customEncoder.elements.forEach {
                it.save(
                    descriptor = descriptor,
                    index = i++,
                    encoder = this
                )
            }
        }
    }
}






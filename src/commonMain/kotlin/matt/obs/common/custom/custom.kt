package matt.obs.common.custom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import matt.lang.anno.Open


interface Element<V> {
    val key: String
    val serializer: KSerializer<in V>

    /*not sure if this even matters, since I am handling questions regarding this manually (handleNotFound,shouldEncode)*/
    val isOptional: Boolean
}

abstract class ElementDecoder<V>(
    @Open override val serializer: KSerializer<in V>,
    private val cast: (Any?) -> V
) : Element<V> {
    fun load(
        descriptor: SerialDescriptor,
        decoder: CompositeDecoder,
        index: Int
    ) {
        val loadedValue = decoder.decodeSerializableElement(descriptor, index, serializer)
        handleLoadedValue(cast(loadedValue))
    }

    abstract fun shouldEncode(v: V): Boolean

    fun save(
        descriptor: SerialDescriptor,
        encoder: CompositeEncoder,
        index: Int
    ) {
        val currentValue = getCurrentValue()
        if (shouldEncode(currentValue)) encoder.encodeSerializableElement(
            descriptor = descriptor,
            index = index,
            serializer = serializer,
            value = currentValue
        )
    }

    abstract fun handleLoadedValue(
        v: V
    )

    abstract fun getCurrentValue(): V

    abstract fun handleNotFound()
}

package matt.obs.prop.typed

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import matt.classload.systemClassGetter
import matt.lang.classname.JvmQualifiedClassName
import matt.lang.setall.setAll
import matt.obs.MObservable
import matt.obs.col.BasicOCollection
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.oset.BasicObservableSet
import matt.obs.col.oset.MutableObsSet
import matt.obs.col.oset.ObsSet
import matt.obs.prop.BindableProperty
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
class TypedBindablePropertySerializer<T>(private val dataSerializer: KSerializer<T>) :
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
        val cls = with(systemClassGetter()) {
            val serialName = valueDescriptor.serialName.removeSuffix("?") /*? at the end of a serialDescriptor means it is nullable*/
            JvmQualifiedClassName(serialName).get() ?: error("Could not get class for serial name: $serialName")
        }
        return TypedBindableProperty(
            cls = cls,
            nullable = valueDescriptor.isNullable,
            value = value
        )
    }
}

inline fun <reified T> typedBindableProperty(value: T) = TypedBindableProperty(T::class, null is T, value)

@Serializable(with = TypedBindablePropertySerializer::class)
class TypedBindableProperty<T>(
    val cls: KClass<*>,
    val nullable: Boolean,
    value: T
) : BindableProperty<T>(value),
    TypedSerializableElement<T> {


    override fun provideEncodable(): T {
        return value
    }


    override fun setFromEncoded(loadedValue: T) {
        value = loadedValue
    }

}


class TypedMutableObsList<E>(
    elementCls: KClass<*>,
    nullableElements: Boolean,
    list: MutableObsList<E>
) :
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

sealed class AbstractTypedObsList<E>(
    elementCls: KClass<*>,
    nullableElements: Boolean,
    private val list: ImmutableObsList<E>
) : AbstractTypedObsCollection<E>(elementCls = elementCls, nullableElements = nullableElements, col = list) {

    final override val colSer by lazy {
        ListSerializer(elementSer)
    }


    final override fun setFromEncoded(loadedValue: Collection<E>) {
        (list as MutableObsList<E>).setAll(loadedValue)
    }

}


class TypedMutableObsSet<E>(
    elementCls: KClass<*>,
    nullableElements: Boolean,
    set: MutableObsSet<E>
) : AbstractTypedObsSet<E>(
    elementCls = elementCls,
    nullableElements = nullableElements,
    set = set
), MutableObsSet<E> by set

class TypedImmutableObsSet<E>(
    elementCls: KClass<*>,
    nullableElements: Boolean,
    set: ObsSet<E>
) : AbstractTypedObsSet<E>(
    elementCls = elementCls,
    nullableElements = nullableElements,
    set = set
), ObsSet<E> by set


sealed class AbstractTypedObsSet<E>(
    elementCls: KClass<*>,
    nullableElements: Boolean,
    private val set: ObsSet<E>
) : AbstractTypedObsCollection<E>(elementCls = elementCls, nullableElements = nullableElements, col = set) {

    final override val colSer by lazy {
        SetSerializer(elementSer)
    }


    final override fun setFromEncoded(loadedValue: Collection<E>) {
        (set as BasicObservableSet<E>).setAll(loadedValue)
    }
}


@OptIn(InternalSerializationApi::class)
sealed class AbstractTypedObsCollection<E>(
    val elementCls: KClass<*>,
    val nullableElements: Boolean,
    private val col: BasicOCollection<E, *, *, *>
) : TypedSerializableElement<Collection<E>> {
    protected val elementSer by lazy {
        elementCls.serializer()
    }

    protected abstract val colSer: KSerializer<*>

    final override fun provideEncodable() = col

}


sealed interface TypedSerializableElement<V> : MObservable {

    fun provideEncodable(): V

    fun setFromEncoded(loadedValue: V)
}




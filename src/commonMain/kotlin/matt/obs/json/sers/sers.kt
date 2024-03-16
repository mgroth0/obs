package matt.obs.json.sers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import matt.json.oldfx.common.putIfValueNotNull
import matt.json.ser.JsonArraySerializer
import matt.json.ser.JsonObjectSerializer
import matt.json.ser.MyJsonSerializer
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.hold.NamedObsHolder
import matt.obs.prop.writable.BindableProperty
import kotlin.reflect.KClass
import kotlin.reflect.cast

class BindablePropertySerializer<T>(val serializer: KSerializer<T>): MyJsonSerializer<BindableProperty<T>>(
    BindableProperty::class
) {



    override fun deserialize(jsonElement: JsonElement): BindableProperty<T> = BindableProperty(Json.decodeFromJsonElement(serializer, jsonElement))

    override fun serialize(value: BindableProperty<T>): JsonElement = Json.encodeToJsonElement(serializer, value.value)
}

class BasicObservableListImplSerializer<E: Any>(val serializer: KSerializer<in E>, private val elementClass: KClass<E>):
    JsonArraySerializer<BasicObservableListImpl<E>>(BasicObservableListImpl::class) {
    override fun deserialize(jsonArray: JsonArray): BasicObservableListImpl<E> {

        /*THIS UNCHECKED CAST IS NECESSARY BECAUSE KOTLINX.SERIALIZATION DOESN'T PREFORM WELL FOR SEALED CLASS POLYMORPHIC SERIALIZERS WHEN YOU ARE CREATING A LIST OF A SPECIFIC SUBCLASS. NOT USING THE BASE CLASS SERIALIZER LEADS TO AN ERROR. SO AN 'IN' GENERIC AND AN UNCHECKED CASE IS NECESSARY. THIS WOULD BE A GREAT THING TO BRING UP ON THE KOTLINX.SERIALIZATION GITHUB.*/

        return BasicObservableListImpl(
            jsonArray.map {
                elementClass.cast(Json.decodeFromJsonElement(serializer, it))
            }
        )
    }

    override fun serialize(value: BasicObservableListImpl<E>): JsonArray = JsonArray(value.map { Json.encodeToJsonElement(serializer, it) })
}


abstract class JsonObjectFXSerializer<T: NamedObsHolder<*>>(cls: KClass<T>): JsonObjectSerializer<T>(cls) {
    final override fun serialize(value: T) =
        buildJsonObject {
            value.namedObservables().forEach {
                putIfValueNotNull(it.key, it.value)
            }
        }
}

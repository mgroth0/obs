package matt.obs.json.sers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import matt.json.ser.JsonArraySerializer
import matt.json.ser.JsonObjectSerializer
import matt.json.ser.MySerializer
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.hold.NamedObsHolder
import matt.obs.prop.BindableProperty
import kotlin.reflect.KClass

class BindablePropertySerializer<T>(val serializer: KSerializer<T>):
  MySerializer<BindableProperty<T>>(BindableProperty::class) {
  override fun deserialize(jsonElement: JsonElement): BindableProperty<T> {
	return BindableProperty(Json.decodeFromJsonElement(serializer, jsonElement))
  }

  override fun serialize(value: BindableProperty<T>): JsonElement {
	return Json.encodeToJsonElement(serializer, value.value)
  }

}

class BasicObservableListImplSerializer<E: Any>(val serializer: KSerializer<in E>):
  JsonArraySerializer<BasicObservableListImpl<E>>(BasicObservableListImpl::class) {
  override fun deserialize(jsonArray: JsonArray): BasicObservableListImpl<E> {

	/*THIS UNCHECKED CAST IS NECESSARY BECAUSE KOTLINX.SERIALIZATION DOESN'T PREFORM WELL FOR SEALED CLASS POLYMORPHIC SERIALIZERS WHEN YOU ARE CREATING A LIST OF A SPECIFIC SUBCLASS. NOT USING THE BASE CLASS SERIALIZER LEADS TO AN ERROR. SO AN 'IN' GENERIC AND AN UNCHECKED CASE IS NECESSARY. THIS WOULD BE A GREAT THING TO BRING UP ON THE KOTLINX.SERIALIZATION GITHUB.*/

	return BasicObservableListImpl(jsonArray.map {
	  @Suppress("UNCHECKED_CAST")
	  Json.decodeFromJsonElement(serializer, it) as E
	})
  }

  override fun serialize(value: BasicObservableListImpl<E>): JsonArray {
	return JsonArray(value.map { Json.encodeToJsonElement(serializer, it) })
  }

}


abstract class JsonObjectFXSerializer<T: NamedObsHolder<*>>(val cls: KClass<T>): JsonObjectSerializer<T>(cls) {
  open val miniSerializers: List<MySerializer<*>> = listOf()
  final override fun serialize(value: T) = jsonObj(
	value.namedObservables(),
	serializers = miniSerializers
  )
}

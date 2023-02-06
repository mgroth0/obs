package matt.obs.json.prop

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import matt.lang.NOT_IMPLEMENTED
import matt.obs.prop.BindableProperty

//private object IntMPropSerializer: MyJsonSerializer<IntegerBProperty>(IntegerBProperty::class) {
//  override fun deserialize(jsonElement: JsonElement) = IntegerBProperty(jsonElement.int)
//  override fun serialize(value: IntegerBProperty) = JsonPrimitive(value.value)
//}
//
//@Serializable(with = IntMPropSerializer::class) private class IntegerBProperty(value: Int): BindableProperty<Int>(value)
//
//private object StringMPropSerializer: MyJsonSerializer<StringBProperty>(StringBProperty::class) {
//  override fun deserialize(jsonElement: JsonElement) = StringBProperty(jsonElement.string)
//  override fun serialize(value: StringBProperty) = JsonPrimitive(value.value)
//}
//
//@Serializable(with = StringMPropSerializer::class) private class StringBProperty(value: String): BindableProperty<String>(value)
//
//private object NStringMPropSerializer: MyJsonSerializer<NStringBProperty>(NStringBProperty::class) {
//  override fun deserialize(jsonElement: JsonElement) = NStringBProperty(jsonElement.nullOr { string })
//  override fun serialize(value: NStringBProperty) = JsonPrimitive(value.value)
//}
//
//@Serializable(with = NStringMPropSerializer::class) private class NStringBProperty(value: String?):
//  BindableProperty<String?>(value)


fun <T> BindableProperty<T>.setFromJson(j: JsonElement) {
  if (j is JsonNull) {
	@Suppress("UNCHECKED_CAST")
	(this as BindableProperty<Any?>).value = null
  } else if (j is JsonPrimitive) {
	if (j.isString) {
	  @Suppress("UNCHECKED_CAST")
	  (this as BindableProperty<String?>).value = j.content
	} else {
	  val int = j.intOrNull
	  if (int!= null) {
		@Suppress("UNCHECKED_CAST")
		(this as BindableProperty<Int?>).value = int
	  } else {
		val double = j.doubleOrNull
		if (double!=null) {
		  @Suppress("UNCHECKED_CAST")
		  (this as BindableProperty<Double?>).value = double
		} else {
		  val bool = j.booleanOrNull
		  if (bool!=null) {
			@Suppress("UNCHECKED_CAST")
			(this as BindableProperty<Boolean?>).value = bool
		  } else {
			NOT_IMPLEMENTED("don't know how to get value from $j")
		  }
		}
	  }
	}
  } else NOT_IMPLEMENTED("don't know how to get value from $j")
}

package matt.obs.json.prop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import matt.json.custom.int
import matt.json.custom.nullOr
import matt.json.custom.string
import matt.json.ser.MyJsonSerializer
import matt.obs.prop.BindableProperty

private object IntMPropSerializer: MyJsonSerializer<IntegerBProperty>(IntegerBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = IntegerBProperty(jsonElement.int)
  override fun serialize(value: IntegerBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = IntMPropSerializer::class) private class IntegerBProperty(value: Int): BindableProperty<Int>(value)

private object StringMPropSerializer: MyJsonSerializer<StringBProperty>(StringBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = StringBProperty(jsonElement.string)
  override fun serialize(value: StringBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = StringMPropSerializer::class) private class StringBProperty(value: String): BindableProperty<String>(value)

private object NStringMPropSerializer: MyJsonSerializer<NStringBProperty>(NStringBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = NStringBProperty(jsonElement.nullOr { string })
  override fun serialize(value: NStringBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = NStringMPropSerializer::class) private class NStringBProperty(value: String?):
  BindableProperty<String?>(value)
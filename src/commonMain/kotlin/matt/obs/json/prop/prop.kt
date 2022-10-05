package matt.obs.json.prop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import matt.json.custom.int
import matt.json.custom.nullOr
import matt.json.custom.string
import matt.json.ser.MySerializer
import matt.obs.prop.BindableProperty

object IntMPropSerializer: MySerializer<IntegerBProperty>(IntegerBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = IntegerBProperty(jsonElement.int)
  override fun serialize(value: IntegerBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = IntMPropSerializer::class) class IntegerBProperty(value: Int): BindableProperty<Int>(value)

object StringMPropSerializer: MySerializer<StringBProperty>(StringBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = StringBProperty(jsonElement.string)
  override fun serialize(value: StringBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = StringMPropSerializer::class) class StringBProperty(value: String): BindableProperty<String>(value)

object NStringMPropSerializer: MySerializer<NStringBProperty>(NStringBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = NStringBProperty(jsonElement.nullOr { string })
  override fun serialize(value: NStringBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = NStringMPropSerializer::class) class NStringBProperty(value: String?):
  BindableProperty<String?>(value)
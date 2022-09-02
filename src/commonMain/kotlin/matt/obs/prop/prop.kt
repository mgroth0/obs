package matt.obs.prop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import matt.json.custom.int
import matt.json.custom.string
import matt.json.ser.ser
import matt.klib.lang.B
import matt.obs.MObservableROValBase
import matt.obs.WritableMObservableVal


open class ReadOnlyBindableProperty<T>(value: T): MObservableROValBase<T>() {

  override var value = value
	protected set(v) {
	  if (v != field) {
		field = v
		notifyListeners(v)
	  }
	}

}

infix fun <T> BindableProperty<T>.v(value: T) {
  this.value = value
}

open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value), WritableMObservableVal<T> {
  override var boundTo: ReadOnlyBindableProperty<out T>? = null
  override var value = value
	set(v) {
	  if (v != field) {
		field = v
		notifyListeners(v)
	  }
	}
}

object IntMPropSerializer: ser<IntegerBProperty>(IntegerBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = IntegerBProperty(jsonElement.int)
  override fun serialize(value: IntegerBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = IntMPropSerializer::class) class IntegerBProperty(value: Int): BindableProperty<Int>(value)

object StringMPropSerializer: ser<StringBProperty>(StringBProperty::class) {
  override fun deserialize(jsonElement: JsonElement) = StringBProperty(jsonElement.string)
  override fun serialize(value: StringBProperty) = JsonPrimitive(value.value)
}

@Serializable(with = StringMPropSerializer::class) class StringBProperty(value: String): BindableProperty<String>(value)

typealias ValProp<T> = ReadOnlyBindableProperty<T>
typealias VarProp<T> = BindableProperty<T>

fun bProp(b: Boolean) = BindableProperty(b)
fun sProp(s: String) = BindableProperty(s)
fun iProp(i: Int) = BindableProperty(i)


fun ValProp<B>.whenTrueOnce(op: ()->Unit) {
  if (value) op()
  else {
	onChangeUntil({ it }) {
	  if (it) op()
	}
  }
}
package matt.obs.prop

import matt.lang.B
import matt.obs.MObservableROValBase
import matt.obs.NewListener
import matt.obs.WritableMObservableVal


open class ReadOnlyBindableProperty<T>(value: T): MObservableROValBase<T>() {

  override var value = value
	protected set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(old, v)
	  }
	}

}

infix fun <T> BindableProperty<T>.v(value: T) {
  this.value = value
}

infix fun <T> BindableProperty<T>.eqNow(value: T): Boolean {
  return this.value == value
}

infix fun <T> BindableProperty<T>.eqNow(value: MObservableROValBase<T>): Boolean {
  return this.value == value.value
}

infix fun <T> BindableProperty<T>.notEqNow(value: T): Boolean {
  return this.value != value
}

infix fun <T> BindableProperty<T>.notEqNow(value: MObservableROValBase<T>): Boolean {
  return this.value != value.value
}

open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value), WritableMObservableVal<T> {
  override var boundTo: ReadOnlyBindableProperty<out T>? = null
  override var value = value
	set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(old, v)
	  }
	}
}


typealias ValProp<T> = ReadOnlyBindableProperty<T>
typealias VarProp<T> = BindableProperty<T>

fun bProp(b: Boolean) = BindableProperty(b)
fun sProp(s: String) = BindableProperty(s)
fun iProp(i: Int) = BindableProperty(i)


fun ValProp<B>.whenTrueOnce(op: ()->Unit) {
  if (value) op()
  else {
	onChangeUntil({ it }, NewListener {
	  if (it) op()
	})
  }
}


fun VarProp<Boolean>.toggle() {
  value = !value
}
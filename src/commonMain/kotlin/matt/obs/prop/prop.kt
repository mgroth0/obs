package matt.obs.prop

import matt.klib.lang.B
import matt.obs.MObservableVarImpl
import kotlin.reflect.KProperty


open class ReadOnlyBindableProperty<T>(value: T): MObservableVarImpl<T>() {

  init {
	onChange { v ->
	  boundedProps.forEach {
		if (it.value != v) it.value = v
	  }
	}
  }

  private val boundedProps = mutableSetOf<BindableProperty<in T>>()

  fun removeListener(listener: (T)->Unit) {
	listeners -= listener
  }

  fun addBoundedProp(p: BindableProperty<in T>) {
	boundedProps += p
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }

}


class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value) {

  fun bind(other: ReadOnlyBindableProperty<out T>) {
	this.value = other.value
	other.addBoundedProp(this)
  }

  fun bindBidirectional(other: BindableProperty<T>) {
	this.value = other.value
	other.addBoundedProp(this)
	addBoundedProp(other)
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
	value = newValue
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
	onChangeUntil({ it }) {
	  if (it) op()
	}
  }
}
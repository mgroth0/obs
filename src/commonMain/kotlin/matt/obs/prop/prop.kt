package matt.obs.prop

import matt.klib.lang.B
import matt.obs.olist.MObservable
import kotlin.reflect.KProperty

open class ReadOnlyBindableProperty<T>(value: T): matt.obs.olist.MObservable<T> {
  protected val boundedProps = mutableSetOf<BindableProperty<in T>>()
  protected val listeners = mutableListOf<(T)->Unit>()
  open var value = value
	protected set(v) {
	  if (v != field) {
		field = v
		boundedProps.forEach { if (it.value != v) it.value = v }
		listeners.forEach { it(v) }
	  }
	}

  override fun onChange(listener: (T)->Unit): (T)->Unit {
	listeners += listener
	return listener
  }

  override fun onChangeUntil(until: (T)->Boolean, listener: (T)->Unit) {
	var realListener: ((T)->Unit)? = null
	realListener = { t: T ->
	  listener(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }


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

  override var value = value
	public set(v) {
	  if (v != field) {
		field = v
		boundedProps.forEach { if (it.value != v) it.value = v }
		listeners.forEach { it(v) }
	  }
	}

  fun bind(other: ReadOnlyBindableProperty<out T>) {
	this.value = other.value
	other.addBoundedProp(this)
  }

  @Suppress("unused") fun bindBidirectional(other: BindableProperty<T>) {
	this.value = other.value
	other.addBoundedProp(this)
	boundedProps.add(other)
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
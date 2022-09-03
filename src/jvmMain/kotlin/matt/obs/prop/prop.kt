@file:JvmName("PropJvmKt")

package matt.obs.prop

import java.util.WeakHashMap
import kotlin.reflect.KProperty

class BasicProperty<T>(initialValue: T) {
  private val listeners = WeakHashMap<Any, (T)->Unit>()
  private val permaListeners = mutableListOf<(T)->Unit>()

  var value: T = initialValue
	set(value) {
	  field = value
	  listeners.forEach { (_, op) ->
		op(value)
	  }
	  permaListeners.forEach {
		it(value)
	  }
	}

  fun onChangeWithWeak(obj: Any, op: (T)->Unit) {
	listeners[obj] = op
  }

  fun onChange(op: (T)->Unit) {
	permaListeners += op
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
	this.value = value
  }

  fun withChangeListener(op: (T)->Unit): BasicProperty<T> {
	onChange(op)
	return this
  }

}


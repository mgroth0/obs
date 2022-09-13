package matt.obs.bind

import matt.obs.listen.NewListener
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.ValueListener
import matt.obs.listen.moveTo
import matt.obs.listen.update.LazyNewValueUpdate
import matt.obs.listen.update.ValueUpdate
import matt.obs.prop.MObservableROPropBase
import matt.obs.prop.MObservableROValBase
import matt.obs.prop.MObservableVal
import matt.obs.prop.MObservableValNewAndOld
import matt.obs.prop.MObservableValNewOnly
import matt.obs.prop.VarProp
import kotlin.jvm.Synchronized

private object NOT_CALCED
class MyBinding<T>(private val calc: ()->T): MObservableROValBase<T, ValueUpdate<T>, NewListener<T>>(),
											 MObservableValNewOnly<T> {

  private var valid = false

  @Synchronized fun invalidate() {
	valid = false
	notifyListeners(LazyNewValueUpdate { value })
  }

  private var lastCalculated: Any? = NOT_CALCED

  override val value: T
	@Synchronized get() {
	  @Suppress("UNCHECKED_CAST") if (valid) return lastCalculated as T
	  else {
		lastCalculated = calc()
		valid = true
		return lastCalculated as T
	  }
	}


}


fun <T, U: ValueUpdate<T>, L: ValueListener<T, U>> MObservableValNewAndOld<MObservableVal<T, U, L>?>.deepOnChange(op: (T)->Unit) {
  var subListener: L? = value?.onChange(op)
  addListener(OldAndNewListener { _, new ->
	if (new != null) {
	  subListener?.moveTo(new) ?: run {
		subListener = value?.onChange(op)
	  }
	}
  })
}

fun <T, R> MObservableValNewAndOld<MObservableVal<T, ValueUpdate<T>, NewListener<T>>?>.deepBinding(
  vararg dependencies: MObservableVal<*, *, *>,
  op: (T)->R
): MObservableROPropBase<R?> {
  val r = VarProp(value?.value?.let(op))
  deepOnChange {
	r.value = op(it)
  }
  dependencies.forEach {
	it.onChange {
	  r.value = value?.value?.let(op)
	}
  }
  return r
}



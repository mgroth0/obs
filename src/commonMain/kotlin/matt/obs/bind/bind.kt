package matt.obs.bind

import matt.obs.MObservable
import matt.obs.UpdatesFromOutside
import matt.obs.col.BasicOCollection
import matt.obs.listen.NewListener
import matt.obs.listen.update.LazyNewValueUpdate
import matt.obs.listen.update.ValueUpdate
import matt.obs.oobj.MObservableObject
import matt.obs.prop.MObservableROValBase
import matt.obs.prop.MObservableValNewOnly
import matt.obs.prop.ObsVal
import kotlin.jvm.Synchronized


fun <T, R> MObservableObject<T>.binding(
  vararg dependencies: MObservable,
  op: T.()->R
): MyBinding<R> = MyBinding(this, *dependencies) { uncheckedThis.op() }

fun <T, R> ObsVal<T>.binding(
  vararg dependencies: MObservable,
  op: (T)->R,
) = MyBinding(this, *dependencies) { op(value) }

fun <T, R> ObsVal<T>.deepBinding(propGetter: (T)->ObsVal<R>) {
  val b = MyBinding {
	propGetter(value).value
  }
  var lastSubListener = propGetter(value).observe {
	b.invalidate()
  }
  onChange {
	lastSubListener.tryRemovingListener()
	b.invalidate()
	lastSubListener = propGetter(value).observe {
	  b.invalidate()
	}
  }
}

fun <E, R> BasicOCollection<E>.binding(
  vararg dependencies: MObservable,
  op: (BasicOCollection<E>)->R,
): MyBinding<R> = MyBinding(this, *dependencies) { op(this) }

private object NOT_CALCED
class MyBinding<T>(vararg dependencies: MObservable, private val calc: ()->T):
  MObservableROValBase<T, ValueUpdate<T>, NewListener<T>>(),
  MObservableValNewOnly<T>,
  UpdatesFromOutside {

  init {
	setupDependencies(*dependencies)
  }

  private var valid = false

  @Synchronized override fun invalidate() {
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


//fun <T, U: ValueUpdate<T>, L: ValueListener<T, U>> MObservableValNewAndOld<MObservableVal<T, U, L>?>.deepOnChange(op: (T)->Unit) {
//  var subListener: L? = value?.onChange(op)
//  addListener(OldAndNewListener { _, new ->
//	if (new != null) {
//	  subListener?.moveTo(new) ?: run {
//		subListener = value?.onChange(op)
//	  }
//	}
//  })
//}
//
//fun <T, R> MObservableValNewAndOld<MObservableVal<T, ValueUpdate<T>, NewListener<T>>?>.deepBinding(
//  vararg dependencies: MObservableVal<*, *, *>,
//  op: (T)->R
//): MObservableROPropBase<R?> {
//  val r = VarProp(value?.value?.let(op))
//  deepOnChange {
//	r.value = op(it)
//  }
//  dependencies.forEach {
//	it.onChange {
//	  r.value = value?.value?.let(op)
//	}
//  }
//  return r
//}



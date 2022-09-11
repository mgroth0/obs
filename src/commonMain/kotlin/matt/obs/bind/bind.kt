package matt.obs.bind

import matt.obs.MObservableObject
import matt.obs.MObservableObjectImpl
import matt.obs.MObservableROValBase
import matt.obs.MObservableVal
import matt.obs.MObservableWithChangeObject
import matt.obs.col.BasicROObservableCollection
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp
import kotlin.jvm.Synchronized

private object NOT_CALCED
class MyBinding<T> internal constructor(private val calc: ()->T): MObservableObjectImpl<MyBinding<T>>() {

  private var valid = false

  @Synchronized
  internal fun invalidate() {
	valid = false
	listeners.forEach { it.invoke(this) }
	//	notifyListeners()
  }

  private var lastCalculated: Any? = NOT_CALCED

  val value: T
	@Synchronized
	get() {
	  @Suppress("UNCHECKED_CAST")
	  if (valid) return lastCalculated as T
	  else {
		lastCalculated = calc()
		valid = true
		return lastCalculated as T
	  }
	}

  //	protected set(v) {
  //	  if (v != field) {
  //		field = v
  //		notifyListeners(v)
  //	  }
  //	}

}

fun <T, R> MObservableROValBase<T>.lazyBinding(
  vararg dependencies: MObservableVal<*>,
  op: (T)->R,
): MyBinding<R> {
  val prop = this
  return MyBinding { op(value) }.apply {
	prop.onChange {
	  invalidate()
	}
	dependencies.forEach {
	  it.onChange {
		invalidate()
	  }
	}
  }
}




fun <T, R> MObservableVal<T>.binding(
  vararg dependencies: MObservableVal<*>,
  debug: Boolean = false,
  op: (T)->R,
): ValProp<R> {
  val prop = this
  return VarProp(op(value)).apply {
	prop.onChange {
	  if (debug) println("prop changed: $it")
	  value = op(it)
	}
	dependencies.forEach {
	  it.onChange {
		if (debug) println("dep changed: $it")
		value = op(prop.value)
	  }
	}
  }
}

fun <T: MObservableObject<T>, R> MObservableObject<T>.lazyBinding(
  vararg dependencies: MObservableVal<*>,
  op: T.()->R,
): MyBinding<R> {
  @Suppress("UNCHECKED_CAST") val prop = this as T
  return MyBinding { prop.op() }.apply {
	prop.onChange {
	  this@apply.invalidate()
	}
	dependencies.forEach {
	  it.onChange {
		invalidate()
	  }
	}
  }
}

fun <T: MObservableObject<T>, R> MObservableObject<T>.binding(
  vararg dependencies: MObservableVal<T>,
  debug: Boolean = false,
  op: T.()->R,
): ValProp<R> {
  @Suppress("UNCHECKED_CAST")
  val prop = this as T
  return VarProp(prop.op()).apply {
	val b = this
	prop.onChange {
	  if (debug) println("MObservableObject changed: $prop")
	  b.value = prop.op()
	}
	dependencies.forEach {
	  it.onChange {
		if (debug) println("dep changed: $it")
		b.value = prop.op()
	  }
	}
  }
}


//fun <T, R, TT: ValProp<R?>> ValProp<T>.lazyChainBinding(
//  op: (T)->TT?,
//): MyBinding<R?> {
//
//  val prop = this
//
//  var tt = op(value)
//
//  return MyBinding({tt?.value}).apply {
//	println("initial value = $value")
//	var listener = tt?.onChange {
//	  invalidate()
////	  value = tt?.value
//	  println("value2=${value}")
//	}
//	prop.onChange {
//	  tt = op(it)
//	  invalidate()
////	  value = tt?.value
//	  println("value3=${value}")
//	  listener?.go {
//
//		removeListener(it) }
//	  listener = tt?.onChange {
//		value = tt?.value
//		println("value4=${value}")
//	  }
//	}
//  }
//}
//
//
//fun <T, R, TT: ValProp<R?>> ValProp<T>.chainBinding(
//  op: (T)->TT?,
//): ValProp<R?> {
//
//  val prop = this
//
//  var tt = op(value)
//
//  return BindableProperty(tt?.value).apply {
//	println("initial value = $value")
//	var listener = tt?.onChange {
//	  value = tt?.value
//	  println("value2=${value}")
//	}
//	prop.onChange {
//	  tt = op(it)
//	  value = tt?.value
//	  println("value3=${value}")
//	  listener?.go { removeListener(it) }
//	  listener = tt?.onChange {
//		value = tt?.value
//		println("value4=${value}")
//	  }
//	}
//  }
//}

fun <E, R> BasicROObservableCollection<E>.lazyBinding(
  vararg dependencies: MObservableVal<*>,
  op: (Collection<E>)->R,
): MyBinding<R> {
  val prop = this
  return MyBinding { op(prop) }.apply {
	prop.onChange {
	  invalidate()
	  //	  value = op(prop)
	}
	dependencies.forEach {
	  it.onChange {
		invalidate()
		//		value = op(prop)
	  }
	}
  }
}

inline fun <C, R, reified CC: MObservableWithChangeObject<C>> CC.binding(
  vararg dependencies: MObservableVal<*>,
  crossinline op: (CC)->R,
): ValProp<R> {
  val prop = this
  return VarProp(op(prop)).apply {
	prop.onChange {
	  value = op(prop)
	}
	dependencies.forEach {
	  it.onChange {
		value = op(prop)
	  }
	}
  }
}
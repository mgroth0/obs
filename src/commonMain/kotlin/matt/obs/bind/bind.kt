package matt.obs.bind

import matt.log.todo
import matt.obs.col.MObservableWithChangeObject
import matt.obs.col.olist.BaseBasicWritableOList
import matt.obs.oobj.MObservableObject
import matt.obs.prop.MObservableROPropBase
import matt.obs.prop.MObservableROValBase
import matt.obs.prop.MObservableVal
import matt.obs.prop.MObservableValNewOnly
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp
import matt.obs.prop.listen.NewListener
import kotlin.jvm.Synchronized

private object NOT_CALCED
class MyBinding<T>(private val calc: ()->T): MObservableROValBase<T, NewListener<T>>(), MObservableValNewOnly<T> {

  private var valid = false

  @Synchronized fun invalidate() {
	todo(
	  "now i know why there are invalidation listeners! because sometimes i don't need to calculate the new result. Need listener types with conditions for cancelling, invalidation listeners, different params, and filters, etc!"
	)
	valid = false
	if (listeners.isNotEmpty()) {
	  val v = value
	  listeners.forEach { it.invoke(v) }    //	notifyListeners()
	}
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


  override fun onChangeUntil(until: (T)->Boolean, listener: NewListener<T>) {
	var realListener: NewListener<T>? = null
	realListener = NewListener { t: T ->
	  listener.invoke(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }


}

fun <T, R> MObservableROPropBase<T>.lazyBinding(
  vararg dependencies: MObservableVal<*, *>,
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


fun <T, R> MObservableVal<T, *>.binding(
  vararg dependencies: MObservableVal<*, *>,
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
  vararg dependencies: MObservableVal<*, *>,
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
  vararg dependencies: MObservableVal<T, *>,
  debug: Boolean = false,
  op: T.()->R,
): ValProp<R> {
  @Suppress("UNCHECKED_CAST") val prop = this as T
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

fun <E, R> BaseBasicWritableOList<E>.lazyBinding(
  vararg dependencies: MObservableVal<*, *>,
  op: (Collection<E>)->R,
): MyBinding<R> {
  val prop = this
  return MyBinding { op(prop) }.apply {
	prop.onChange {
	  invalidate()    //	  value = op(prop)
	}
	dependencies.forEach {
	  it.onChange {
		invalidate()        //		value = op(prop)
	  }
	}
  }
}

inline fun <C, R, reified CC: MObservableWithChangeObject<C>> CC.binding(
  vararg dependencies: MObservableVal<*, *>,
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
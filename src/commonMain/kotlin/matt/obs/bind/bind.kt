package matt.obs.bind

import matt.klib.lang.go
import matt.obs.olist.BasicObservableCollection
import matt.obs.olist.MObservable
import matt.obs.prop.BindableProperty
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp
import kotlin.contracts.ExperimentalContracts

fun <T, R> ValProp<T>.binding(
  vararg dependencies: MObservable<*>,
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


@OptIn(ExperimentalContracts::class)
fun <T, R, TT: ValProp<R?>> ValProp<T>.chainBinding(
  op: (T)->TT?,
): ValProp<R?> {

  val prop = this

  var tt = op(value)

  return BindableProperty(tt?.value).apply {
	println("initial value = ${value}")
	var listener = tt?.onChange {
	  value = tt?.value
	  println("value2=${value}")
	}
	prop.onChange {
	  tt = op(it)
	  value = tt?.value
	  println("value3=${value}")
	  listener?.go { removeListener(it) }
	  listener = tt?.onChange {
		value = tt?.value
		println("value4=${value}")
	  }
	}
  }
}

fun <E, R> BasicObservableCollection<E>.binding(
  vararg dependencies: MObservable<*>,
  op: (Collection<E>)->R,
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
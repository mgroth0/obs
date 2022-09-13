package matt.obs.oobj

import matt.obs.MObservable
import matt.obs.MObservableImpl
import matt.obs.bind.MyBinding
import matt.obs.listen.ContextListener
import matt.obs.listen.update.ContextUpdate
import matt.obs.prop.MObservableVal
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp


interface MObservableObject<T>: MObservable<ContextListener<T>> {

  @Suppress("UNCHECKED_CAST") val uncheckedThis get() = this as T

  fun onChange(op: T.()->Unit) = addListener(ContextListener<T>(uncheckedThis) {
	op()
  })

  fun <R> lazyBinding(
	vararg dependencies: MObservableVal<*, *, *>,
	op: T.()->R,
  ): MyBinding<R> {
	return MyBinding { uncheckedThis.op() }.apply {
	  this@MObservableObject.onChange {
		invalidate()
	  }
	  dependencies.forEach {
		it.onChange {
		  invalidate()
		}
	  }
	}
  }

  fun <R> binding(
	vararg dependencies: MObservableVal<T, *, *>,
	debug: Boolean = false,
	op: T.()->R,
  ): ValProp<R> {
	return VarProp(uncheckedThis.op()).apply {
	  val b = this
	  this@MObservableObject.onChange {
		if (debug) println("MObservableObject changed: ${this@MObservableObject}")
		b.value = this@MObservableObject.uncheckedThis.op()
	  }
	  dependencies.forEach {
		it.onChange {
		  if (debug) println("dep changed: $it")
		  b.value = this@MObservableObject.uncheckedThis.op()
		}
	  }
	}
  }
}

class ObservableObject<T: ObservableObject<T>> internal constructor():
  MObservableImpl<ContextUpdate, ContextListener<T>>(),
  MObservableObject<T> {

  fun invalidate() {
	notifyListeners(ContextUpdate)
  }

}

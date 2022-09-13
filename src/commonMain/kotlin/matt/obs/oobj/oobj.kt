package matt.obs.oobj

import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bind.MyBinding
import matt.obs.listen.ContextListener
import matt.obs.listen.update.ContextUpdate
import matt.obs.prop.MObservableVal
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp


interface MObservableObject<T>: MListenable<ContextListener<T>> {

  @Suppress("UNCHECKED_CAST") val uncheckedThis get() = this as T

  override fun observe(op: ()->Unit) = onChange { op() }
  fun onChange(op: T.()->Unit) = addListener(ContextListener<T>(uncheckedThis) {
	op()
  })

  fun <R> binding(
	vararg dependencies: MObservableVal<T, *, *>,
	debug: Boolean = false,
	op: T.()->R,
  ): MyBinding<R> {
	val b = MyBinding { uncheckedThis.op() }
	observe { b.invalidate() }
	dependencies.forEach { observe { b.invalidate() } }
  }
}

abstract class ObservableObject<T: ObservableObject<T>>: MObservableImpl<ContextUpdate, ContextListener<T>>(),
														 MObservableObject<T> {

  fun invalidate() {
	notifyListeners(ContextUpdate)
  }

}

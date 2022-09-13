package matt.obs.oobj

import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.UpdatesFromOutside
import matt.obs.listen.ContextListener
import matt.obs.listen.update.ContextUpdate


interface MObservableObject<T>: MListenable<ContextListener<T>>, UpdatesFromOutside {

  @Suppress("UNCHECKED_CAST") val uncheckedThis get() = this as T

  override fun observe(op: ()->Unit) = onChange { op() }
  fun onChange(op: T.()->Unit) = addListener(ContextListener<T>(uncheckedThis) {
	op()
  })


}

abstract class ObservableObject<T: ObservableObject<T>>: MObservableImpl<ContextUpdate, ContextListener<T>>(),
														 MObservableObject<T> {

  override fun invalidate() {
	notifyListeners(ContextUpdate)
  }

}

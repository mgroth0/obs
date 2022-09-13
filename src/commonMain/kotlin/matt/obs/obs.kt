package matt.obs

import matt.obs.listen.MyListener
import matt.obs.listen.update.Update
import kotlin.jvm.Synchronized

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObservable<L: MyListener<*>> {

  fun onChange(op: () -> Unit): L
  fun addListener(listener: L): L
  fun removeListener(listener: MyListener<*>): Boolean

}

abstract class MObservableImpl<U: Update, L: MyListener<U>> internal constructor(): MObservable<L> {

  private val listeners = mutableListOf<L>()

  @Synchronized
  final override fun addListener(listener: L): L {
	listeners += listener
	require(listener.currentObservable == null)
	listener.currentObservable = this
	return listener
  }

  @Synchronized
  protected fun notifyListeners(update: U) {
	/*gotta make a new list to prevent concurrent mod error if listeners list is edited in a listener*/
	listeners.toList().forEach {
	  if (it.preInvocation()) {
		it.notify(update)
		it.postInvocation()
	  }
	}
  }

  @Synchronized
  override fun removeListener(listener: MyListener<*>): Boolean {
	val b = listeners.remove(listener)
	listener.currentObservable = null
	return b
  }


}



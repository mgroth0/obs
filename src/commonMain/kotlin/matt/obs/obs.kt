package matt.obs

import matt.obs.prop.listen.MyListener
import kotlin.jvm.Synchronized

@DslMarker annotation class ObservableDSL

@ObservableDSL sealed interface MObservable<L: MyListener> {

  fun addListener(listener: L)
  fun onChange(op: ()->Unit): L
  fun removeListener(listener: MyListener): Boolean

}

abstract class MObservableImpl<L: MyListener> internal constructor(): MObservable<L> {

  private val listeners = mutableListOf<L>()

  @Synchronized
  final override fun addListener(listener: L) {
	listeners += listener
	require(listener.currentObservable == null)
	listener.currentObservable = this
  }

  @Synchronized
  protected fun notifyListeners() {
	listeners.forEach {
	  if (it.preInvocation()) {
		it.notify()
		it.postInvocation()
	  }
	}
  }

  protected abstract fun L.notify()


  @Synchronized
  override fun removeListener(listener: MyListener): Boolean {
	val b = listeners.remove(listener)
	listener.currentObservable = null
	return b
  }

  fun onChangeOnce(op: ()->Unit) = onChange(op).apply {
	removeAfterInvocation = true
  }


}



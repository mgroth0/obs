package matt.obs

import matt.collect.snapshotToPreventConcurrentModification
import matt.obs.listen.MyListener
import matt.obs.listen.update.Update
import kotlin.jvm.Synchronized

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObservable {
  fun observe(op: ()->Unit): MyListener<*>
  fun removeListener(listener: MyListener<*>): Boolean
}


@ObservableDSL interface MListenable<L: MyListener<*>>: MObservable {
  fun addListener(listener: L): L
}

abstract class MObservableImpl<U: Update, L: MyListener<U>> internal constructor(): MListenable<L> {

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
	listeners.snapshotToPreventConcurrentModification().forEach {
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





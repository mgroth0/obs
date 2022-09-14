package matt.obs

import matt.collect.snapshotToPreventConcurrentModification
import matt.lang.function.MetaFunction
import matt.obs.listen.Listener
import matt.obs.listen.MyListener
import matt.obs.listen.update.Update
import kotlin.jvm.Synchronized

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObservable {
  fun observe(op: ()->Unit): Listener
  fun removeListener(listener: Listener): Boolean

  /*critical if an observer is receiving a batch of redundant notfications and only needs to act once*/
  fun patientlyObserve(scheduleOp: MetaFunction, op: ()->Unit): Listener {
	var shouldScheduleAnother = true
	return observe {
	  if (shouldScheduleAnother) {
		shouldScheduleAnother = false
		scheduleOp {
		  shouldScheduleAnother = true
		  op()
		}
	  }
	}
  }
}


@ObservableDSL interface MListenable<L: Listener>: MObservable {
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
  override fun removeListener(listener: Listener): Boolean {
	val b = listeners.remove(listener)
	listener.currentObservable = null
	return b
  }

}





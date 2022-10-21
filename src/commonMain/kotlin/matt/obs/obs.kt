package matt.obs

import matt.lang.function.MetaFunction
import matt.lang.weak.WeakRef
import matt.model.debug.DebugLogger
import matt.model.syncop.AntiDeadlockSynchronizer
import matt.model.tostringbuilder.toStringBuilder
import matt.obs.listen.Listener
import matt.obs.listen.MyListener
import matt.obs.listen.update.Update

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObservable {
  var nam: String?
  fun observe(op: ()->Unit): Listener
  fun removeListener(listener: Listener)

  /*critical if an observer is receiving a batch of redundant notifications and only needs to act once*/
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

  var debugger: DebugLogger?

}


@ObservableDSL interface MListenable<L: Listener>: MObservable {
  fun addListener(listener: L): L
}

abstract class MObservableImpl<U: Update, L: MyListener<U>> internal constructor(): MListenable<L> {

  override var nam: String? = null

  override fun toString() = toStringBuilder(
	"name" to nam, "#listeners" to listeners.size
  )

  private val listeners = mutableListOf<L>()
  private val synchronizer by lazy { AntiDeadlockSynchronizer() }


  final override fun addListener(listener: L): L {
	synchronizer.operateOnInternalDataNowOrLater {
	  listeners += listener
	  require(listener.currentObservable == null)
	  listener.currentObservable = WeakRef(this)
	}
	return listener
  }

  override var debugger: DebugLogger? = null

  private var currentUpdateCount = 0

  protected fun notifyListeners(update: U) {
	synchronizer.useInternalData {
	  listeners.forEach { listener ->
		if (listener.preInvocation()) {
		  listener.notify(update, debugger = debugger)
		  listener.postInvocation()
		}
	  }
	}
  }

  override fun removeListener(listener: Listener) {
	synchronizer.operateOnInternalDataNowOrLater {
	  listeners.remove(listener)
	  listener.currentObservable = null
	}
  }

  private val changeQueue = mutableListOf<()->Unit>()

}


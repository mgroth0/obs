package matt.obs

import matt.collect.snapshotToPreventConcurrentModification
import matt.lang.function.MetaFunction
import matt.lang.weak.WeakRef
import matt.model.debug.DebugLogger
import matt.model.tostringbuilder.toStringBuilder
import matt.obs.listen.Listener
import matt.obs.listen.MyListener
import matt.obs.listen.update.Update
import matt.time.UnixTime
import kotlin.jvm.Synchronized

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObservable {
  var nam: String?
  fun observe(op: ()->Unit): Listener
  fun removeListener(listener: Listener): Boolean

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


  @Synchronized final override fun addListener(listener: L): L {
	debugger?.println("adding listener: $listener")
	listeners += listener
	require(listener.currentObservable == null)
	listener.currentObservable = WeakRef(this)
	debugger?.println("added listener: $listener")
	return listener
  }

  override var debugger: DebugLogger? = null

  @Synchronized protected fun notifyListeners(update: U) {
	debugger?.println("notifying listeners of $this")
	val start = if (debugger != null) UnixTime() else null
	listeners.snapshotToPreventConcurrentModification().forEach { listener ->
	  if (listener.preInvocation()) {
		var now = start?.let { UnixTime() - it }
		now?.let { debugger?.println("$it\tinvoking $listener for $this") }
		listener.notify(update, debugger = debugger)
		now = start?.let { UnixTime() - it }
		now?.let { debugger?.println("$it\tfinished invoking") }
		listener.postInvocation()
	  }
	}
  }

  //  @Synchronized
  //  override fun removeListener(listener: Listener): Boolean {
  //
  //  }

  override fun removeListener(listener: Listener): Boolean {
	val b = listeners.remove(listener)
	listener.currentObservable = null
	return b
  }


}





package matt.obs

import matt.collect.snapshotToPreventConcurrentModification
import matt.lang.function.MetaFunction
import matt.lang.weak.WeakRef
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

  var verboseObservations: Boolean

}


@ObservableDSL interface MListenable<L: Listener>: MObservable {
  fun addListener(listener: L): L
}

abstract class MObservableImpl<U: Update, L: MyListener<U>> internal constructor(): MListenable<L> {

  override var nam: String? = null

  override fun toString() = toStringBuilder(
	"name" to nam,
	"#listeners" to listeners.size
  )

  private val listeners = mutableListOf<L>()


  @Synchronized
  final override fun addListener(listener: L): L {
	listeners += listener
	require(listener.currentObservable == null)
	listener.currentObservable = WeakRef(this)
	return listener
  }

  override var verboseObservations: Boolean = false

  @Synchronized
  protected fun notifyListeners(update: U) {
	val start = if (verboseObservations) UnixTime() else null
	listeners.snapshotToPreventConcurrentModification().forEach { listener ->
	  if (listener.preInvocation()) {
		var now = start?.let { UnixTime() - it }
		now?.let { println("$it\tinvoking $listener for $this") }
		println("notify update = $update")
		listener.notify(update)
		now = start?.let { UnixTime() - it }
		now?.let { println("$it\tfinished invoking") }
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





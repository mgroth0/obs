package matt.obs

import matt.lang.function.MetaFunction
import matt.lang.weak.MyWeakRef
import matt.model.flowlogic.syncop.AntiDeadlockSynchronizer
import matt.model.obj.tostringbuilder.toStringBuilder
import matt.model.op.prints.Prints
import matt.obs.listen.MyListener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.MyWeakListener
import matt.obs.listen.update.Update

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObservable {
  var nam: String?
  fun observe(op: ()->Unit): MyListenerInter<*>
  fun observeWeakly(w: MyWeakRef<*>, op: ()->Unit): MyListenerInter<*>
  fun removeListener(listener: MyListenerInter<*>)

  /*critical if an observer is receiving a batch of redundant notifications and only needs to act once*/
  fun patientlyObserve(scheduleOp: MetaFunction, op: ()->Unit): MyListenerInter<*> {
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

  var debugger: Prints?

}


@ObservableDSL interface MListenable<L: MyListenerInter<*>>: MObservable {
  fun addListener(listener: L): L
}

expect fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean

abstract class MObservableImpl<U: Update, L: MyListenerInter<in U>> internal constructor(): MListenable<L> {

  override var nam: String? = null

  override fun toString() = toStringBuilder(
	"name" to nam, "#listeners" to listeners.size
  )

  private val listeners = mutableListOf<L>()

  private val weakListeners = run {


  }

  private val synchronizer by lazy { AntiDeadlockSynchronizer() }


  final override fun addListener(listener: L): L {
	synchronizer.operateOnInternalDataNowOrLater {
	  listeners += listener
	  listener as MyListener<*>
	  require(listener.currentObservable == null)
	  listener.currentObservable = MyWeakRef(this)
	  if (listener is MyWeakListener<*>) {
		if (!maybeRemoveByRefQueue(listener)) {
		  listeners.remove(listener)
		  (listener as? MyListener<*>)?.currentObservable = null
		}
	  }
	}
	return listener
  }

  override var debugger: Prints? = null

  private var currentUpdateCount = 0

  protected fun notifyListeners(update: U) {
	synchronizer.useInternalData {
	  listeners.forEach { listener ->
		@Suppress("UNCHECKED_CAST")
		listener as MyListener<U>
		if (listener.preInvocation(update) != null) {
		  listener.notify(update, debugger = debugger)
		  listener.postInvocation()
		}
	  }
	}
  }

  override fun removeListener(listener: MyListenerInter<*>) {
	synchronizer.operateOnInternalDataNowOrLater {
	  listeners.remove(listener)
	  (listener as? MyListener<*>)?.currentObservable = null
	}
  }

  private val changeQueue = mutableListOf<()->Unit>()

  /*TODO: This should be done by a ReferencesQueue on JVM*/
  fun cleanWeakListeners() {

	listeners.filterIsInstance<MyWeakListener<*>>().forEach {
	  if (it.shouldBeCleaned()) {
		removeListener(it)
	  }
	}
  }

}


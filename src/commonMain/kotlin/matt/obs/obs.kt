package matt.obs

import matt.lang.assertions.require.requireNonNegative
import matt.lang.assertions.require.requireNull
import matt.lang.exec.Exec
import matt.lang.function.Op
import matt.lang.tostring.mehToStringBuilder
import matt.lang.weak.MyWeakRef
import matt.model.flowlogic.syncop.AntiDeadlockSynchronizer
import matt.model.op.prints.Prints
import matt.obs.listen.MyListener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.MyWeakListener
import matt.obs.listen.update.Update

@DslMarker
annotation class ObservableDSL

@ObservableDSL
interface MObservable {
    var nam: String?
    fun observe(op: () -> Unit): MyListenerInter<*>
    fun observeWeakly(
        w: MyWeakRef<*>,
        op: () -> Unit
    ): MyListenerInter<*>

    fun removeListener(listener: MyListenerInter<*>)

    /*critical if an observer is receiving a batch of redundant notifications and only needs to act once*/
    fun patientlyObserve(
        scheduleOp: Exec,
        op: () -> Unit
    ): MyListenerInter<*> {
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


@ObservableDSL
interface MListenable<L : MyListenerInter<*>> : MObservable {
    fun addListener(listener: L): L
    fun releaseUpdatesAfter(op: Op)
}

expect fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean

abstract class MObservableImpl<U : Update, L : MyListenerInter<in U>> : MListenable<L> {

    override var nam: String? = null

    override fun toString() = mehToStringBuilder(
        "name" to nam, "#listeners" to listeners.size
    )

    private val listeners = mutableListOf<L>()

    private val synchronizer by lazy { AntiDeadlockSynchronizer() }


    final override fun addListener(listener: L): L {
        synchronizer.operateOnInternalDataNowOrLater {
            listeners += listener
            listener as MyListener<*>
            requireNull(listener.currentObservable)
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

    private var notifyAfterUpdates: MutableList<U>? = null
    private var notifyAfterDepth = 0

    protected fun notifyListeners(update: U) = synchronizer.useInternalData {
        if (notifyAfterDepth > 0) {
            if (notifyAfterUpdates == null) {
                notifyAfterUpdates = mutableListOf()
            }
            notifyAfterUpdates!!.add(update)
        } else {
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

    /*TODO: This should be done by a ReferencesQueue on JVM*/
    fun cleanWeakListeners() {
        listeners.filterIsInstance<MyWeakListener<*>>().forEach {
            if (it.shouldBeCleaned()) {
                removeListener(it)
            }
        }
    }


    override fun releaseUpdatesAfter(op: Op) = synchronizer.useInternalData {
        notifyAfterDepth += 1
        op()
        notifyAfterDepth -= 1
        requireNonNegative(notifyAfterDepth)
        if (notifyAfterDepth == 0) {
            notifyAfterUpdates?.forEach {
                notifyListeners(it)
            }
            notifyAfterUpdates = null
        }
    }


}


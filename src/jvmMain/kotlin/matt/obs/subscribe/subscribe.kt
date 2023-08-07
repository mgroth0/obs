package matt.obs.subscribe

import matt.collect.weak.bag.WeakBag
import matt.lang.go
import matt.model.flowlogic.latch.LatchCancelled
import matt.model.flowlogic.latch.SimpleLatch
import matt.obs.listen.event.Subscription

fun Subscription<*>.waitForThereToBeAtLeastOneNotificationThenUnsubscribe(latchMan: LatchManager) {
    val gate = latchMan.getLatch()
    whenItHasAtLeastOneNotification {
        unsubscribe()
        gate.open()
    }
    gate.await()
}


class LatchManager {
    private val latches = WeakBag<SimpleLatch>()
    private var cancellation: LatchCancelled? = null

    @Synchronized
    fun cancel(cause: Throwable?) {
        if (cancellation != null) {
            println("WARNING: ${this} cancelled twice")
        }
        latches.values().forEach { it.cancel(e = cause) }
        cancellation = LatchCancelled(cause = cause)
    }

    @Synchronized
    fun getLatch(): SimpleLatch {
        cancellation?.go { throw LatchCancelled(cause = it) }
        val l = SimpleLatch()
        latches += l
        return l
    }
}

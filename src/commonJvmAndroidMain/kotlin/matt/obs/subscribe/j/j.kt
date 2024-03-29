package matt.obs.subscribe.j

import matt.collect.weak.jvm.WeakSet
import matt.lang.common.go
import matt.model.flowlogic.latch.LatchCancelled
import matt.model.flowlogic.latch.j.SimpleThreadLatch
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
    private val latches = WeakSet<SimpleThreadLatch>()
    private var cancellation: LatchCancelled? = null

    @Synchronized
    fun cancel(cause: Throwable?) {
        if (cancellation != null) {
            println("WARNING: $this cancelled twice")
        }
        latches.forEach { it.cancel(e = cause) }
        cancellation = LatchCancelled(cause = cause)
    }

    @Synchronized
    fun getLatch(): SimpleThreadLatch {
        cancellation?.go { throw LatchCancelled(cause = it) }
        val l = SimpleThreadLatch()
        latches += l
        return l
    }
}

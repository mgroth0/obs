@file:JvmName("ListenJvmKt")

package matt.obs.listen

import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.obs.prop.ObsVal

fun <T> ObsVal<T>.awaitThisToBe(t: T) {
    val latch = SimpleThreadLatch()
    whenEqualsOnce(t) {
        latch.open()
    }
    latch.await()
}



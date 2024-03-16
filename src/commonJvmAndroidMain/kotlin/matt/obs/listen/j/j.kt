
package matt.obs.listen.j

import matt.model.flowlogic.latch.j.SimpleThreadLatch
import matt.obs.listen.whenEqualsOnce
import matt.obs.prop.ObsVal

inline fun <reified T> ObsVal<T>.awaitThisToBe(t: T) {
    val latch = SimpleThreadLatch()
    whenEqualsOnce(t) {
        latch.open()
    }
    latch.await()
}



@file:JvmName("ListenJvmKt")

package matt.obs.listen

import matt.model.flowlogic.latch.SimpleLatch
import matt.obs.prop.ObsVal

fun <T> ObsVal<T>.awaitThisToBe(t: T) {
  val latch = SimpleLatch()
  whenEqualsOnce(t) {
	latch.open()
  }
  latch.await()
}
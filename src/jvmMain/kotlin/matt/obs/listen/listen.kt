@file:JvmName("ListenJvmKt")

package matt.obs.listen

import matt.model.flowlogic.latch.SimpleLatch
import matt.obs.col.change.QueueChange
import matt.obs.col.change.SetChange
import matt.obs.listen.update.QueueUpdate
import matt.obs.listen.update.SetUpdate
import matt.obs.prop.ObsVal

fun <T> ObsVal<T>.awaitThisToBe(t: T) {
  val latch = SimpleLatch()
  whenEqualsOnce(t) {
	latch.open()
  }
  latch.await()
}


class QueueListener<E>(invoke: CollectionListener<E, QueueChange<E>, QueueUpdate<E>>.(change: QueueChange<E>)->Unit):
	CollectionListener<E, QueueChange<E>, QueueUpdate<E>>(
	  invoke
	)
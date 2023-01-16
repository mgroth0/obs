package matt.obs.col.col

import matt.obs.col.InternallyBackedOCollection
import matt.obs.col.change.QueueChange
import matt.obs.listen.CollectionListener
import matt.obs.listen.QueueListener
import matt.obs.listen.update.QueueUpdate

abstract class InternallyBackedOQueue<E> internal constructor(): InternallyBackedOCollection<E, QueueChange<E>, QueueUpdate<E>, QueueListener<E>>() {
  override fun updateFrom(c: QueueChange<E>): QueueUpdate<E> {
	return QueueUpdate(c)
  }

  override fun createListener(invoke: CollectionListener<E, QueueChange<E>, QueueUpdate<E>>.(change: QueueChange<E>)->Unit): QueueListener<E> {
	val l = QueueListener<E>(invoke)
	return l
  }
}
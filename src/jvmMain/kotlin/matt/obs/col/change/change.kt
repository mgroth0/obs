package matt.obs.col.change

import java.util.Queue

sealed interface QueueChange<E>: CollectionChange<E, Queue<E>>


class QueueAdd<E>(val added: E, override val collection: Queue<E>): QueueChange<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T): QueueAdd<T> {
	return QueueAdd(convert(added), collection as Queue<T>)
  }
}

class QueueRemove<E>(val removed: E, override val collection: Queue<E>): QueueChange<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T): QueueRemove<T> {
	return QueueRemove(convert(removed), collection as Queue<T>)
  }
}

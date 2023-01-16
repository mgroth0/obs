package matt.obs.queue

import matt.obs.col.col.InternallyBackedOQueue
import matt.obs.col.change.QueueAdd
import matt.obs.col.change.QueueRemove
import java.util.Queue

fun <E> Queue<E>.wrapInObservableQueue(): ObservableQueue<E> {
  require(this !is ObservableQueue)
  return ObservableQueue(this)
}

class ObservableQueue<E> internal constructor(private val q: Queue<E>): InternallyBackedOQueue<E>(), Queue<E>, Collection<E> by q {

  override fun add(element: E): Boolean {
	val b = q.add(element)
	emitChange(QueueAdd(added = element, collection = this))
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	TODO("Not yet implemented")
  }

  override fun clear() {
	TODO("Not yet implemented")
  }

  override fun remove(): E {
	TODO("Not yet implemented")
  }

  override fun poll(): E? {
	val e = q.poll()
	if (e != null) emitChange(QueueRemove(removed = e, collection = this))
	return e
  }

  override fun element(): E {
	TODO("Not yet implemented")
  }

  override fun peek(): E {
	TODO("Not yet implemented")
  }

  override fun offer(e: E): Boolean {
	TODO("Not yet implemented")
  }

  override fun retainAll(elements: Collection<E>): Boolean {
	TODO("Not yet implemented")
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	TODO("Not yet implemented")
  }

  override fun remove(element: E): Boolean {
	TODO("Not yet implemented")
  }

  override fun iterator(): MutableIterator<E> {
	TODO("Not yet implemented")
  }
}
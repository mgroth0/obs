package matt.obs.queue

import matt.obs.col.ObservableCollectionImpl
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.RemoveAt
import java.util.Queue

fun <E> Queue<E>.wrapInObservableQueue(): ObservableQueue<E> {
  require(this !is ObservableQueue)
  return ObservableQueue(this)
}

class ObservableQueue<E> internal constructor(private val q: Queue<E>): ObservableCollectionImpl<E>(), Queue<E>,
																		Collection<E> by q {

  override fun add(element: E): Boolean {
	val b = q.add(element)
	emitChange(AddAtEnd(added = element, collection = this))
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
	if (e != null) emitChange(RemoveAt(index = 0, removed = e, collection = this))
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
package matt.obs.olist

import matt.stream.itr.MutableIteratorWrapper
import matt.stream.itr.MutableListIteratorWrapper


fun <E> Collection<E>.toBasicObservableList(): BasicObservableList<E> {
  return BasicObservableList(this)
}

fun <E> Iterable<E>.toBasicObservableList(): BasicObservableList<E> {
  return BasicObservableList(this.toList())
}

fun <E> Sequence<E>.toBasicObservableList(): BasicObservableList<E> {
  return BasicObservableList(this.toList())
}

interface MObservable<T> {
  fun onChange(listener: (T)->Unit): (T)->Unit
  fun onChangeUntil(until: (T)->Boolean, listener: (T)->Unit)
  fun onChangeOnce(listener: (T)->Unit) = onChangeUntil({ true }, listener)
}

sealed interface CollectionChange<E> {
  val collection: Collection<E>
}

sealed class Addition<E>(override val collection: Collection<E>, val added: E): CollectionChange<E>
class AddAtEnd<E>(collection: Collection<E>, added: E): Addition<E>(collection, added)

class AddAt<E>(collection: Collection<E>, added: E, val index: Int): Addition<E>(collection, added)

sealed class MultiAddition<E>(override val collection: Collection<E>, val added: Collection<E>): CollectionChange<E>
class MultiAddAtEnd<E>(collection: Collection<E>, added: Collection<E>): MultiAddition<E>(collection, added)

class MultiAddAt<E>(collection: Collection<E>, added: Collection<E>, val index: Int):
  MultiAddition<E>(collection, added)

sealed class Removal<E>(override val collection: Collection<E>, val removed: E): CollectionChange<E>
class RemoveElement<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed)
class RemoveAt<E>(collection: Collection<E>, removed: E, val index: Int): Removal<E>(collection, removed)
class RemoveFirst<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed)

sealed class MultiRemoval<E>(override val collection: Collection<E>, val removed: Collection<E>): CollectionChange<E>
class RemoveElements<E>(collection: Collection<E>, removed: Collection<E>): MultiRemoval<E>(collection, removed)

class RetainAll<E>(collection: Collection<E>, removed: Collection<E>, val retained: Collection<E>):
  MultiRemoval<E>(collection, removed)

sealed class Replacement<E>(override val collection: Collection<E>, val removed: E, val added: E): CollectionChange<E>
class ReplaceElement<E>(collection: Collection<E>, removed: E, added: E): Replacement<E>(collection, removed, added)

class ReplaceAt<E>(collection: Collection<E>, removed: E, added: E, val index: Int):
  Replacement<E>(collection, removed, added)

class Clear<E>(override val collection: Collection<E>): CollectionChange<E>


abstract class BasicObservableCollection<E>: MObservable<CollectionChange<E>>, Collection<E> {
  private val listeners = mutableListOf<(CollectionChange<E>)->Unit>()
  override fun onChange(listener: (CollectionChange<E>)->Unit): (CollectionChange<E>)->Unit {
	listeners.add(listener)
	return listener
  }

  protected fun change(change: CollectionChange<E>) {
	listeners.forEach { it(change) }
  }

  override fun onChangeUntil(until: (CollectionChange<E>)->Boolean, listener: (CollectionChange<E>)->Unit) {
	var realListener: ((CollectionChange<E>)->Unit)? = null
	realListener = { t: CollectionChange<E> ->
	  listener(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }


}

inline fun <reified E, reified T: BasicObservableList<E>> T.withChangeListener(noinline listener: (CollectionChange<E>)->Unit): T {
  onChange(listener)
  return this
}


class BasicObservableList<E>(c: Collection<E> = mutableListOf()): BasicObservableCollection<E>(), MutableList<E> {


  private val list = c.toMutableList()


  override val size: Int
	get() = list.size

  override fun contains(element: E): Boolean {
	return list.contains(element)
  }

  override fun containsAll(elements: Collection<E>): Boolean {
	return list.containsAll(elements)
  }

  override fun get(index: Int): E {
	return list[index]
  }

  override fun indexOf(element: E): Int {
	return list.indexOf(element)
  }

  override fun isEmpty(): Boolean {
	return list.isEmpty()
  }

  override fun iterator(): MutableIterator<E> = listIterator()

  override fun lastIndexOf(element: E): Int {
	return list.lastIndexOf(element)
  }

  override fun add(element: E): Boolean {
	val b = list.add(element)
	require(b)
	if (b) {
	  change(AddAtEnd(list, element))
	}
	return b
  }

  override fun add(index: Int, element: E) {
	list.add(index, element)
	change(AddAt(list, element, index))
  }

  override fun addAll(index: Int, elements: Collection<E>): Boolean {
	val b = list.addAll(index, elements)
	if (b) change(MultiAddAt(list, elements, index))
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	val b = list.addAll(elements)
	if (b) change(MultiAddAtEnd(list, elements))
	return b
  }

  override fun clear() {
	list.clear()
	change(Clear(list))
  }


  override fun listIterator(): MutableListIterator<E> = lItr()
  override fun listIterator(index: Int): MutableListIterator<E> = lItr(index)

  private fun lItr(index: Int? = null) = object: MutableListIteratorWithSomeMemory<E>(list, index) {

	override fun remove() {
	  println("${hashCode()} in remove 1")
	  super.remove()
	  println("${hashCode()} in remove 2")
	  change(RemoveAt(list, lastReturned!!, lastIndex))
	  println("${hashCode()} in remove 3")
	}

	override fun add(element: E) {
	  super.add(element)
	  change(AddAt(list, element, lastIndex))
	}

	override fun set(element: E) {
	  super.set(element)
	  change(ReplaceAt(list, lastReturned!!, element, index = lastIndex))
	}
  }


  override fun remove(element: E): Boolean {
	val b = list.remove(element)
	if (b) change(RemoveElement(list, element))
	return b
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	val b = list.removeAll(elements)
	if (b) change(RemoveElements(list, elements))
	return b
  }

  override fun removeAt(index: Int): E {
	val e = list.removeAt(index)
	change(RemoveAt(list, e, index))
	return e
  }

  override fun retainAll(elements: Collection<E>): Boolean {
	val toRemove = list.filter { it !in elements }
	val b = list.retainAll(elements)
	if (b) change(RetainAll(list, toRemove, retained = elements))
	return b
  }

  override fun set(index: Int, element: E): E {
	val oldElement = list.set(index, element)
	change(ReplaceAt(list, removed = oldElement, added = element, index = index))
	return oldElement
  }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
	return list.subList(fromIndex, toIndex)
  }

}

open class MutableIteratorWithSomeMemory<E>(list: MutableCollection<E>):
  MutableIteratorWrapper<E>(list) {
  var hadFirstReturn = false
  var lastReturned: E? = null
  override val itrWrapper: (()->E)->E = {
	val r = it()
	hadFirstReturn = true
	lastReturned = r
	r
  }
}

open class MutableListIteratorWithSomeMemory<E>(list: MutableList<E>, index: Int? = null):
  MutableListIteratorWrapper<E>(
	list, index = index
  ) {
  var hadFirstReturn = false
  var lastReturned: E? = null
  override val itrWrapper: (()->E)->E = {
	val r = it()
	hadFirstReturn = true
	lastReturned = r
	r
  }
}
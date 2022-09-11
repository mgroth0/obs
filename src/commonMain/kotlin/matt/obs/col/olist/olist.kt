@file:OptIn(ExperimentalContracts::class)

package matt.obs.col.olist

import matt.collect.itr.MutableListIteratorWithSomeMemory
import matt.lang.go
import matt.lang.reflect.isSubTypeOf
import matt.obs.BaseBasicWritableOList
import matt.obs.BasicROObservableList
import matt.obs.BasicWritableObservableList
import matt.obs.JAVAFX_OBSERVABLE_CLASS
import matt.obs.col.AddAt
import matt.obs.col.AddAtEnd
import matt.obs.col.Clear
import matt.obs.col.CollectionChange
import matt.obs.col.MultiAddAt
import matt.obs.col.MultiAddAtEnd
import matt.obs.col.RemoveAt
import matt.obs.col.RemoveElement
import matt.obs.col.RemoveElements
import matt.obs.col.ReplaceAt
import matt.obs.col.RetainAll
import kotlin.contracts.ExperimentalContracts


fun <E> basicROObservableListOf(vararg elements: E): BasicROObservableList<E> =
  BasicObservableListImpl(elements.toList())

fun <E> basicMutableObservableListOf(vararg elements: E): BasicWritableObservableList<E> =
  BasicObservableListImpl(elements.toList())

class BasicObservableListImpl<E> private constructor(private val list: MutableList<E>):
  BaseBasicWritableOList<E>(), List<E> by list {

  constructor(c: Collection<E>): this(c.apply {
	JAVAFX_OBSERVABLE_CLASS?.go {
	  require(!c::class.isSubTypeOf(it)) {
		"this is the wrong way to make a BasicObservableList from an ObservableList if you want them to be synced"
	  }
	}
  }.toMutableList())

  constructor(): this(mutableListOf())

  override fun iterator(): MutableIterator<E> = listIterator()

  override fun add(element: E): Boolean {
	val b = list.add(element)
	require(b)
	if (b) {
	  emitChange(AddAtEnd(list, element))
	}
	return b
  }

  override fun add(index: Int, element: E) {
	list.add(index, element)
	emitChange(AddAt(list, element, index))
  }

  override fun addAll(index: Int, elements: Collection<E>): Boolean {
	val b = list.addAll(index, elements)
	if (b) emitChange(MultiAddAt(list, elements, index))
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	val b = list.addAll(elements)
	if (b) emitChange(MultiAddAtEnd(list, elements))
	return b
  }

  override fun clear() {
	val removed = list.toList()
	list.clear()
	emitChange(Clear(list, removed = removed))
  }


  override fun listIterator(): MutableListIterator<E> = lItr()
  override fun listIterator(index: Int): MutableListIterator<E> = lItr(index)

  private fun lItr(index: Int? = null) = object: MutableListIteratorWithSomeMemory<E>(list, index) {

	override fun remove() {
	  super.remove()
	  emitChange(RemoveAt(list, lastReturned!!, lastIndex))
	}

	override fun add(element: E) {
	  super.add(element)
	  emitChange(AddAt(list, element, lastIndex))
	}

	override fun set(element: E) {
	  super.set(element)
	  emitChange(ReplaceAt(list, lastReturned!!, element, index = lastIndex))
	}
  }


  override fun remove(element: E): Boolean {
	val b = list.remove(element)
	if (b) emitChange(RemoveElement(list, element))
	return b
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	val b = list.removeAll(elements)
	if (b) emitChange(RemoveElements(list, elements))
	return b
  }

  override fun removeAt(index: Int): E {
	val e = list.removeAt(index)
	emitChange(RemoveAt(list, e, index))
	return e
  }

  override fun retainAll(elements: Collection<E>): Boolean {
	val toRemove = list.filter { it !in elements }
	val b = list.retainAll(elements)
	if (b) emitChange(RetainAll(list, toRemove, retained = elements))
	return b
  }

  override fun set(index: Int, element: E): E {
	val oldElement = list.set(index, element)
	emitChange(ReplaceAt(list, removed = oldElement, added = element, index = index))
	return oldElement
  }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
	return list.subList(fromIndex, toIndex)
  }


}


inline fun <reified E, reified T: BasicObservableListImpl<E>> T.withChangeListener(noinline listener: (CollectionChange<E>)->Unit): T {
  onChange(listener)
  return this
}


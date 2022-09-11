@file:OptIn(ExperimentalContracts::class)

package matt.obs.col.olist

import matt.collect.itr.MutableListIteratorWithSomeMemory
import matt.lang.setAll
import matt.lang.weak.WeakRef
import matt.lang.weak.getValue
import matt.obs.MObservable
import matt.obs.col.BasicOCollection
import matt.obs.col.ObservableCollectionImpl
import matt.obs.col.change.AddAt
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.Clear
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveElement
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.ReplaceAt
import matt.obs.col.change.RetainAll
import matt.obs.col.change.mirror
import matt.obs.col.olist.filtered.BasicFilteredList
import matt.obs.col.olist.sorted.BasicSortedList
import matt.obs.prop.ValProp
import matt.obs.requireNotObservable
import kotlin.contracts.ExperimentalContracts
import kotlin.jvm.Synchronized

interface BasicROObservableList<E>: BasicOCollection<E> {
  fun filtered(filter: (E)->Boolean) = BasicFilteredList(this, filter)
}

interface ListThatCanBind<E> {
  fun unbind()
  fun <S> bind(source: BasicObservableListImpl<S>, converter: (S)->E)
  fun <S> bind(source: ValProp<S>, converter: (S)->List<E>)
}


class ListBindingHelper<E>(private val list: MutableList<E>): ListThatCanBind<E> {
  private var theBind: TheBind<*>? = null

  @Synchronized override fun unbind() {
	theBind?.cut()
	theBind = null
  }

  @Synchronized override fun <S> bind(source: BasicObservableListImpl<S>, converter: (S)->E) {
	unbind()
	list.setAll(source.map(converter))
	val listener = source.onChange {
	  list.mirror(it, converter)
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun <S> bind(source: ValProp<S>, converter: (S)->List<E>) {
	unbind()
	list.setAll(converter(source.value))
	val listener = source.onChange {
	  list.setAll(converter(it))
	}
	theBind = TheBind(source = source, listener = listener)
  }
}

fun <E: Comparable<E>> BasicROObservableList<E>.sorted() = BasicSortedList(this)
interface BasicWritableObservableList<E>: MutableList<E>, BasicROObservableList<E>


abstract class BaseBasicWritableOList<E>(list: MutableList<E>): ObservableCollectionImpl<E>(), BasicROObservableList<E>,
																BasicWritableObservableList<E>,
																ListThatCanBind<E> by ListBindingHelper(list)

private class TheBind<L>(
  source: MObservable<L, *>, private val listener: L
) {
  val source by WeakRef(source)
  fun cut() = source?.removeListener(listener)
}


fun <E> Collection<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(this)
}

fun <E> Iterable<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(this.toList())
}

fun <E> Sequence<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(this.toList())
}


fun <E> basicROObservableListOf(vararg elements: E): BasicROObservableList<E> =
  BasicObservableListImpl(elements.toList())

fun <E> basicMutableObservableListOf(vararg elements: E): BasicWritableObservableList<E> =
  BasicObservableListImpl(elements.toList())

class BasicObservableListImpl<E> private constructor(private val list: MutableList<E>): BaseBasicWritableOList<E>(list),
																						List<E> by list {

  constructor(c: Collection<E>): this(c.requireNotObservable().toMutableList())

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


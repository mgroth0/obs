package matt.obs.col.olist

import matt.collect.itr.MutableListIteratorWithSomeMemory
import matt.lang.NOT_IMPLEMENTED
import matt.lang.comparableComparator
import matt.lang.weak.WeakRef
import matt.obs.bind.MyBinding
import matt.obs.bindhelp.BindableList
import matt.obs.bindhelp.BindableListImpl
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOCollection
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
import matt.obs.col.olist.dynamic.BasicFilteredList
import matt.obs.col.olist.dynamic.BasicSortedList
import matt.obs.col.olist.dynamic.DynamicList
import matt.obs.fx.requireNotObservable
import matt.obs.prop.MObservableVal

interface ObsList<E>: BasicOCollection<E>, BindableList<E>, List<E> {
  fun filtered(filter: (E)->Boolean): BasicFilteredList<E> = DynamicList(this, filter = filter)

  fun sorted(comparator: Comparator<in E>? = null): BasicSortedList<E> = DynamicList(this, comparator = comparator)

  fun onChangeWithWeak(
	o: Any, op: ()->Unit
  ) = run {
	val weakRef = WeakRef(o)
	onChange {
	  op()
	}.apply {
	  removeCondition = { weakRef.deref() == null }
	}
  }


}


fun <E: Comparable<E>> ObsList<E>.sorted(): BasicSortedList<E> =
  DynamicList(this, comparator = comparableComparator())


interface MutableObsList<E>: MutableList<E>, ObsList<E>


abstract class BaseBasicWritableOList<E>(list: MutableList<E>): InternallyBackedOCollection<E>(),
																ObsList<E>,
																MutableObsList<E>,
																BindableList<E> by BindableListImpl(list) {
  fun <R> lazyBinding(
	vararg dependencies: MObservableVal<*, *, *>,
	op: (Collection<E>)->R,
  ): MyBinding<R> {
	val prop = this
	return MyBinding { op(prop) }.apply {
	  prop.onChange {
		markInvalid()    //	  value = op(prop)
	  }
	  dependencies.forEach {
		it.onChange {
		  markInvalid()        //		value = op(prop)
		}
	  }
	}
  }
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


fun <E> basicROObservableListOf(vararg elements: E): ObsList<E> =
  BasicObservableListImpl(elements.toList())

fun <E> basicMutableObservableListOf(vararg elements: E): MutableObsList<E> =
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
	  emitChange(RemoveAt(list, lastReturned!!, currentIndex))
	}

	override fun add(element: E) {
	  super.add(element)
	  emitChange(AddAt(list, element, currentIndex))
	}

	override fun set(element: E) {
	  super.set(element)
	  NOT_IMPLEMENTED /*would need to track whether last call was a next() or a previous()*/
	  @Suppress("UNREACHABLE_CODE")
	  emitChange(ReplaceAt(list, lastReturned!!, element, index = -99999999))
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
	require(index >= 0)
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


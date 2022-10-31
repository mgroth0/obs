package matt.obs.col.olist

import matt.collect.fake.FakeMutableList
import matt.collect.itr.FakeMutableIterator
import matt.collect.itr.FakeMutableListIterator
import matt.collect.itr.ItrDir.NEXT
import matt.collect.itr.ItrDir.PREVIOUS
import matt.collect.itr.MutableListIteratorWithSomeMemory
import matt.lang.ILLEGAL
import matt.lang.NEVER
import matt.lang.NeedsTest
import matt.lang.comparableComparator
import matt.lang.sync.inSync
import matt.lang.weak.WeakRef
import matt.obs.bind.MyBinding
import matt.obs.bindhelp.BindableList
import matt.obs.bindhelp.BindableListImpl
import matt.obs.bindings.bool.ObsB
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOCollection
import matt.obs.col.change.AddAt
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.Clear
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveElementFromList
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.ReplaceAt
import matt.obs.col.change.RetainAll
import matt.obs.col.olist.dynamic.BasicFilteredList
import matt.obs.col.olist.dynamic.BasicSortedList
import matt.obs.col.olist.dynamic.DynamicList
import matt.obs.fx.requireNotObservable
import matt.obs.prop.MObservableVal
import matt.obs.prop.ObsVal
import kotlin.jvm.Synchronized

interface ObsList<E>: BasicOCollection<E>, BindableList<E>, List<E> {
  fun filtered(filter: (E)->Boolean): BasicFilteredList<E> = DynamicList(this, filter = filter)
  fun dynamicallyFiltered(filter: (E)->ObsB): BasicFilteredList<E> = DynamicList(this, dynamicFilter = filter)

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

fun <E> ObsList<E>.toFakeMutableObsList() = FakeMutableObsList(this)


class FakeMutableObsList<E>(private val o: ObsList<E>): ObsList<E> by o, MutableObsList<E> {
  override fun add(element: E) = ILLEGAL

  override fun add(index: Int, element: E) = ILLEGAL

  override fun addAll(index: Int, elements: Collection<E>) = ILLEGAL

  override fun addAll(elements: Collection<E>) = ILLEGAL

  override fun clear() = ILLEGAL

  override fun remove(element: E) = ILLEGAL

  override fun removeAll(elements: Collection<E>) = ILLEGAL

  override fun removeAt(index: Int) = ILLEGAL

  override fun retainAll(elements: Collection<E>) = ILLEGAL

  override fun set(index: Int, element: E) = ILLEGAL

  override fun iterator(): MutableIterator<E> {
	return FakeMutableIterator(o.iterator())
  }

  override fun listIterator(): MutableListIterator<E> {
	return FakeMutableListIterator(o.listIterator())
  }

  override fun listIterator(index: Int): MutableListIterator<E> {
	return FakeMutableListIterator(o.listIterator(index))
  }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
	return FakeMutableList(o.subList(fromIndex, toIndex))
  }
}

interface MutableObsList<E>: MutableList<E>, ObsList<E>


abstract class BaseBasicWritableOList<E>: InternallyBackedOCollection<E>(),
										  ObsList<E>,
										  MutableObsList<E>,
										  BindableList<E> {

  val bindableListHelper by lazy { BindableListImpl(this) }
  override fun <S> bind(source: ObsList<S>, converter: (S)->E) = bindableListHelper.bind(source, converter)
  override fun <T> bind(source: ObsVal<T>, converter: (T)->List<E>) = bindableListHelper.bind(source, converter)
  final override val bindManager get() = bindableListHelper.bindManager
  override var theBind
	get() = bindManager.theBind
	set(value) {
	  bindManager.theBind = value
	}

  override fun unbind() = bindableListHelper.unbind()


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


fun <E> Array<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(toList())
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


fun <E> ObsList<E>.toMutableObsList() = toBasicObservableList()

open class BasicObservableListImpl<E> private constructor(private val list: MutableList<E>):
  BaseBasicWritableOList<E>(),
  List<E> by list {


  constructor(c: Iterable<E>): this(c.requireNotObservable().toMutableList())

  constructor(): this(mutableListOf())

  override fun iterator(): MutableIterator<E> = listIterator()

  override fun add(element: E): Boolean {
	val b = list.add(element)
	require(b)
	if (b) {
	  changedFromOuter(AddAtEnd(list, element))
	}
	return b
  }

  override fun add(index: Int, element: E) {
	list.add(index, element)
	changedFromOuter(AddAt(list, element, index))
  }

  override fun addAll(index: Int, elements: Collection<E>): Boolean {
	val b = list.addAll(index, elements)
	if (b) changedFromOuter(MultiAddAt(list, elements, index))
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	val b = list.addAll(elements)
	if (b) changedFromOuter(MultiAddAtEnd(list, elements))
	return b
  }

  override fun clear() {
	val removed = list.toList()
	list.clear()
	changedFromOuter(Clear(list, removed = removed))
  }


  override fun listIterator(): MutableListIterator<E> = lItr()
  override fun listIterator(index: Int): MutableListIterator<E> = lItr(index)

  private fun lItr(index: Int? = null) = object: MutableListIteratorWithSomeMemory<E>(list, index) {

	override fun remove() {
	  super.remove()
	  changedFromOuter(RemoveAt(list, lastReturned!!, currentIndex))
	}

	override fun add(element: E) {
	  super.add(element)
	  changedFromOuter(AddAt(list, element, currentIndex))
	}

	@NeedsTest
	override fun set(element: E) {
	  super.set(element)
	  changedFromOuter(
		ReplaceAt(
		  list, lastReturned!!, element, index = when (lastItrDir) {
			NEXT     -> {
			  currentIndex - 1
			}

			PREVIOUS -> {
			  currentIndex
			}

			else     -> NEVER
		  }
		)
	  )
	}
  }


  override fun remove(element: E): Boolean {
	val i = list.indexOf(element)
	val b = list.remove(element)
	if (b) changedFromOuter(RemoveElementFromList(list, element, i))
	return b
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	val b = list.removeAll(elements)
	if (b) changedFromOuter(RemoveElements(list, elements))
	return b
  }

  override fun removeAt(index: Int): E {
	require(index >= 0)
	val e = list.removeAt(index)
	changedFromOuter(RemoveAt(list, e, index))
	return e
  }

  override fun retainAll(elements: Collection<E>): Boolean {
	val toRemove = list.filter { it !in elements }
	val b = list.retainAll(elements)
	if (b) changedFromOuter(RetainAll(list, toRemove, retained = elements))
	return b
  }

  override fun set(index: Int, element: E): E {
	val oldElement = list.set(index, element)
	changedFromOuter(ReplaceAt(list, removed = oldElement, added = element, index = index))
	return oldElement
  }

  fun changedFromOuter(c: CollectionChange<E>) {
	invalidateSubLists()
	emitChange(c)
  }


  @Synchronized
  override fun subList(fromIndex: Int, toIndex: Int) = SubList(fromIndex, toIndex)

  @Synchronized
  private fun invalidateSubLists() {
	validSubLists.forEach { it.isValid = false }
	validSubLists.clear()
  }

  private var validSubLists = mutableListOf<SubList>()

  inner class SubList(
	private val fromIndex: Int,
	toIndexExclusive: Int
  ):
	MutableList<E> {
	internal var isValid = true

	private val subList = list.subList(fromIndex, toIndexExclusive)

	init {
	  validSubLists += this
	}

	override val size: Int
	  get() {
		return inSync(this@BasicObservableListImpl) {
		  require(isValid)
		  subList.size
		}
	  }

	override fun clear() {
	  inSync(this@BasicObservableListImpl) {
		require(isValid)

		val copy = subList.toList()

		subList.clear()

		emitChange(RemoveElements(collection = this@BasicObservableListImpl, removed = copy))
	  }

	}

	override fun addAll(elements: Collection<E>): Boolean {
	  TODO("Not yet implemented")
	}

	override fun addAll(index: Int, elements: Collection<E>): Boolean {
	  TODO("Not yet implemented")
	}

	override fun add(index: Int, element: E) {
	  TODO("Not yet implemented")
	}

	override fun add(element: E): Boolean {
	  require(isValid)
	  val b = subList.add(element)
	  require(b)
	  emitChange(AddAt(this@BasicObservableListImpl, element, fromIndex + subList.size - 1))
	  return b
	}

	override fun get(index: Int): E {
	  require(isValid)
	  return subList[index]
	}

	override fun isEmpty(): Boolean {
	  require(isValid)
	  return subList.isEmpty()
	}

	override fun iterator(): MutableIterator<E> {
	  val itr by lazy { subList.iterator() }
	  return FakeMutableIterator<E>(object: Iterator<E> {
		override fun hasNext(): Boolean {
		  require(isValid)
		  return itr.hasNext()
		}

		override fun next(): E {
		  require(isValid)
		  return itr.next()
		}
	  })
	}

	override fun listIterator(): MutableListIterator<E> {
	  TODO("Not yet implemented")
	}

	override fun listIterator(index: Int): MutableListIterator<E> {
	  TODO("Not yet implemented")
	}

	override fun removeAt(index: Int): E {
	  TODO("Not yet implemented")
	}

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
	  TODO("Not yet implemented")
	}

	override fun set(index: Int, element: E): E {
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

	override fun lastIndexOf(element: E): Int {
	  require(isValid)
	  return subList.lastIndexOf(element)
	}

	override fun indexOf(element: E): Int {
	  require(isValid)
	  return subList.indexOf(element)
	}

	override fun containsAll(elements: Collection<E>): Boolean {
	  require(isValid)
	  return subList.containsAll(elements)
	}

	override fun contains(element: E): Boolean {
	  require(isValid)
	  return subList.contains(element)
	}
  }


}


inline fun <reified E, reified T: BasicObservableListImpl<E>> T.withChangeListener(noinline listener: (CollectionChange<E>)->Unit): T {
  onChange(listener)
  return this
}





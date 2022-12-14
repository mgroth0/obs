package matt.obs.col.olist

import matt.collect.fake.FakeMutableList
import matt.collect.itr.FakeMutableIterator
import matt.collect.itr.FakeMutableListIterator
import matt.collect.itr.ItrDir.NEXT
import matt.collect.itr.ItrDir.PREVIOUS
import matt.collect.itr.MutableListIteratorWithSomeMemory
import matt.lang.ILLEGAL
import matt.lang.NEVER
import matt.lang.NOT_IMPLEMENTED
import matt.lang.anno.NeedsTest
import matt.lang.comparableComparator
import matt.lang.function.Consume
import matt.lang.sync.inSync
import matt.lang.weak.WeakRef
import matt.model.op.prints.Prints
import matt.obs.bind.MyBinding
import matt.obs.bindhelp.BindableList
import matt.obs.bindhelp.BindableListImpl
import matt.obs.bindings.bool.ObsB
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOCollection
import matt.obs.col.change.AddAt
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.AdditionBase
import matt.obs.col.change.Clear
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemovalBase
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveElementFromList
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.ReplaceAt
import matt.obs.col.change.RetainAll
import matt.obs.col.olist.dynamic.BasicFilteredList
import matt.obs.col.olist.dynamic.BasicSortedList
import matt.obs.col.olist.dynamic.DynamicList
import matt.obs.fx.requireNotObservable
import matt.obs.listen.CollectionListener
import matt.obs.listen.CollectionListenerBase
import matt.obs.listen.MyListenerInter
import matt.obs.listen.WeakCollectionListener
import matt.obs.prop.MObservableVal
import matt.obs.prop.ObsVal
import kotlin.jvm.Synchronized

interface ImmutableObsList<E>: BasicOCollection<E>, List<E>

interface ObsList<E>: ImmutableObsList<E>, BindableList<E>, MutableList<E> {
  fun filtered(filter: (E)->Boolean): BasicFilteredList<E> = DynamicList(this, filter = filter)
  fun dynamicallyFiltered(filter: (E)->ObsB): BasicFilteredList<E> = DynamicList(this, dynamicFilter = filter)

  fun sorted(comparator: Comparator<in E>? = null): BasicSortedList<E> = DynamicList(this, comparator = comparator)

  fun <W: Any> onChangeWithWeak(
	o: W, op: (W, CollectionChange<E>)->Unit
  ) = run {
	val weakRef = WeakRef(o)
	onChangeWithAlreadyWeak(weakRef) { w, c ->
	  op(w, c)
	}
  }

  fun <W: Any> onChangeWithAlreadyWeak(weakRef: WeakRef<W>, op: (W, CollectionChange<E>)->Unit) = run {
	val listener = WeakCollectionListener<W, E>(weakRef) { o: W, c: CollectionChange<E> ->
	  op(o, c)
	}
	addListener(listener)
  }


  fun onAdd(op: Consume<E>) = listen(onAdd = op, onRemove = {})
  fun onRemove(op: Consume<E>) = listen(onAdd = { }, onRemove = op)

  fun listen(
	onAdd: ((E)->Unit),
	onRemove: ((E)->Unit),
  ) {
	addListener(CollectionListener {
	  (it as? AdditionBase)?.addedElements?.forEach { e ->
		onAdd(e)
	  }
	  (it as? RemovalBase)?.removedElements?.forEach { e ->
		onRemove(e)
	  }
	})
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
	private var toIndexExclusive: Int
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

		toIndexExclusive = fromIndex

		emitChange(RemoveElements(collection = this@BasicObservableListImpl, removed = copy))
	  }

	}

	override fun addAll(elements: Collection<E>): Boolean {
	  return inSync(this@BasicObservableListImpl) {
		require(isValid)
		val addIndex = toIndexExclusive
		toIndexExclusive += elements.size
		val r = subList.addAll(elements)
		emitChange(MultiAddAt(collection = this@BasicObservableListImpl, added = elements, index = addIndex))
		r
	  }
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
	  toIndexExclusive++
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

	override fun listIterator() = NOT_IMPLEMENTED
	override fun listIterator(index: Int) = NOT_IMPLEMENTED
	override fun removeAt(index: Int) = NOT_IMPLEMENTED
	override fun subList(fromIndex: Int, toIndex: Int) = NOT_IMPLEMENTED
	override fun set(index: Int, element: E): E {
	  require(isValid)
	  val prev = subList.set(index, element)
	  emitChange(ReplaceAt(this@BasicObservableListImpl, removed = prev, added = element, index = fromIndex + index))
	  return prev
	}

	override fun retainAll(elements: Collection<E>) = NOT_IMPLEMENTED
	override fun removeAll(elements: Collection<E>): Boolean {
	  require(isValid)
	  val prevSize = subList.size
	  val b = subList.removeAll(elements)
	  val newSize = subList.size
	  val numRemoved = prevSize - newSize
	  toIndexExclusive -= numRemoved
	  if (b) emitChange(RemoveElements(this@BasicObservableListImpl, elements))
	  return b
	}

	override fun remove(element: E): Boolean {
	  require(isValid)
	  val i = subList.indexOf(element)
	  val b = subList.remove(element)
	  if (b) toIndexExclusive--
	  if (b) emitChange(RemoveAt(this@BasicObservableListImpl, element, i + fromIndex))
	  return b
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


  fun <R> view(converter: (E)->R) = object: ImmutableObsList<R> {
	override fun onChange(listenerName: String?, op: (CollectionChange<R>)->Unit): CollectionListenerBase<R> {
	  TODO("Not yet implemented")
	}

	override val size: Int
	  get() = this@BasicObservableListImpl.size

	override fun isEmpty(): Boolean {
	  return this@BasicObservableListImpl.isEmpty()
	}

	override fun iterator(): Iterator<R> = listIterator()

	override fun addListener(listener: CollectionListenerBase<R>): CollectionListenerBase<R> {
	  TODO("Not yet implemented")
	}

	override var nam: String?
	  get() = this@BasicObservableListImpl.nam
	  set(value) {
		this@BasicObservableListImpl.nam = value
	  }

	override fun removeListener(listener: MyListenerInter) {
	  TODO("Not yet implemented")
	}

	override var debugger: Prints?
	  get() = this@BasicObservableListImpl.debugger
	  set(value) {
		this@BasicObservableListImpl.debugger = value
	  }

	override fun get(index: Int): R {
	  return converter(this@BasicObservableListImpl.get(index))
	}

	override fun listIterator() = listIterator(0)

	override fun listIterator(index: Int) = object: ListIterator<R> {
	  private val itr = this@BasicObservableListImpl.listIterator(index)
	  override fun hasNext() = itr.hasNext()

	  override fun hasPrevious(): Boolean {
		TODO("Not yet implemented")
	  }

	  override fun next() = converter(itr.next())

	  override fun nextIndex(): Int {
		TODO("Not yet implemented")
	  }

	  override fun previous(): R {
		TODO("Not yet implemented")
	  }

	  override fun previousIndex(): Int {
		TODO("Not yet implemented")
	  }

	}

	override fun subList(fromIndex: Int, toIndex: Int): List<R> {
	  TODO("Not yet implemented")
	}

	override fun lastIndexOf(element: R): Int {
	  TODO("Not yet implemented")
	}

	override fun indexOf(element: R): Int {
	  TODO("Not yet implemented")
	}

	override fun containsAll(elements: Collection<R>): Boolean {
	  TODO("Not yet implemented")
	}

	override fun contains(element: R): Boolean {
	  TODO("Not yet implemented")
	}

  }

}


inline fun <reified E, reified T: BasicObservableListImpl<E>> T.withChangeListener(
  listenerName: String? = null,
  noinline listener: (CollectionChange<E>)->Unit
): T {
  onChange(listenerName = listenerName, listener)
  return this
}





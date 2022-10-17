package matt.obs.col.olist.mappedlist


import matt.model.convert.Converter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.col.change.mirror
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.ObsList

//fun <O, E> ObservableList<O>.toMappedList(mapFun: (O)->E) = MappedList(this.createImmutableWrapper(), mapFun)
//fun <O, E> ObservableList<O>.toSyncedList(converter: matt.model.convert.Converter<O, E>) = SyncedList(this.createMutableWrapper(), converter)

//fun <O, E> MyObservableListWrapperPlusList<O>.toMappedList(mapFun: (O)->E) = MappedList(this, mapFun)
//fun <O, E> MyObservableListWrapperPlusMutableList<O>.toSyncedList(converter: matt.model.convert.Converter<O, E>) = SyncedList(this, converter)

fun <S, T> ObsList<S>.toMappedList(mapFun: (S)->T): ObsList<T> {
  val r = BasicObservableListImpl(map(mapFun))
  onChange {
	r.mirror(it, mapFun)
  }
  return r
}

fun <S, T> MutableObsList<S>.toSyncedList(converter: Converter<S, T>): MutableObsList<T> {
  val r = BasicObservableListImpl(map { converter.convertToB(it) })
  val rb = RecursionBlocker()
  onChange {
	rb.with {
	  r.mirror(it) { converter.convertToB(it) }
	}
  }
  r.onChange {
	rb.with {
//	  println("r on change c = $it")
	  mirror(it) { converter.convertToA(it) }
	}
  }
  return r
}

//interface MaybeNonNullOList<T>: ObservableList<T>, List<T>
//
//open class MappedList<O, E>(
//  sourceList: BasicROObservableList<O>,
//  mapFun: (O)->E
//): BasicObservableListImpl<E>(sourceList.map(mapFun)), List<E> {
//
////  protected val list = sourceList.map(mapFun).toMutableList()  //.toObservable()
//  protected var changingFromMappedView = false
//
//  init {
//	/*TODO: see tornadofx matt.hurricanefx.eye.collect.collectbind.ListConversionListener thing for ideas on optimization,
//	   both in terms of weak references and editing subLists*/
//	sourceList.onChange {
//	  if (!changingFromMappedView) /*synchronized(this) {*/
//	  /*individual changes seem impossible to track down since observableList listeners don't use indices?*/
//	  //		list.setAll(sourceList.map(mapFun))
//
//	  /*}*/
//		/*list.*/mirror(it, mapFun)
////		emitChange()
//	}
//  }

//  override val size: Int get() = list.size
//  override fun containsAll(elements: Collection<E>) = list.containsAll(elements)
//  override fun indexOf(element: E) = list.indexOf(element)
//  override fun contains(element: E) = list.contains(element)
//  override fun get(index: Int): E = list.get(index)
//  override fun isEmpty() = list.isEmpty()
//  override fun lastIndexOf(element: E) = list.lastIndexOf(element)

//  override fun addListener(listener: InvalidationListener?) = list.addListener(listener)
//  override fun removeListener(listener: InvalidationListener?) = list.removeListener(listener)
//  override fun addListener(listener: ListChangeListener<in E>?) = list.addListener(listener)
//  override fun removeListener(listener: ListChangeListener<in E>?) = list.removeListener(listener)

//  override fun add(element: E): Boolean = MUST_USE_SUBCLASS
//  override fun add(index: Int, element: E): Unit = MUST_USE_SUBCLASS
//  override fun addAll(vararg elements: E): Boolean = MUST_USE_SUBCLASS
//  override fun addAll(index: Int, elements: Collection<E>): Boolean = MUST_USE_SUBCLASS
//  override fun addAll(elements: Collection<E>): Boolean = MUST_USE_SUBCLASS
//  override fun clear(): Unit = MUST_USE_SUBCLASS
//  override fun remove(from: Int, to: Int): Unit = MUST_USE_SUBCLASS
//  override fun remove(element: E): Boolean = MUST_USE_SUBCLASS
//  override fun removeAll(vararg elements: E): Boolean = MUST_USE_SUBCLASS
//  override fun removeAll(elements: Collection<E>): Boolean = MUST_USE_SUBCLASS
//  override fun removeAt(index: Int): E = MUST_USE_SUBCLASS
//  override fun retainAll(vararg elements: E): Boolean = MUST_USE_SUBCLASS
//  override fun retainAll(elements: Collection<E>): Boolean = MUST_USE_SUBCLASS
//  override fun set(index: Int, element: E): E = MUST_USE_SUBCLASS
//  override fun setAll(vararg elements: E): Boolean = MUST_USE_SUBCLASS
//  override fun setAll(col: MutableCollection<out E>?): Boolean = MUST_USE_SUBCLASS
//
//  override fun iterator(): MutableIterator<E> = listIterator()
//
//  override fun listIterator(): MutableListIterator<E> =
//	MutableListIteratorWrapper(list, null) { _, _ -> MUST_USE_SUBCLASS }
//
//  override fun listIterator(index: Int): MutableListIterator<E> =
//	MutableListIteratorWrapper(list, index) { _, _ -> MUST_USE_SUBCLASS }
//
//  override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = MUST_USE_SUBCLASS

//}
//
//class SyncedList<O, E>(
//  private val sourceList: BasicWritableObservableList<O>,
//  mapFun: (O)->E,
//  val mapBackFun: (E)->O
//): MappedList<O, E>(sourceList, mapFun), MutableList<E> {
//
//  constructor(
//	sourceList: BasicWritableObservableList<O>,
//	converter: matt.model.convert.Converter<O, E>
//  ): this(
//	sourceList,
//	{ converter.convertToB(it) },
//	{ converter.convertToA(it) }
//  )
//
//  private fun <R> changeFromMapView(op: ()->R): R {
//	val r = op()
//	changingFromMappedView = true
//	/*matt.log.todo.todo:
//	*  1. optimize
//	*  2. use correct changes*/
//	sourceList.setAll(list.map { mapBackFun(it) })
//
//	changingFromMappedView = false
//	return r
//  }
//
//  override fun add(element: E) = changeFromMapView { list.add(element) }
//  override fun add(index: Int, element: E) = changeFromMapView { list.add(index, element) }
//  override fun addAll(vararg elements: E) = changeFromMapView { list.addAll(elements) }
//  override fun addAll(index: Int, elements: Collection<E>) = changeFromMapView { list.addAll(index, elements) }
//  override fun addAll(elements: Collection<E>) = changeFromMapView { list.addAll(elements) }
//  override fun clear() = changeFromMapView { list.clear() }
//  override fun remove(from: Int, to: Int) = changeFromMapView { list.remove(from, to) }
//  override fun remove(element: E) = changeFromMapView { list.remove(element) }
//  override fun removeAll(vararg elements: E) = changeFromMapView { list.removeAll(elements.toSet()) }
//  override fun removeAll(elements: Collection<E>) = changeFromMapView { list.removeAll(elements) }
//  override fun removeAt(index: Int): E = changeFromMapView { list.removeAt(index) }
//  override fun retainAll(vararg elements: E) = changeFromMapView { list.retainAll(elements.toSet()) }
//  override fun retainAll(elements: Collection<E>) = changeFromMapView { list.retainAll(elements) }
//  override fun set(index: Int, element: E): E = changeFromMapView { list.set(index, element) }
//  override fun setAll(vararg elements: E): Boolean = changeFromMapView { list.setAll(*elements) }
//  override fun setAll(col: MutableCollection<out E>?) = changeFromMapView { list.setAll(col) }
//
//  override fun iterator(): MutableIterator<E> = listIterator()
//  override fun listIterator(): MutableListIterator<E> =
//	MutableListIteratorWrapper(list, null) { _, op -> changeFromMapView(op) }
//
//  override fun listIterator(index: Int): MutableListIterator<E> =
//	MutableListIteratorWrapper(list, index) { _, op -> changeFromMapView(op) }
//
//  override fun subList(fromIndex: Int, toIndex: Int) = list.subList(fromIndex, toIndex).toFakeMutableList().apply {
//	matt.log.todo.todoOnce("make this work")
//  }
//
//}


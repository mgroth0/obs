package matt.obs.col.oset

import matt.collect.itr.MutableIteratorWithSomeMemory
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOSet
import matt.obs.col.change.AddIntoSet
import matt.obs.col.change.ClearSet
import matt.obs.col.change.MultiAddIntoSet
import matt.obs.col.change.RemoveElementFromSet
import matt.obs.col.change.RemoveElementsFromSet
import matt.obs.col.change.RetainAllSet
import matt.obs.col.change.SetChange
import matt.obs.fx.requireNotObservable
import matt.obs.listen.SetListener
import matt.obs.listen.update.SetUpdate

interface ObsSet<E>: Set<E>, BasicOCollection<E, SetChange<E>, SetUpdate<E>, SetListener<E>>

interface MutableObsSet<E>: ObsSet<E>, MutableSet<E>

fun <E> Collection<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this)
}

fun <E> Iterable<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this.toSet())
}

fun <E> Sequence<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this.toSet())
}

fun <E> basicObservableSetOf(vararg values: E) = BasicObservableSet<E>(values.toMutableSet())

class BasicObservableSet<E>(private val theSet: MutableSet<E>): InternallyBackedOSet<E>(),
																MutableObsSet<E> {


  constructor(c: Collection<E>): this(c.requireNotObservable().toMutableSet())
  constructor(): this(emptySet())


  override val size: Int
	get() = theSet.size

  override fun contains(element: E): Boolean {
	return theSet.contains(element)
  }

  override fun containsAll(elements: Collection<E>): Boolean {
	return theSet.containsAll(elements)
  }

  override fun isEmpty(): Boolean {
	return theSet.isEmpty()
  }

  override fun iterator() = object: MutableIteratorWithSomeMemory<E>(theSet) {
	override fun remove(): Unit {
	  super.remove()
	  emitChange(
		RemoveElementFromSet(theSet, lastReturned!!)
	  )
	}
  }

  override fun add(element: E): Boolean {
	val b = theSet.add(element)
	//        println("BasicObservableSet.add(${element})")
	if (b) {
	  emitChange(AddIntoSet(theSet, element))
	}
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	val b = theSet.addAll(elements)
	//        taball("set addAll",elements)
	if (b) emitChange(MultiAddIntoSet(theSet, elements))
	return b
  }

  override fun clear() {
	//        println("BasicObservableSet.clear")
	val removed = theSet.toSet()
	theSet.clear()
	emitChange(ClearSet(theSet, removed = removed))
  }

  override fun remove(element: E): Boolean {
	val b = theSet.remove(element)
	//        println("BasicObservableSet.remove(${element})")
	if (b) emitChange(RemoveElementFromSet(theSet, element))
	return b
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	val b = theSet.removeAll(elements)
	//        taball("set removeAll",elements)
	if (b) emitChange(RemoveElementsFromSet(theSet, elements))
	return b
  }


  override fun retainAll(elements: Collection<E>): Boolean {
	val toRemove = theSet.filter { it !in elements }
	val b = theSet.retainAll(elements)
	//        taball("set retainAll",elements)
	if (b) emitChange(RetainAllSet(theSet, toRemove, retained = elements))
	return b
  }

}
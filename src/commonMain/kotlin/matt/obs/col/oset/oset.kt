package matt.obs.col.oset

import matt.collect.itr.MutableIteratorWithSomeMemory
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOCollection
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.Clear
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemoveElement
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.RetainAll
import matt.obs.fx.requireNotObservable


fun <E> Collection<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this)
}

fun <E> Iterable<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this.toSet())
}

fun <E> Sequence<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this.toSet())
}

class BasicObservableSet<E>(private val theSet: MutableSet<E>): InternallyBackedOCollection<E>(),
																MutableSet<E> {


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
		RemoveElement(theSet, lastReturned!!)
	  )
	}
  }

  override fun add(element: E): Boolean {
	val b = theSet.add(element)
	//        println("BasicObservableSet.add(${element})")
	if (b) {
	  emitChange(AddAtEnd(theSet, element))
	}
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	val b = theSet.addAll(elements)
	//        taball("set addAll",elements)
	if (b) emitChange(MultiAddAtEnd(theSet, elements))
	return b
  }

  override fun clear() {
	//        println("BasicObservableSet.clear")
	val removed = theSet.toSet()
	theSet.clear()
	emitChange(Clear(theSet, removed = removed))
  }

  override fun remove(element: E): Boolean {
	val b = theSet.remove(element)
	//        println("BasicObservableSet.remove(${element})")
	if (b) emitChange(RemoveElement(theSet, element))
	return b
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	val b = theSet.removeAll(elements)
	//        taball("set removeAll",elements)
	if (b) emitChange(RemoveElements(theSet, elements))
	return b
  }


  override fun retainAll(elements: Collection<E>): Boolean {
	val toRemove = theSet.filter { it !in elements }
	val b = theSet.retainAll(elements)
	//        taball("set retainAll",elements)
	if (b) emitChange(RetainAll(theSet, toRemove, retained = elements))
	return b
  }

}
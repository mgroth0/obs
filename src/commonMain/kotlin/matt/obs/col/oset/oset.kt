package matt.obs.col.oset

import matt.obs.col.AddAtEnd
import matt.obs.col.BasicObservableCollection
import matt.obs.col.Clear
import matt.obs.col.MultiAddAtEnd
import matt.obs.col.RemoveElement
import matt.obs.col.RemoveElements
import matt.obs.col.RetainAll
import matt.obs.col.olist.MutableIteratorWithSomeMemory


fun <E> Collection<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this)
}

fun <E> Iterable<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this.toSet())
}

fun <E> Sequence<E>.toBasicObservableSet(): BasicObservableSet<E> {
  return BasicObservableSet(this.toSet())
}

class BasicObservableSet<E>(c: Collection<E> = mutableSetOf()): BasicObservableCollection<E>(), MutableSet<E> {

  private val theSet = c.toMutableSet()

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
	theSet.clear()
	emitChange(Clear(theSet))
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
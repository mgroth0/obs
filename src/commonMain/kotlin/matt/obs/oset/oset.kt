package matt.obs.oset

import matt.obs.olist.AddAtEnd
import matt.obs.olist.BasicObservableCollection
import matt.obs.olist.Clear
import matt.obs.olist.MultiAddAtEnd
import matt.obs.olist.MutableIteratorWithSomeMemory
import matt.obs.olist.RemoveElement
import matt.obs.olist.RemoveElements
import matt.obs.olist.RetainAll


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
	  change(
		RemoveElement(theSet, lastReturned!!)
	  )
	}
  }

  override fun add(element: E): Boolean {
	val b = theSet.add(element)
	//        println("BasicObservableSet.add(${element})")
	if (b) {
	  change(AddAtEnd(theSet, element))
	}
	return b
  }

  override fun addAll(elements: Collection<E>): Boolean {
	val b = theSet.addAll(elements)
	//        taball("set addAll",elements)
	if (b) change(MultiAddAtEnd(theSet, elements))
	return b
  }

  override fun clear() {
	//        println("BasicObservableSet.clear")
	theSet.clear()
	change(Clear(theSet))
  }

  override fun remove(element: E): Boolean {
	val b = theSet.remove(element)
	//        println("BasicObservableSet.remove(${element})")
	if (b) change(RemoveElement(theSet, element))
	return b
  }

  override fun removeAll(elements: Collection<E>): Boolean {
	val b = theSet.removeAll(elements)
	//        taball("set removeAll",elements)
	if (b) change(RemoveElements(theSet, elements))
	return b
  }


  override fun retainAll(elements: Collection<E>): Boolean {
	val toRemove = theSet.filter { it !in elements }
	val b = theSet.retainAll(elements)
	//        taball("set retainAll",elements)
	if (b) change(RetainAll(theSet, toRemove, retained = elements))
	return b
  }

}
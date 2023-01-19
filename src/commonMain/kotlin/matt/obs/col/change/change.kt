package matt.obs.col.change

import matt.collect.set.ordered.OrderedSet
import matt.collect.set.ordered.orderedSetOf
import matt.collect.set.ordered.toOrderedSet
import matt.log.taball
import matt.model.data.index.AdditionIndex
import matt.model.data.index.All
import matt.model.data.index.End
import matt.model.data.index.First
import matt.model.data.index.Index
import matt.model.data.index.MyIndexedValue
import matt.model.data.index.RemovalIndex
import matt.model.data.index.withIndex
import matt.model.obj.tostringbuilder.toStringBuilder
import matt.obs.col.change.atomic.compile
import matt.prim.str.elementsToString


interface CollectionChange<E, COL: Collection<E>> {
  val collection: Collection<E>
  fun <T> convert(collection: Collection<T>, convert: (E)->T): CollectionChange<T, out Collection<T>>
}

sealed interface SetChange<E>: CollectionChange<E, Set<E>> {
  override val collection: Set<E>
  override fun <T> convert(collection: Collection<T>, convert: (E)->T): SetChange<T>
}

sealed interface ListChange<E>: CollectionChange<E, List<E>> {
  override val collection: List<E>
  val lowestChangedIndex: Int
  override fun <T> convert(collection: Collection<T>, convert: (E)->T): ListChange<T>
}

class AtomicListChange<E>(
  override val collection: List<E>, val changes: List<ListChange<E>>, var isCompiled: Boolean = false
): ListChange<E>, ListAdditionBase<E>, ListRemovalBase<E> {


  override val addedElementsIndexed: OrderedSet<out MyIndexedValue<E, out AdditionIndex>>
	get() = changes.filterIsInstance<ListAdditionBase<E>>().flatMap { it.addedElementsIndexed }.toOrderedSet()
  override val removedElementsIndexed: OrderedSet<out MyIndexedValue<E, out RemovalIndex>>
	get() = changes.filterIsInstance<ListRemovalBase<E>>().flatMap { it.removedElementsIndexed }.toOrderedSet()


  override val lowestChangedIndex: Int
	get() = run {
	  changes.minOf { it.lowestChangedIndex }
	}

  override fun <T> convert(collection: Collection<T>, convert: (E)->T): AtomicListChange<T> {
	return AtomicListChange(
	  collection as List<T>, changes.map { it.convert(collection, convert) }, isCompiled = isCompiled
	)
  }


  override fun toString(): String {
	return toStringBuilder(mapOf("changes" to changes.elementsToString()))
  }
}


sealed interface AdditionBase<E, COL: Collection<E>>: CollectionChange<E, COL> {
  val addedElements: List<E>
}

sealed interface SetAdditionBase<E>: SetChange<E>, AdditionBase<E, Set<E>>

sealed interface ListAdditionBase<E>: ListChange<E>, AdditionBase<E, List<E>> {
  val addedElementsIndexed: OrderedSet<out MyIndexedValue<E, out AdditionIndex>>
  override val addedElements: List<E> get() = addedElementsIndexed.map { it.element }
}


sealed interface RemovalBase<E, COL: Collection<E>>: CollectionChange<E, COL> {
  val removedElements: List<E>
}

sealed interface SetRemovalBase<E>: SetChange<E>, RemovalBase<E, Set<E>>

sealed interface ListRemovalBase<E>: RemovalBase<E, List<E>>, ListChange<E> {
  val removedElementsIndexed: OrderedSet<out MyIndexedValue<E, out RemovalIndex>>
  override val removedElements: List<E> get() = removedElementsIndexed.map { it.element }
}

sealed class SetAddition<E>(override val collection: Set<E>, val added: E): SetAdditionBase<E>
sealed class ListAddition<E>(override val collection: List<E>, val added: E): ListAdditionBase<E>


class AddIntoSet<E>(override val collection: Set<E>, added: E): SetAddition<E>(collection, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T): AddIntoSet<T> =
	AddIntoSet(collection as Set<T>, convert(added))

  override fun toString() = toStringBuilder("added" to added)
  override val addedElements = listOf(added)
}

class AddAtEnd<E>(override val collection: List<E>, added: E): ListAddition<E>(collection, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = AddAtEnd(collection as List<T>, convert(added))
  override fun toString() = toStringBuilder("added" to added)
  override val addedElementsIndexed get() = orderedSetOf(added withIndex End)
  override val lowestChangedIndex: Int = collection.size - 1
}

class AddAt<E>(override val collection: List<E>, added: E, val index: Int): ListAddition<E>(collection, added),
																			ListAdditionBase<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	AddAt(collection as List<T>, convert(added), index = index)

  override fun toString() = toStringBuilder("added" to added, "index" to index)

  override val addedElementsIndexed get() = orderedSetOf(added withIndex index)
  override val lowestChangedIndex: Int = index
}


sealed class MultiAddition<E, COL: Collection<E>>(
  override val collection: COL
): AdditionBase<E, COL>

class MultiAddIntoSet<E>(collection: Set<E>, val added: Collection<E>): MultiAddition<E, Set<E>>(
  collection
), SetChange<E>, SetAdditionBase<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	MultiAddIntoSet(collection as Set<T>, added.map(convert))

  override fun toString() = toStringBuilder("added" to added)

  override val addedElements get() = added.toList()
}

sealed class MultiAdditionIntoList<E>(override val collection: List<E>): ListAdditionBase<E>

class MultiAddAtEnd<E>(collection: List<E>, val added: Collection<E>): MultiAdditionIntoList<E>(collection) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	MultiAddAtEnd(collection as List<T>, added.map(convert))


  override fun toString() = toStringBuilder("added" to added)

  override val addedElementsIndexed get() = added.map { it withIndex End }.toOrderedSet()

  override val lowestChangedIndex = collection.size - added.size

}

class MultiAddAt<E>(collection: List<E>, val added: Collection<E>, val index: Int): MultiAdditionIntoList<E>(
  collection
) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	MultiAddAt(collection as List<T>, added.map(convert), index)

  override val addedElementsIndexed get() = added.mapIndexed { idx, it -> it withIndex index + idx }.toOrderedSet()


  override val lowestChangedIndex = index

  override fun toString(): String {
	return toStringBuilder(mapOf("index" to index))
  }

  val isRange by lazy {
	addedElementsIndexed.zipWithNext { a, b -> a.index.i == b.index.i - 1 }.all { it }
  }

}

sealed class Removal<E, COL: Collection<E>>(
  override val collection: COL,
  val removed: E
): RemovalBase<E, COL> { //  override val removedElements get() = listOf(removed)
}

sealed class SetRemoval<E>(override val collection: Set<E>, removed: E): Removal<E, Set<E>>(collection, removed),
																		 SetRemovalBase<E>

sealed class ListRemoval<E>(override val collection: List<E>, removed: E): Removal<E, List<E>>(collection, removed),
																		   ListRemovalBase<E>


class RemoveElementFromSet<E>(collection: Set<E>, removed: E): SetRemoval<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RemoveElementFromSet(collection as Set<T>, convert(removed))

  override val removedElements get() = listOf(removed)
}

//open class RemoveElement<E>(collection: Collection<E>, removed: E, override val lowestChangedIndex: Int): Removal<E>(
//  collection,
//  removed
//), ListRemovalBase<E> {
//  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
//	RemoveElement(collection, convert(removed), lowestChangedIndex)
//
//  override val removedElementsIndexed get() = orderedSetOf(removed withIndex First)
//
//}


class RemoveElementFromList<E>(collection: List<E>, removed: E, val index: Int): ListRemoval<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RemoveElementFromList(collection as List<T>, convert(removed), index)

  override val removedElementsIndexed get() = orderedSetOf(removed withIndex First)

  override val lowestChangedIndex: Int = index
}


class RemoveAt<E>(collection: List<E>, removed: E, val index: Int): ListRemoval<E>(collection, removed),
																	ListRemovalBase<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RemoveAt(collection as List<T>, convert(removed), index)

  override val removedElementsIndexed get() = orderedSetOf(removed withIndex index)

  override val lowestChangedIndex = index

  override fun toString(): String {
	return toStringBuilder(mapOf("index" to index, "removed" to removed))
  }

} //class RemoveFirst<E>(collection: Collection<E>, removed: E): matt.obs.map.change.Removal<E>(collection, removed)

sealed class MultiRemoval<E, COL: Collection<E>>(
  override val collection: COL, val removed: Collection<E>
): RemovalBase<E, COL> { //  override val removedElements get() = removed.toList()
}

sealed class MultiRemovalFromSet<E>(
  override val collection: Set<E>, removed: Collection<E>
): MultiRemoval<E, Set<E>>(collection, removed), SetRemovalBase<E>

sealed class MultiRemovalFromList<E>(
  override val collection: List<E>, removed: Collection<E>
): MultiRemoval<E, List<E>>(collection, removed), ListRemovalBase<E>


class RemoveElementsFromSet<E>(collection: Set<E>, removed: Collection<E>): MultiRemovalFromSet<E>(
  collection, removed
) {

  override val removedElements get() = removed.toList()

  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RemoveElementsFromSet(collection as Set<T>, removed.map(convert))

  override fun toString() = toStringBuilder(
	mapOf(
	  "removed" to removed.elementsToString()
	)
  )

}


class RemoveElements<E>(collection: List<E>, removed: Collection<E>, override val lowestChangedIndex: Int):
	MultiRemovalFromList<E>(collection, removed) {

  override val removedElementsIndexed get() = removed.map { it withIndex All }.toOrderedSet()

  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RemoveElements(collection as List<T>, removed.map(convert), lowestChangedIndex = lowestChangedIndex)

  override fun toString() = toStringBuilder(
	mapOf(
	  "removed" to removed.elementsToString()
	)
  )

}


class RemoveAtIndices<E>(collection: List<E>, removed: List<IndexedValue<E>>): MultiRemovalFromList<E>(collection,
																									   removed.map { it.value }) {
  private val removedWithIndices = removed
  override val removedElementsIndexed
	get() = removedWithIndices.map {
	  MyIndexedValue(
		element = it.value, index = Index(it.index)
	  )
	}.toOrderedSet()

  override fun <T> convert(collection: Collection<T>, convert: (E)->T): RemoveAtIndices<T> {
	return RemoveAtIndices(collection as List<T>, removedWithIndices.map { IndexedValue(it.index, convert(it.value)) })
  }

  override val lowestChangedIndex: Int = removed.minOfOrNull { it.index }!!

  val isRange by lazy {
	removedWithIndices.zipWithNext { a, b -> a.index == b.index - 1 }.all { it }
  }
}


class RetainAllSet<E>(collection: Set<E>, removed: Collection<E>, val retained: Collection<E>): MultiRemovalFromSet<E>(
  collection,
  removed
), SetChange<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RetainAllSet(collection as Set<T>, removed.map(convert), retained = retained.map(convert))

  override val removedElements = removed.toList()
}


class RetainAllList<E>(
  collection: List<E>, removed: Collection<E>, val retained: Collection<E>, override val lowestChangedIndex: Int
): MultiRemovalFromList<E>(collection, removed), ListRemovalBase<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = RetainAllList(
	collection as List<T>,
	removed.map(convert),
	retained = retained.map(convert),
	lowestChangedIndex = lowestChangedIndex
  )

  override val removedElementsIndexed: OrderedSet<MyIndexedValue<E, out RemovalIndex>>
	get() = removed.map { it withIndex All }.toOrderedSet()


}


sealed class Replacement<E>(override val collection: List<E>, val removed: E, val added: E): ListAdditionBase<E>,
																							 ListRemovalBase<E> { //  override val addedElements get() = listOf(added)
  //  override val removedElements get() = listOf(removed)


}

class ReplaceAt<E>(collection: List<E>, removed: E, added: E, val index: Int): Replacement<E>(
  collection, removed, added
) {

  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	ReplaceAt(collection as List<T>, convert(removed), added = convert(added), index = index)

  override val removedElementsIndexed get() = orderedSetOf(removed.withIndex(index))
  override val addedElementsIndexed get() = orderedSetOf(added withIndex index)
  override val lowestChangedIndex: Int
	get() = index

  override fun toString() = toStringBuilder(
	mapOf(
	  "removed" to removed, "added" to added, "index" to index
	)
  )

}

class ClearSet<E>(collection: Set<E>, removed: Collection<E>): MultiRemovalFromSet<E>(collection, removed),
															   SetChange<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	ClearSet(collection as Set<T>, removed = removed.map(convert))

  override val removedElements = removed.toList()
}

class ClearList<E>(collection: List<E>, removed: Collection<E>): MultiRemovalFromList<E>(collection, removed),
																 ListRemovalBase<E> {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	ClearList(collection as List<T>, removed = removed.map(convert))

  override val removedElementsIndexed: OrderedSet<MyIndexedValue<E, out RemovalIndex>>
	get() = removed.map { it withIndex All }.toOrderedSet()

  override val lowestChangedIndex: Int get() = 0
}


fun <E> MutableSet<E>.mirror(c: SetChange<E>): SetChange<E> {
  try {
	when (c) {
	  is ClearSet              -> clear()
	  is RetainAllSet          -> retainAll(c.retained)
	  is RemoveElementFromSet  -> remove(c.removed)
	  is AddIntoSet            -> add(c.added)
	  is MultiAddIntoSet       -> addAll(c.added)
	  is RemoveElementsFromSet -> removeAll(c.removed)
	}
  } catch (e: IllegalArgumentException) {    /*Throwable:  Throwable=java.lang.IllegalArgumentException: Children: duplicate children added: parent = TextFlow@36365ac6*/
	println("c=$c")
	taball("mirroring set", this)
	throw e
  }

  return c
}

fun <S, T> MutableSet<T>.mirror(c: SetChange<S>, convert: (S)->T) {
  mirror(c.convert(this, convert))
}


fun <E> MutableList<E>.mirror(c: ListChange<E>, debug: Boolean = false): ListChange<E> {
  if (debug) {
	println("mirror: $c")
	taball("before mirror", this)
  }
  try {
	when (c) {
	  is AddAt                 -> add(index = c.index, element = c.added)
	  is AddAtEnd              -> add(c.added)
	  is MultiAddAt            -> addAll(index = c.index, elements = c.added)
	  is MultiAddAtEnd         -> addAll(c.added)
	  is ReplaceAt             -> set(index = c.index, c.added)
	  is ClearList             -> clear()
	  is RemoveElements        -> removeAll(c.removed)
	  is RetainAllList         -> retainAll(c.retained)
	  is RemoveAt              -> removeAt(c.index)
	  is RemoveElementFromList -> remove(c.removed)
	  is RemoveAtIndices       -> {
		require(c.isRange)
		subList(
		  c.lowestChangedIndex,
		  c.removedElementsIndexed.last().index.i + 1
		).clear()        //		c.removedElementsIndexed.sortedBy { it.index }.forEach {
		//		  removeAt(it.index.i)
		//		}
	  }

	  is AtomicListChange      -> {

		c.compile().changes.forEach {
		  mirror(it)
		}
	  }
	}
  } catch (e: IllegalArgumentException) {    /*Throwable:  Throwable=java.lang.IllegalArgumentException: Children: duplicate children added: parent = TextFlow@36365ac6*/
	println("c=$c")
	taball("mirroring list", this)
	throw e
  }

  return c
}

fun <S, T> MutableList<T>.mirror(c: ListChange<S>, convert: (S)->T) {
  mirror(c.convert(this, convert))
}


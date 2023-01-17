package matt.obs.col.change.atomic

import matt.model.data.index.toKotlinIndexedValue
import matt.model.data.index.withIndex
import matt.obs.col.change.AddAt
import matt.obs.col.change.AtomicListChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveAtIndices
import matt.obs.col.change.ReplaceAt

fun <E> AtomicListChange<E>.compile(): AtomicListChange<E> {

  if (isCompiled) return this

  /*taball("input changes", changes)*/

  val compiledChanges = mutableListOf<ListChange<E>>()


  val itr = changes.iterator()

  var lastChange: ListChange<E>? = null

  while (itr.hasNext()) {

	val c = itr.next()



	if (lastChange is RemoveAt) {
	  if (c is RemoveAt) {
		if (lastChange.index == c.index) {
		  val i = c.index
		  compiledChanges.removeLast()
		  val newRemoval = RemoveAtIndices(
			collection,
			listOf(
			  lastChange.removed.withIndex(i),
			  c.removed.withIndex(i + 1)
			).map { it.toKotlinIndexedValue() }
		  )
		  compiledChanges += newRemoval
		  lastChange = newRemoval
		  continue
		}
	  }
	} else if (lastChange is RemoveAtIndices && lastChange.isRange) {
	  if (c is RemoveAt) {
		if (lastChange.removedElementsIndexed.first().index.i == c.index) {
		  compiledChanges.removeLast()
		  val newRemoval = RemoveAtIndices(
			collection,
			lastChange.removedElementsIndexed.toList().map { it.toKotlinIndexedValue() } + listOf(
			  c.removed.withIndex(
				lastChange.removedElementsIndexed.last().index.i + 1
			  ).toKotlinIndexedValue()
			)
		  )
		  compiledChanges += newRemoval
		  lastChange = newRemoval
		  continue
		}
	  }
	} else if (lastChange is ReplaceAt) {
	  val cSingleIndex = when (c) {
		is RemoveAt        -> c.lowestChangedIndex
		is RemoveAtIndices -> c.lowestChangedIndex
		else               -> null
	  }
	  val cSingleRemoved = when (c) {
		is RemoveAt        -> c.removed
		is RemoveAtIndices -> c.removedElements.singleOrNull()
		else               -> null
	  }
	  if (cSingleIndex != null && cSingleRemoved != null) {
		if (lastChange.index == cSingleIndex - 1) {
		  if (lastChange.added == cSingleRemoved) {
			compiledChanges.removeLast()
			val newRemoval = RemoveAt(
			  collection,
			  lastChange.removed,
			  lastChange.index
			)
			compiledChanges += newRemoval
			lastChange = newRemoval
			continue
		  }
		}
	  }
	} else if (lastChange is AddAt) {
	  if (c is AddAt) {
		if (lastChange.index == c.index - 1) {
		  compiledChanges.removeLast()
		  val newAdd = MultiAddAt(
			collection = collection,
			added = listOf(lastChange.added, c.added),
			index = lastChange.index
		  )
		  compiledChanges += newAdd
		  lastChange = newAdd
		  continue
		}
	  }
	} else if (lastChange is MultiAddAt && lastChange.isRange) {
	  if (c is AddAt) {
		if (lastChange.addedElementsIndexed.last().index.i == c.index - 1) {
		  compiledChanges.removeLast()
		  val newAdd = MultiAddAt(
			collection = collection,
			added = lastChange.added + listOf(c.added),
			index = lastChange.index
		  )
		  compiledChanges += newAdd
		  lastChange = newAdd
		  continue
		}
	  }
	}

	lastChange = c
	compiledChanges += c

  }

  /*taball("output changes", compiledChanges)*/


  return AtomicListChange(
	collection = collection,
	changes = compiledChanges,
	isCompiled = true
  )


}
package matt.obs.col.change.atomic

import matt.obs.col.change.AtomicListChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveAtIndices
import matt.obs.col.change.ReplaceAt

fun <E> AtomicListChange<E>.compile(): AtomicListChange<E> {

  if (isCompiled) return this

  val compiledChanges = mutableListOf<ListChange<E>>()


  val itr = changes.iterator()

  var lastChange: ListChange<E>? = null

  while (itr.hasNext()) {

	val c = itr.next()

	if (lastChange is ReplaceAt) {
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
			compiledChanges += RemoveAt(
			  collection,
			  lastChange.removed,
			  lastChange.index
			)
			lastChange = c
			continue
		  }
		}
	  }
	}

	lastChange = c
	compiledChanges += c

  }


  return AtomicListChange(
	collection = collection,
	changes = compiledChanges,
	isCompiled = true
  )


}
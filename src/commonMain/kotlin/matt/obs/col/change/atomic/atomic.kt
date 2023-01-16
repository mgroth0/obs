package matt.obs.col.change.atomic

import matt.obs.col.change.AtomicListChange

fun <E> AtomicListChange<E>.compile(): AtomicListChange<E> {






  return AtomicListChange(
	collection = collection,
	changes = changes
  )





}
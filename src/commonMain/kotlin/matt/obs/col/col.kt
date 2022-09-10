package matt.obs.col

import matt.obs.MObservableWithChangeObjectImpl
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.prop.VarProp

fun <E> Collection<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(this)
}

fun <E> Iterable<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(this.toList())
}

fun <E> Sequence<E>.toBasicObservableList(): BasicObservableListImpl<E> {
  return BasicObservableListImpl(this.toList())
}


sealed interface CollectionChange<E> {
  val collection: Collection<E>
}


sealed interface AdditionBase<E>: CollectionChange<E> {
  val addedElements: List<E>
}

sealed interface RemovalBase<E>: CollectionChange<E> {
  val removedElements: List<E>
}

sealed class Addition<E>(override val collection: Collection<E>, val added: E): AdditionBase<E> {
  override val addedElements get() = listOf(added)
}

class AddAtEnd<E>(collection: Collection<E>, added: E): Addition<E>(collection, added)
class AddAt<E>(collection: Collection<E>, added: E, val index: Int): Addition<E>(collection, added)
sealed class MultiAddition<E>(override val collection: Collection<E>, val added: Collection<E>): AdditionBase<E> {
  override val addedElements get() = added.toList()
}

class MultiAddAtEnd<E>(collection: Collection<E>, added: Collection<E>): MultiAddition<E>(collection, added)
class MultiAddAt<E>(collection: Collection<E>, added: Collection<E>, val index: Int):
  MultiAddition<E>(collection, added)


sealed class Removal<E>(override val collection: Collection<E>, val removed: E): RemovalBase<E> {
  override val removedElements get() = listOf(removed)
}

class RemoveElement<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed)
class RemoveAt<E>(collection: Collection<E>, removed: E, val index: Int): Removal<E>(collection, removed)
class RemoveFirst<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed)

sealed class MultiRemoval<E>(override val collection: Collection<E>, val removed: Collection<E>): RemovalBase<E> {
  override val removedElements get() = removed.toList()
}

class RemoveElements<E>(collection: Collection<E>, removed: Collection<E>): MultiRemoval<E>(collection, removed)
class RetainAll<E>(collection: Collection<E>, removed: Collection<E>, val retained: Collection<E>):
  MultiRemoval<E>(collection, removed)


sealed class Replacement<E>(override val collection: Collection<E>, val removed: E, val added: E): AdditionBase<E>,
																								   RemovalBase<E> {
  override val addedElements get() = listOf(added)
  override val removedElements get() = listOf(removed)
}

class ReplaceElement<E>(collection: Collection<E>, removed: E, added: E): Replacement<E>(collection, removed, added)

class ReplaceAt<E>(collection: Collection<E>, removed: E, added: E, val index: Int):
  Replacement<E>(collection, removed, added)

class Clear<E>(collection: Collection<E>, removed: Collection<E>): MultiRemoval<E>(collection, removed)


abstract class BasicROObservableCollection<E>: MObservableWithChangeObjectImpl<CollectionChange<E>>(), Collection<E> {
  val isEmptyProp by lazy {
	VarProp(isEmpty()).apply {
	  onChange {
		value = this@BasicROObservableCollection.isEmpty()
	  }
	}
  }
}
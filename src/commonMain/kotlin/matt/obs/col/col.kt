package matt.obs.col

import matt.obs.MObservableWithChangeObjectImpl
import matt.obs.col.olist.BasicObservableListImpl

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

sealed class Addition<E>(override val collection: Collection<E>, val added: E): CollectionChange<E>
class AddAtEnd<E>(collection: Collection<E>, added: E): Addition<E>(collection, added)

class AddAt<E>(collection: Collection<E>, added: E, val index: Int): Addition<E>(collection, added)

sealed class MultiAddition<E>(override val collection: Collection<E>, val added: Collection<E>): CollectionChange<E>
class MultiAddAtEnd<E>(collection: Collection<E>, added: Collection<E>): MultiAddition<E>(collection, added)

class MultiAddAt<E>(collection: Collection<E>, added: Collection<E>, val index: Int):
  MultiAddition<E>(collection, added)

sealed class Removal<E>(override val collection: Collection<E>, val removed: E): CollectionChange<E>
class RemoveElement<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed)
class RemoveAt<E>(collection: Collection<E>, removed: E, val index: Int): Removal<E>(collection, removed)
class RemoveFirst<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed)

sealed class MultiRemoval<E>(override val collection: Collection<E>, val removed: Collection<E>): CollectionChange<E>
class RemoveElements<E>(collection: Collection<E>, removed: Collection<E>): MultiRemoval<E>(collection, removed)

class RetainAll<E>(collection: Collection<E>, removed: Collection<E>, val retained: Collection<E>):
  MultiRemoval<E>(collection, removed)

sealed class Replacement<E>(override val collection: Collection<E>, val removed: E, val added: E): CollectionChange<E>
class ReplaceElement<E>(collection: Collection<E>, removed: E, added: E): Replacement<E>(collection, removed, added)

class ReplaceAt<E>(collection: Collection<E>, removed: E, added: E, val index: Int):
  Replacement<E>(collection, removed, added)

class Clear<E>(override val collection: Collection<E>): CollectionChange<E>


abstract class BasicObservableCollection<E>: MObservableWithChangeObjectImpl<CollectionChange<E>>(), Collection<E>
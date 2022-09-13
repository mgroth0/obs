package matt.obs.map.change

sealed interface MapChange<K, V> {
  val map: Map<K, V>
  //  fun <T> convert(collection: Collection<T>, convert: (E)->T): CollectionChange<T>
}

class Put<K, V>(override val map: Map<K, V>, val key: K, val value: V): MapChange<K, V>
class PutAll<K, V>(override val map: Map<K, V>, val from: Map<out K, V>): MapChange<K, V>
class Remove<K, V>(override val map: Map<K, V>, val key: K): MapChange<K, V>
class ItrRemove<K, V>(override val map: Map<K, V>): MapChange<K, V>
class RemoveValue<K, V>(override val map: Map<K, V>): MapChange<K, V>
class Clear<K, V>(override val map: Map<K, V>): MapChange<K, V>

/*

sealed interface AdditionBase<E>: MapChange<E> {
  val addedElements: List<E>
}

sealed interface RemovalBase<E>: MapChange<E> {
  val removedElements: List<E>
}

sealed class Addition<E>(override val collection: Collection<E>, val added: E): AdditionBase<E> {
  override val addedElements get() = listOf(added)
}

class AddAtEnd<E>(collection: Collection<E>, added: E): Addition<E>(collection, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = AddAtEnd(collection, convert(added))
}

class AddAt<E>(collection: Collection<E>, added: E, val index: Int): Addition<E>(collection, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	AddAt(collection, convert(added), index = index)
}

sealed class MultiAddition<E>(override val collection: Collection<E>, val added: Collection<E>): AdditionBase<E> {
  override val addedElements get() = added.toList()
}

class MultiAddAtEnd<E>(collection: Collection<E>, added: Collection<E>): MultiAddition<E>(collection, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = MultiAddAtEnd(collection, added.map(convert))
}

class MultiAddAt<E>(collection: Collection<E>, added: Collection<E>, val index: Int):
  MultiAddition<E>(collection, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	MultiAddAt(collection, added.map(convert), index)
}


sealed class Removal<E>(override val collection: Collection<E>, val removed: E): RemovalBase<E> {
  override val removedElements get() = listOf(removed)
}

class RemoveElement<E>(collection: Collection<E>, removed: E): Removal<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = RemoveElement(collection, convert(removed))
}

class RemoveAt<E>(collection: Collection<E>, removed: E, val index: Int): Removal<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = RemoveAt(collection, convert(removed), index)
}
//class RemoveFirst<E>(collection: Collection<E>, removed: E): matt.obs.map.change.Removal<E>(collection, removed)

sealed class MultiRemoval<E>(override val collection: Collection<E>, val removed: Collection<E>): RemovalBase<E> {
  override val removedElements get() = removed.toList()
}

class RemoveElements<E>(collection: Collection<E>, removed: Collection<E>): MultiRemoval<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) = MultiAddAtEnd(collection, removed.map(convert))
}

class RetainAll<E>(collection: Collection<E>, removed: Collection<E>, val retained: Collection<E>):
  MultiRemoval<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	RetainAll(collection, removed.map(convert), retained = retained.map(convert))
}


sealed class Replacement<E>(override val collection: Collection<E>, val removed: E, val added: E): AdditionBase<E>,
																								   RemovalBase<E> {
  override val addedElements get() = listOf(added)
  override val removedElements get() = listOf(removed)
}

class ReplaceAt<E>(collection: Collection<E>, removed: E, added: E, val index: Int):
  Replacement<E>(collection, removed, added) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	ReplaceAt(collection, convert(removed), added = convert(added), index = index)
}

class Clear<E>(collection: Collection<E>, removed: Collection<E>): MultiRemoval<E>(collection, removed) {
  override fun <T> convert(collection: Collection<T>, convert: (E)->T) =
	Clear(collection, removed = removed.map(convert))
}


fun <E> MutableList<E>.mirror(c: CollectionChange<E>): CollectionChange<E> {
  when (c) {
	is AddAt          -> add(index = c.index, element = c.added)
	is AddAtEnd       -> add(c.added)
	is MultiAddAt     -> addAll(index = c.index, elements = c.added)
	is MultiAddAtEnd  -> addAll(c.added)
	is ReplaceAt      -> set(index = c.index, c.added)
	is Clear          -> clear()
	is RemoveElements -> removeAll(c.removed)
	is RetainAll      -> retainAll(c.retained)
	is RemoveAt       -> removeAt(c.index)
	is RemoveElement  -> remove(c.removed)
  }
  return c
}

fun <S, T> MutableList<T>.mirror(c: CollectionChange<S>, convert: (S)->T) = mirror(c.convert(this, convert))
*/

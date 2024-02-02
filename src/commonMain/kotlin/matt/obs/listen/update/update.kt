package matt.obs.listen.update

import matt.lang.idea.EventIdea
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.NonNullCollectionChange
import matt.obs.col.change.QueueChange
import matt.obs.col.change.SetChange
import matt.obs.map.change.MapChange

sealed interface Update
interface Event : Update, EventIdea
object Beep : Event
class PagerMessage<M>(val message: M) : Event
sealed interface ValueUpdate<T> : Update {
    val new: T
}

open class NewValueUpdate<T>(final override val new: T) : ValueUpdate<T>

interface WeakUpdate<W : Any> : Update {
    val weakObj: W
}

class ValueUpdateWithWeakObj<W : Any, T>(
    override val new: T,
    override val weakObj: W
) : ValueUpdate<T>, WeakUpdate<W>


/*inspired by invalidation listeners!*/
/*sometimes listeners don't always need to calculate stuff, so that stuff should not be calculated eagerly*/
open class LazyNewValueUpdate<T>(private val newOp: () -> T) : ValueUpdate<T> {
    final override val new by lazy { newOp() }
}

open class LazyMaybeNewValueUpdate<T>(private val newOp: () -> T) : ValueUpdate<T> {
    final override val new by lazy { newOp() }
}


open class ValueChange<T>(
    internal val old: T,
    new: T
) : NewValueUpdate<T>(new)

class ValueUpdateWithWeakObjAndOld<W : Any, T>(
    new: T,
    old: T,
    override val weakObj: W
) :
    ValueChange<T>(
            old = old, new = new
        ), WeakUpdate<W>

abstract class NonNullCollectionUpdate<E, C : NonNullCollectionChange<E, out Collection<E>>>(internal open val change: C) : Update
abstract class CollectionUpdate<E, C : CollectionChange<E, out Collection<E>>>(internal open val change: C) : Update
class SetUpdate<E>(override val change: SetChange<E>) : CollectionUpdate<E, SetChange<E>>(change)
class ListUpdate<E>(override val change: ListChange<E>) : CollectionUpdate<E, ListChange<E>>(change)
class MapUpdate<K, V>(internal val change: MapChange<K, V>) : Update

object ContextUpdate : Update
object ObsHolderUpdate : Update


class QueueUpdate<E : Any>(override val change: QueueChange<E>) : NonNullCollectionUpdate<E, QueueChange<E>>(change)

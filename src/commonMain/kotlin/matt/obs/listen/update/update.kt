package matt.obs.listen.update

import matt.obs.col.change.CollectionChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.SetChange
import matt.obs.map.change.MapChange

sealed interface Update
interface Event: Update
object Beep: Event
class PagerMessage<M>(val message: M): Event
sealed interface ValueUpdate<T>: Update {
  val new: T
}

open class NewValueUpdate<T>(override val new: T): ValueUpdate<T>

interface WeakUpdate<W: Any>: Update {
  val weakObj: W
}

class ValueUpdateWithWeakObj<W: Any, T>(override val new: T, override val weakObj: W): ValueUpdate<T>, WeakUpdate<W>


/*inspired by invalidation listeners!*/
/*sometimes listeners don't always need to calculate stuff, so that stuff should not be calculated eagerly*/
open class LazyNewValueUpdate<T>(private val newOp: ()->T): ValueUpdate<T> {
  override val new by lazy { newOp() }
}


open class ValueChange<T>(internal val old: T, new: T): NewValueUpdate<T>(new)
class ValueUpdateWithWeakObjAndOld<W: Any, T>(new: T, old: T, override val weakObj: W):
	ValueChange<T>(
	  old = old, new = new
	), WeakUpdate<W>

sealed class CollectionUpdate<E>(internal open val change: CollectionChange<E, *>): Update
class SetUpdate<E>(override val change: SetChange<E>): CollectionUpdate<E>(change)
class ListUpdate<E>(override val change: ListChange<E>): CollectionUpdate<E>(change)
class MapUpdate<K, V>(internal val change: MapChange<K, V>): Update

object ContextUpdate: Update
object ObsHolderUpdate: Update
package matt.obs.listen.update

import matt.obs.col.change.CollectionChange
import matt.obs.map.change.MapChange

sealed interface Update
sealed interface ValueUpdate<T>: Update {
  val new: T
}

open class NewValueUpdate<T>(override val new: T): ValueUpdate<T>

/*inspired by invalidation listeners!*/
/*sometimes listeners don't always need to calculate stuff, so that stuff should not be calculated eagerly*/
open class LazyNewValueUpdate<T>(private val newOp: ()->T): ValueUpdate<T> {
  override val new by lazy { newOp() }
}
class ValueChange<T>(internal val old: T, new: T): NewValueUpdate<T>(new)
class CollectionUpdate<E>(internal val change: CollectionChange<E>): Update
class MapUpdate<K,V>(internal val change: MapChange<K,V>): Update

object ContextUpdate: Update
object ObsHolderUpdate: Update
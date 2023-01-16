package matt.obs.listen.update

import matt.obs.col.change.QueueChange

class QueueUpdate<E>(override val change: QueueChange<E>): CollectionUpdate<E, QueueChange<E>>(change)
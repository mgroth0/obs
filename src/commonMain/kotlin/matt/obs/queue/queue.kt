package matt.obs.queue

import matt.collect.queue.MyMutableQueue
import matt.lang.assertions.require.requireNotIs
import matt.obs.col.InternallyBackedOQueue
import matt.obs.col.change.QueueAdd
import matt.obs.col.change.QueueRemove


fun <E : Any> MyMutableQueue<E>.wrapInObservableQueue(): ObservableQueue<E> {
    requireNotIs<ObservableQueue<*>>(this)
    return ObservableQueue(this)
}

class ObservableQueue<E : Any> internal constructor(private val q: MyMutableQueue<E>) :
    InternallyBackedOQueue<E>(),
    MyMutableQueue<E>,
    Collection<E> by q {

        override fun add(element: E): Boolean {
            val b = q.add(element)
            emitChange(QueueAdd(added = element, collection = this))
            return b
        }

        override fun addAll(elements: Collection<E>): Boolean {
            TODO()
        }

        override fun clear() {
            TODO()
        }


        override fun poll(): E? {
            val e = q.poll()
            if (e != null) emitChange(QueueRemove(removed = e, collection = this))
            return e
        }

        override fun element(): E {
            TODO()
        }

        override fun peek(): E {
            TODO()
        }

        override fun offer(e: E): Boolean {
            TODO()
        }

        override fun retainAll(elements: Collection<E>): Boolean {
            TODO()
        }

        override fun removeAll(elements: Collection<E>): Boolean {
            TODO()
        }

        override fun remove(element: E): Boolean {
            TODO()
        }

        override fun iterator(): MutableIterator<E> {
            TODO()
        }
    }

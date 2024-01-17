package matt.obs.col.olist.view

import matt.lang.function.Op
import matt.model.op.prints.Prints
import matt.obs.col.change.AddAt
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.AtomicListChange
import matt.obs.col.change.ClearList
import matt.obs.col.change.ListChange
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveAtIndices
import matt.obs.col.change.RemoveElementFromList
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.ReplaceAt
import matt.obs.col.change.RetainAllList
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.dynamic.InterestingList
import matt.obs.listen.ListListenerBase
import matt.obs.listen.MyListenerInter
import matt.obs.listen.update.ListUpdate

/*like a DynamicList, but more sophisticated and performant*/
sealed class ViewOfObsList<E>(
    private val source: ImmutableObsList<E>,
) : ImmutableObsList<E>, InterestingList


class NonNullsOfObsList<E>(val source: ImmutableObsList<E?>) : ImmutableObsList<E & Any> {

    private fun indexMap(toSourceExclusive: Int): Map<Int, Int> {
        var n = 0
        return source.asSequence().withIndex().filter { it.value != null }.take(toSourceExclusive).associate {
            it.index to n++
        }
    }


    override fun onChange(listenerName: String?, op: (ListChange<E & Any>) -> Unit): MyListenerInter<*> {
        TODO()
    }

    override val size get() = source.count { it != null }

    override fun isEmpty(): Boolean {
        TODO()
    }

    override fun iterator() = listIterator()

    override fun releaseUpdatesAfter(op: Op) {
        TODO()
    }


    @Suppress("UNREACHABLE_CODE")
    override fun addListener(listener: ListListenerBase<E & Any>): ListListenerBase<E & Any> {
        source.onChange { c ->
            when (c) {
                is AtomicListChange      -> TODO()
                is AddAt                 -> TODO()
                is AddAtEnd              -> TODO()
                is MultiAddAt            -> TODO()
                is MultiAddAtEnd         -> TODO()
                is ReplaceAt             -> TODO()
                is ClearList             -> TODO()
                is RemoveAt              -> TODO()
                is RemoveElementFromList -> TODO()
                is RemoveAtIndices       -> {
                    error("seems bugged :(")

                    require(c.isRange)
                    val lowIndex = c.lowestChangedIndex
                    val nullCount = source.take(lowIndex).count { it == null }
                    var nextI = lowIndex - nullCount
                    val newRemoved = c.removedElementsIndexed.sortedBy { it.index }.mapNotNull {
                        it.element?.let { e ->
                            IndexedValue(index = nextI++, value = e)
                        }

                    }
                    /*  var nCount = 0
                      val sourceItr = source.listIterator()
                      var currentI = 0
                      val newRemoved = c.removedElementsIndexed.sortedBy { it.index }.mapNotNull {
                          val changedIndex = it.index.i
                          if (it.element == null) {
                              nCount++
                              null
                          } else {
                              while (sourceItr.hasNext() && currentI < changedIndex) {
                                  val n = sourceItr.next()
                                  if (n == null) nCount++
                                  currentI++
                              }
                              IndexedValue(index = changedIndex - nCount, value = it.element!!)
                          }
                      }*/
                    listener.notify(
                        ListUpdate(
                            RemoveAtIndices(
                                collection = this,
                                removed = newRemoved
                            )
                        )
                    )
                }

                is RemoveElements        -> TODO()
                is RetainAllList         -> TODO()
            }
        }
        return listener
    }

    @Suppress("UNUSED_PARAMETER")
    override var nam: String?
        get() = TODO()
        set(value) {
            TODO()
        }

    override fun removeListener(listener: MyListenerInter<*>) {
        TODO()
    }

    @Suppress("UNUSED_PARAMETER")
    override var debugger: Prints?
        get() = TODO()
        set(value) {
            TODO()
        }

    override fun get(index: Int): E & Any {
        TODO()
    }

    override fun listIterator() = listIterator(0)

    override fun listIterator(index: Int): ListIterator<E & Any> = object : ListIterator<E & Any> {

        private val itr = source.listIterator(index)

        override fun hasNext(): Boolean {
            while (itr.hasNext()) {
                val n = itr.next()
                if (n != null) {
                    itr.previous()
                    return true
                }
            }
            return false
        }

        override fun hasPrevious(): Boolean {
            while (itr.hasPrevious()) {
                val n = itr.previous()
                if (n != null) {
                    itr.next()
                    return true
                }
            }
            return false
        }

        override fun next(): E & Any {
            do {
                val n = itr.next()
                if (n != null) return n
            } while (true)
        }

        override fun nextIndex(): Int {
            TODO()
        }

        override fun previous(): E & Any {
            TODO()
        }

        override fun previousIndex(): Int {
            TODO()
        }

    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E & Any> {
        TODO()
    }

    override fun lastIndexOf(element: E & Any): Int {
        TODO()
    }

    override fun indexOf(element: E & Any): Int {
        TODO()
    }

    override fun containsAll(elements: Collection<E & Any>): Boolean {
        TODO()
    }

    override fun contains(element: E & Any): Boolean {
        TODO()
    }

}
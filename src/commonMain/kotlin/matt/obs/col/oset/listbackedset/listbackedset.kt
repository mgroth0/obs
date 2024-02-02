package matt.obs.col.oset.listbackedset

import matt.lang.function.Op
import matt.model.op.prints.Prints
import matt.obs.col.change.AddAt
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.AddIntoSet
import matt.obs.col.change.AtomicListChange
import matt.obs.col.change.ClearList
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveAtIndices
import matt.obs.col.change.RemoveElementFromList
import matt.obs.col.change.RemoveElementFromSet
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.ReplaceAt
import matt.obs.col.change.RetainAllList
import matt.obs.col.change.SetChange
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.oset.ObsSet
import matt.obs.listen.MyListenerInter
import matt.obs.listen.SetListenerBase


class ListBackedObservableSet<E>(
    private val list: ImmutableObsList<E>
) : ObsSet<E> {


    override val size: Int
        get() = TODO()

    override fun isEmpty(): Boolean {
        TODO()
    }

    override fun iterator(): Iterator<E> {
        TODO()
    }

    override fun releaseUpdatesAfter(op: Op) {
        TODO()
    }

    override fun addListener(listener: SetListenerBase<E>): SetListenerBase<E> {
        TODO()
    }

    override var nam: String?
        get() = TODO()
        set(value) {}

    override fun removeListener(listener: MyListenerInter<*>) {
        TODO()
    }

    override var debugger: Prints?
        get() = TODO()
        set(value) {}

    override fun onChange(
        listenerName: String?,
        op: (SetChange<E>) -> Unit
    ): MyListenerInter<*> = list.onChange(listenerName) {
        val setChange: SetChange<E>? = when (it) {
            is AtomicListChange      -> TODO()
            is AddAt                 -> TODO()
            is AddAtEnd              -> {
                val e = it.addedElements.single()
                if (e !in list.subList(0, list.indices.last)) {
                    AddIntoSet(this, e)
                } else null
            }

            is MultiAddAt            -> TODO()
            is MultiAddAtEnd         -> TODO()
            is ReplaceAt             -> TODO()
            is ClearList             -> TODO()
            is RemoveAt              -> {
                val e = it.removedElements.single()
                if (e !in list) {
                    RemoveElementFromSet(this, e)
                } else null
            }

            is RemoveElementFromList -> TODO()
            is RemoveAtIndices       -> TODO()
            is RemoveElements        -> TODO()
            is RetainAllList         -> TODO()
        }
        if (setChange != null) {
            op(setChange)
        }

    }

    override fun containsAll(elements: Collection<E>): Boolean {
        TODO()
    }

    override fun contains(element: E): Boolean {
        TODO()
    }


}

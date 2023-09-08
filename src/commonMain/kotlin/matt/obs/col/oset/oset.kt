package matt.obs.col.oset

import matt.collect.itr.IteratorExtender
import matt.collect.itr.MutableIteratorExtender
import matt.lang.weak.MyWeakRef
import matt.obs.bind.binding
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOSet
import matt.obs.col.change.AddIntoSet
import matt.obs.col.change.ClearSet
import matt.obs.col.change.MultiAddIntoSet
import matt.obs.col.change.RemoveElementFromSet
import matt.obs.col.change.RemoveElementsFromSet
import matt.obs.col.change.RetainAllSet
import matt.obs.col.change.SetChange
import matt.obs.fx.requireNotObservable
import matt.obs.listen.SetListenerBase
import matt.obs.listen.WeakSetListener
import matt.obs.listen.update.SetUpdate

interface ObsSet<E> : Set<E>, BasicOCollection<E, SetChange<E>, SetUpdate<E>, SetListenerBase<E>> {

    override fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, SetChange<E>) -> Unit
    ) = run {
        val weakRef = MyWeakRef(o)
        onChangeWithAlreadyWeak(weakRef) { w, c ->
            op(w, c)
        }
    }

    override fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: MyWeakRef<W>,
        op: (W, SetChange<E>) -> Unit
    ) = run {
        val listener = WeakSetListener(weakRef) { o: W, c: SetChange<E> ->
            op(o, c)
        }
        addListener(listener)
    }


}



fun <E> Collection<E>.toBasicImmutableObservableSet(): ObsSet<E> {
    return BasicImmutableObservableSet(this)
}

fun <E> Iterable<E>.toBasicImmutableObservableSet(): ObsSet<E> {
    return BasicImmutableObservableSet(this.toSet())
}

fun <E> Sequence<E>.toBasicImmutableObservableSet(): ObsSet<E> {
    return BasicImmutableObservableSet(this.toSet())
}

fun <E> basicImmutableObservableSetOf(vararg values: E) = BasicImmutableObservableSet<E>(values.toSet())

open class BasicImmutableObservableSet<E>(private val theSet: Set<E>) : InternallyBackedOSet<E>(),
    ObsSet<E> {


    constructor(c: Collection<E>) : this(c.requireNotObservable().toMutableSet())
    constructor() : this(emptySet())


    final override val size: Int
        get() = theSet.size

    final override fun contains(element: E): Boolean {
        return theSet.contains(element)
    }

    final override fun containsAll(elements: Collection<E>): Boolean {
        return theSet.containsAll(elements)
    }

    final override fun isEmpty(): Boolean {
        return theSet.isEmpty()
    }

    override fun iterator(): Iterator<E> = object : IteratorExtender<E>(theSet) {
        var lastNext: E? = null
        override fun postNext(e: E) {
            lastNext = e
        }
    }

}


interface MutableObsSet<E> : ObsSet<E>, MutableSet<E>

fun <E> Collection<E>.toBasicObservableSet(): BasicObservableSet<E> {
    return BasicObservableSet(this)
}

fun <E> Iterable<E>.toBasicObservableSet(): BasicObservableSet<E> {
    return BasicObservableSet(this.toSet())
}

fun <E> Sequence<E>.toBasicObservableSet(): BasicObservableSet<E> {
    return BasicObservableSet(this.toSet())
}

fun <E> basicObservableSetOf(vararg values: E) = BasicObservableSet<E>(values.toMutableSet())

class BasicObservableSet<E>(private val theSet: MutableSet<E>) : BasicImmutableObservableSet<E>(theSet),
    MutableObsSet<E> {


    constructor(c: Collection<E>) : this(c.requireNotObservable().toMutableSet())
    constructor() : this(emptySet())


    override fun iterator() = object : MutableIteratorExtender<E>(theSet) {
        var lastNext: E? = null
        override fun postNext(e: E) {
            lastNext = e
        }

        override fun remove() {
            super.remove()
            emitChange(
                RemoveElementFromSet(theSet, lastNext ?: error("todo: this does not work with null elements yet"))
            )
        }
    }

    override fun add(element: E): Boolean {
        val b = theSet.add(element)
        if (b) {
            emitChange(AddIntoSet(theSet, element))
        }
        return b
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val b = theSet.addAll(elements)
        if (b) emitChange(MultiAddIntoSet(theSet, elements))
        return b
    }

    override fun clear() {
        val removed = theSet.toSet()
        theSet.clear()
        emitChange(ClearSet(theSet, removed = removed))
    }

    override fun remove(element: E): Boolean {
        val b = theSet.remove(element)
        if (b) emitChange(RemoveElementFromSet(theSet, element))
        return b
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        val b = theSet.removeAll(elements)
        if (b) emitChange(RemoveElementsFromSet(theSet, elements))
        return b
    }


    override fun retainAll(elements: Collection<E>): Boolean {
        val toRemove = theSet.filter { it !in elements }
        val b = theSet.retainAll(elements)
        if (b) emitChange(RetainAllSet(theSet, toRemove, retained = elements))
        return b
    }

}


val <E> ObsSet<E>.sizeProperty get() = binding { size }

val <E> ObsSet<E>.isEmptyProperty get() = binding { isEmpty() }
val <E> ObsSet<E>.isNotEmptyProperty get() = binding { isNotEmpty() }
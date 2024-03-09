package matt.obs.col.olist

import matt.collect.fake.FakeMutableList
import matt.collect.itr.FakeMutableIterator
import matt.collect.itr.FakeMutableListIterator
import matt.collect.itr.ItrDir
import matt.collect.itr.ItrDir.NEXT
import matt.collect.itr.ItrDir.PREVIOUS
import matt.collect.itr.MutableListIteratorExtender
import matt.lang.anno.NeedsTest
import matt.lang.anno.Open
import matt.lang.assertions.require.requireNonNegative
import matt.lang.assertions.require.requireNot
import matt.lang.common.ILLEGAL
import matt.lang.common.NEVER
import matt.lang.common.NOT_IMPLEMENTED
import matt.lang.compare.comparableComparator
import matt.lang.function.Consume
import matt.lang.function.Op
import matt.lang.ktversion.ifPastInitialK2
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.inSync
import matt.lang.sync.inSync
import matt.lang.weak.common.WeakRefInter
import matt.lang.weak.weak
import matt.model.op.prints.Prints
import matt.obs.bind.MyBinding
import matt.obs.bind.binding
import matt.obs.bindhelp.BindableList
import matt.obs.bindhelp.BindableListImpl
import matt.obs.bindings.bool.ObsB
import matt.obs.col.BasicOCollection
import matt.obs.col.InternallyBackedOList
import matt.obs.col.change.AddAt
import matt.obs.col.change.AddAtEnd
import matt.obs.col.change.AtomicListChange
import matt.obs.col.change.ClearList
import matt.obs.col.change.ListAdditionBase
import matt.obs.col.change.ListChange
import matt.obs.col.change.ListRemovalBase
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.MultiAddAtEnd
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveAtIndices
import matt.obs.col.change.RemoveElementFromList
import matt.obs.col.change.RemoveElements
import matt.obs.col.change.ReplaceAt
import matt.obs.col.change.RetainAllList
import matt.obs.col.olist.dynamic.BasicFilteredList
import matt.obs.col.olist.dynamic.BasicSortedList
import matt.obs.fx.requireNotFxObservable
import matt.obs.listen.ListListener
import matt.obs.listen.ListListenerBase
import matt.obs.listen.MyListenerInter
import matt.obs.listen.WeakListListener
import matt.obs.listen.update.ListUpdate
import matt.obs.prop.MObservableVal
import matt.obs.prop.ObsVal
import matt.obs.tempcommon.tempexpect.dynamicList


interface ImmutableObsList<E> :
    BasicOCollection<
        E,
        ListChange<E>,
        ListUpdate<E>,
        ListListenerBase<E>
    >,

    List<E>  {
    @Open
    override fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, ListChange<E>) -> Unit
    ) = run {
        val weakRef = weak(o)
        onChangeWithAlreadyWeak(weakRef) { w, c ->
            op(w, c)
        }
    }

    @Open
    override fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: WeakRefInter<W>,
        op: (W, ListChange<E>) -> Unit
    ) = run {
        val listener =
            WeakListListener(weakRef) { o: W, c: ListChange<E> ->
                op(o, c)
            }
        addListener(listener)
    }

    @Open fun filtered(filter: (E) -> Boolean): BasicFilteredList<E> = dynamicList(this, filter = filter)
    @Open fun dynamicallyFiltered(filter: (E) -> ObsB): BasicFilteredList<E> = dynamicList(this, dynamicFilter = filter)
    @Suppress("UNCHECKED_CAST")
    @Open fun sorted(
        comparator: Comparator<in E>? = null
    ): BasicSortedList<E> = dynamicList(this, comparator = comparator) as BasicSortedList<E>
    @Open fun onAdd(op: Consume<E>) = listen(onAdd = op, onRemove = {})
    @Open fun onRemove(op: Consume<E>) = listen(onAdd = { }, onRemove = op)

    @Open fun listen(
        onAdd: ((E) -> Unit),
        onRemove: ((E) -> Unit)
    ) {
        addListener(
            ListListener {
                (it as? ListAdditionBase)?.addedElements?.forEach { e ->
                    onAdd(e)
                }
                (it as? ListRemovalBase)?.removedElements?.forEach { e ->
                    onRemove(e)
                }
            }
        )
    }
}

interface MutableObsList<E> : ImmutableObsList<E>, BindableList<E>, MutableList<E> {
    fun atomicChange(op: MutableObsList<E>.() -> Unit)
}

@Suppress("UNCHECKED_CAST")
fun <E : Comparable<E>> MutableObsList<E>.sorted(): BasicSortedList<E> = dynamicList(this, comparator = comparableComparator()) as BasicSortedList<E>

fun <E> MutableObsList<E>.toFakeMutableObsList() = FakeMutableObsList(this)


class FakeMutableObsList<E>(private val o: MutableObsList<E>) : MutableObsList<E> by o {
    override fun add(element: E) = ILLEGAL

    override fun add(
        index: Int,
        element: E
    ) = ILLEGAL

    override fun addAll(
        index: Int,
        elements: Collection<E>
    ) = ILLEGAL

    override fun addAll(elements: Collection<E>) = ILLEGAL

    override fun clear() = ILLEGAL

    override fun remove(element: E) = ILLEGAL

    override fun removeAll(elements: Collection<E>) = ILLEGAL

    override fun removeAt(index: Int) = ILLEGAL

    override fun retainAll(elements: Collection<E>) = ILLEGAL

    override fun set(
        index: Int,
        element: E
    ) = ILLEGAL

    override fun iterator(): MutableIterator<E> = FakeMutableIterator(o.iterator())

    override fun listIterator(): MutableListIterator<E> = FakeMutableListIterator(o.listIterator())

    override fun listIterator(index: Int): MutableListIterator<E> = FakeMutableListIterator(o.listIterator(index))

    override fun subList(
        fromIndex: Int,
        toIndex: Int
    ): MutableList<E> = FakeMutableList(o.subList(fromIndex, toIndex))
}


abstract class BaseBasicWritableOList<E> :
    InternallyBackedOList<E>(),
    MutableObsList<E>,
    BindableList<E> {


    val bindableListHelper by lazy { BindableListImpl(this) }
    final override fun <S> bind(
        source: ImmutableObsList<S>,
        converter: (S) -> E
    ) = bindableListHelper.bind(source, converter)

    final override fun <S> bindWeakly(
        source: ImmutableObsList<S>,
        converter: (S) -> E
    ) =
        bindableListHelper.bindWeakly(source, converter)

    final override fun <T> bind(
        source: ObsVal<T>,
        converter: (T) -> List<E>
    ) = bindableListHelper.bind(source, converter)

    final override val bindManager get() = bindableListHelper.bindManager
    final override var theBind
        get() = bindManager.theBind
        set(value) {
            bindManager.theBind = value
        }

    final override fun unbind() = bindableListHelper.unbind()


    fun <R> lazyBinding(
        vararg dependencies: MObservableVal<*, *, *>,
        op: (Collection<E>) -> R
    ): MyBinding<R> {
        val prop = this
        return MyBinding { op(prop) }.apply {
            prop.onChange {
                markInvalid()
            }
            dependencies.forEach {
                it.onChange {
                    markInvalid()
                }
            }
        }
    }
}


fun <E> Array<E>.toBasicObservableList(): BasicObservableListImpl<E> = BasicObservableListImpl(toList())

fun <E> Collection<E>.toBasicObservableList(): BasicObservableListImpl<E> = BasicObservableListImpl(this)

fun <E> Iterable<E>.toBasicObservableList(): BasicObservableListImpl<E> = BasicObservableListImpl(toList())

fun <E> Sequence<E>.toBasicObservableList(): BasicObservableListImpl<E> = BasicObservableListImpl(toList())


fun <E> basicROObservableListOf(vararg elements: E): MutableObsList<E> =
    BasicObservableListImpl(elements.toList())

fun <E> basicMutableObservableListOf(vararg elements: E): MutableObsList<E> =
    BasicObservableListImpl(elements.toList())

fun <E> basicMutableObservableListOf(elements: List<E>): MutableObsList<E> =
    BasicObservableListImpl(elements.toList())


fun <E> ImmutableObsList<E>.toMutableObsList() = toBasicObservableList()


fun <E> MutableObsList<E>.readOnly() = ReadOnlyObsList(this)
class ReadOnlyObsList<E>(private val obsList: ImmutableObsList<E>) : ImmutableObsList<E> by obsList


open class BasicObservableListImpl<E> private constructor(private val list: MutableList<E>) :
    BaseBasicWritableOList<E>(), List<E> by list, ReferenceMonitor {


        init {
            ifPastInitialK2 {
                println("this is probably fixed by now")
            }
        }

    /*
    used to be delegated  List<E> by list, but not any more due to https://youtrack.jetbrains.com/issue/KT-57299/K2-VerifyError-due-to-overriding-final-method-size-on-a-subclass-of-Collection-and-Set
     * */

        final override val size: Int
            get() = list.size

        final override fun contains(element: E): Boolean = list.contains(element)

        final override fun containsAll(elements: Collection<E>): Boolean = list.containsAll(elements)

        final override fun indexOf(element: E): Int = list.indexOf(element)

        final override fun lastIndexOf(element: E): Int = list.lastIndexOf(element)

        final override fun isEmpty(): Boolean = list.isEmpty()

        final override fun get(index: Int): E = list[index]

        constructor(c: Iterable<E>) : this(c.requireNotFxObservable().toMutableList())

        constructor() : this(mutableListOf())

        final override fun iterator(): MutableIterator<E> = listIterator()

        final override fun add(element: E): Boolean {
            val b = list.add(element)
            require(b)
            if (b) {
                changedFromOuter(AddAtEnd(list, element))
            }
            return b
        }

        final override fun add(
            index: Int,
            element: E
        ) {
            list.add(index, element)
            changedFromOuter(AddAt(list, element, index))
        }

        final override fun addAll(
            index: Int,
            elements: Collection<E>
        ): Boolean {
            val b = list.addAll(index, elements)
            if (b) changedFromOuter(MultiAddAt(list, elements, index))
            return b
        }

        final override fun addAll(elements: Collection<E>): Boolean {
            val b = list.addAll(elements)
            if (b) changedFromOuter(MultiAddAtEnd(list, elements))
            return b
        }

        final override fun clear() {
            val removed = list.toList()
            list.clear()
            changedFromOuter(ClearList(list, removed = removed))
        }


        final override fun listIterator(): MutableListIterator<E> = lItr()
        final override fun listIterator(index: Int): MutableListIterator<E> = lItr(index)

        private fun lItr(index: Int? = null) =
            object : MutableListIteratorExtender<E>(list, index ?: 0) {

                var lastElement: E? = null
                var lastItrDir: ItrDir? = null

                override fun postNext(e: E) {
                    lastElement = e
                    lastItrDir = NEXT
                }

                override fun postPrevious(e: E) {
                    lastElement = e
                    lastItrDir = PREVIOUS
                }

                override fun remove() {
                    super.remove()
                    changedFromOuter(RemoveAt(list, lastElement ?: TODO("null"), previousIndex() + 1))
                }

                override fun add(element: E) {
                    super.add(element)
                    changedFromOuter(AddAt(list, element, previousIndex()))
                }

                @NeedsTest
                override fun set(element: E) {
                    super.set(element)
                    changedFromOuter(
                        ReplaceAt(
                            list, lastElement ?: TODO("null"), element,
                            index =
                                when (lastItrDir) {
                                    NEXT -> {
                                        previousIndex()
                                    }

                                    PREVIOUS -> {
                                        previousIndex() + 1
                                    }

                                    else -> NEVER
                                }
                        )
                    )
                }
            }


        final override fun remove(element: E): Boolean {
            val i = list.indexOf(element)
            val b = list.remove(element)
            if (b) changedFromOuter(RemoveElementFromList(list, element, i))
            return b
        }

        final override fun removeAll(elements: Collection<E>): Boolean {
            val lowestChangedIndex = withIndex().firstOrNull { it.value in elements }?.index
            val b = list.removeAll(elements)
            if (b) {
                changedFromOuter(RemoveElements(list, elements, lowestChangedIndex = lowestChangedIndex!!))
            }
            return b
        }

        final override fun removeAt(index: Int): E {
            requireNonNegative(index)
            val e = list.removeAt(index)
            changedFromOuter(RemoveAt(list, e, index))
            return e
        }

        final override fun retainAll(elements: Collection<E>): Boolean {
            val lowestChangedIndex = withIndex().firstOrNull { it.value !in elements }?.index
            val toRemove = list.filter { it !in elements }
            val b = list.retainAll(elements)
            if (b) changedFromOuter(
                RetainAllList(
                    list,
                    toRemove,
                    retained = elements,
                    lowestChangedIndex = lowestChangedIndex!!
                )
            )
            return b
        }

        final override fun set(
            index: Int,
            element: E
        ): E {
            val oldElement = list.set(index, element)
            changedFromOuter(ReplaceAt(list, removed = oldElement, added = element, index = index))
            return oldElement
        }

        final override fun atomicChange(op: MutableObsList<E>.() -> Unit) {
            requireNot(isAtomicallyChanging)
            val changes = mutableListOf<ListChange<E>>()
            atomicChanges = changes
            isAtomicallyChanging = true
            op()
            isAtomicallyChanging = false
            atomicChanges = null
            if (changes.isNotEmpty()) {
                emitChange(AtomicListChange(this, changes))
            }
        }

        fun changedFromOuter(c: ListChange<E>) {
            invalidateSubLists()
            processChange(c)
        }

        private var isAtomicallyChanging = false
        private var atomicChanges: MutableList<ListChange<E>>? = null
        fun processChange(c: ListChange<E>) {
            if (isAtomicallyChanging) {
                atomicChanges!!.add(c)
            } else {
                emitChange(c)
            }
        }


        final override fun subList(
            fromIndex: Int,
            toIndex: Int
        ) = inSync { SubList(fromIndex, toIndex) }

        private fun invalidateSubLists() =
            inSync {
                validSubLists.forEach { it.isValid = false }
                validSubLists.clear()
            }

        private var validSubLists = mutableListOf<SubList>()

        inner class SubList(
            private val fromIndex: Int,
            private var toIndexExclusive: Int
        ) :
            MutableList<E> {
            internal var isValid = true

            private val subList = list.subList(fromIndex, toIndexExclusive)

            init {
                validSubLists += this
            }

            override val size: Int
                get() {
                    return inSync(this@BasicObservableListImpl) {
                        require(isValid)
                        subList.size
                    }
                }

            override fun clear() {
                inSync(this@BasicObservableListImpl) {
                    require(isValid)
                    val subListIsEmpty = isEmpty()
                    if (!subListIsEmpty) {
                        val copy = subList.toList()
                        val copyWithIndices = copy.zip(fromIndex..<toIndexExclusive)
                        val copyWithIndices2 = copyWithIndices.map { IndexedValue(it.second, it.first) }
                        subList.clear()
                        toIndexExclusive = fromIndex
                        processChange(
                            RemoveAtIndices(
                                collection = this@BasicObservableListImpl,
                                removed = copyWithIndices2,
                                quickIsRange = true
                            )
                        )
                    }
                }
            }

            override fun addAll(elements: Collection<E>): Boolean =
                inSync(this@BasicObservableListImpl) {
                    require(isValid)
                    val addIndex = toIndexExclusive
                    toIndexExclusive += elements.size
                    val r = subList.addAll(elements)
                    processChange(
                        MultiAddAt(
                            collection = this@BasicObservableListImpl,
                            added = elements,
                            index = addIndex
                        )
                    )
                    r
                }

            override fun addAll(
                index: Int,
                elements: Collection<E>
            ): Boolean {
                TODO()
            }

            override fun add(
                index: Int,
                element: E
            ) {
                TODO()
            }

            override fun add(element: E): Boolean {
                require(isValid)
                val b = subList.add(element)
                require(b)
                toIndexExclusive++
                processChange(AddAt(this@BasicObservableListImpl, element, fromIndex + subList.size - 1))
                return b
            }

            override fun get(index: Int): E {
                require(isValid)
                return subList[index]
            }

            override fun isEmpty(): Boolean {
                require(isValid)
                return subList.isEmpty()
            }

            override fun iterator(): MutableIterator<E> {
                val itr by lazy { subList.iterator() }
                return FakeMutableIterator<E>(
                    object : Iterator<E> {
                        override fun hasNext(): Boolean {
                            require(isValid)
                            return itr.hasNext()
                        }

                        override fun next(): E {
                            require(isValid)
                            return itr.next()
                        }
                    }
                )
            }

            override fun listIterator() = NOT_IMPLEMENTED
            override fun listIterator(index: Int) = NOT_IMPLEMENTED
            override fun removeAt(index: Int) = NOT_IMPLEMENTED
            override fun subList(
                fromIndex: Int,
                toIndex: Int
            ) = NOT_IMPLEMENTED

            override fun set(
                index: Int,
                element: E
            ): E {
                require(isValid)
                val prev = subList.set(index, element)
                processChange(
                    ReplaceAt(
                        this@BasicObservableListImpl,
                        removed = prev,
                        added = element,
                        index = fromIndex + index
                    )
                )
                return prev
            }

            override fun retainAll(elements: Collection<E>) = NOT_IMPLEMENTED
            override fun removeAll(elements: Collection<E>): Boolean {
                require(isValid)
                val prevSize = subList.size
                val willRemove = subList.withIndex().filter { it.value in elements }
                val b = subList.removeAll(elements)
                val newSize = subList.size
                val numRemoved = prevSize - newSize
                toIndexExclusive -= numRemoved
                if (b) processChange(
                    RemoveAtIndices(
                        this@BasicObservableListImpl,
                        willRemove.map { IndexedValue(it.index + fromIndex, it.value) }
                    )
                )
                return b
            }

            override fun remove(element: E): Boolean {
                require(isValid)
                val i = subList.indexOf(element)
                val b = subList.remove(element)
                if (b) toIndexExclusive--
                if (b) processChange(RemoveAt(this@BasicObservableListImpl, element, i + fromIndex))
                return b
            }

            override fun lastIndexOf(element: E): Int {
                require(isValid)
                return subList.lastIndexOf(element)
            }

            override fun indexOf(element: E): Int {
                require(isValid)
                return subList.indexOf(element)
            }

            override fun containsAll(elements: Collection<E>): Boolean {
                require(isValid)
                return subList.containsAll(elements)
            }

            override fun contains(element: E): Boolean {
                require(isValid)
                return subList.contains(element)
            }
        }
    }


fun <E, R> ImmutableObsList<E>.view(converter: (E) -> R) =
    object : ImmutableObsList<R> {


        override fun onChange(
            listenerName: String?,
            op: (ListChange<R>) -> Unit
        ): MyListenerInter<*> =
            this@view.onChange {
                op(it.convert(this, converter))
            }

        override val size: Int
            get() = this@view.size

        override fun isEmpty(): Boolean = this@view.isEmpty()

        override fun iterator(): Iterator<R> = listIterator()

        override fun addListener(listener: ListListenerBase<R>): ListListener<R> {
            TODO()
        }

        override var nam: String?
            get() = this@view.nam
            set(value) {
                this@view.nam = value
            }

        override fun removeListener(listener: MyListenerInter<*>) {
            TODO()
        }

        override var debugger: Prints?
            get() = this@view.debugger
            set(value) {
                this@view.debugger = value
            }

        override fun get(index: Int): R = converter(this@view[index])

        override fun listIterator() = listIterator(0)

        override fun listIterator(index: Int) =
            object : ListIterator<R> {
                private val itr = this@view.listIterator(index)
                override fun hasNext() = itr.hasNext()

                override fun hasPrevious(): Boolean {
                    TODO()
                }

                override fun next() = converter(itr.next())

                override fun nextIndex(): Int {
                    TODO()
                }

                override fun previous(): R {
                    TODO()
                }

                override fun previousIndex(): Int {
                    TODO()
                }
            }

        override fun subList(
            fromIndex: Int,
            toIndex: Int
        ): List<R> {
            TODO()
        }

        override fun lastIndexOf(element: R): Int {
            TODO()
        }

        override fun indexOf(element: R): Int {
            this@view.forEachIndexed { idx, it ->
                if (converter(it) == element) return idx
            }
            return -1
        }

        override fun containsAll(elements: Collection<R>): Boolean {
            TODO()
        }

        override fun contains(element: R): Boolean {
            TODO()
        }

        override fun releaseUpdatesAfter(op: Op) {
            TODO()
        }
    }

inline fun <reified E, reified T : BasicObservableListImpl<E>> T.withChangeListener(
    listenerName: String? = null,
    noinline listener: (ListChange<E>) -> Unit
): T {
    onChange(listenerName = listenerName, listener)
    return this
}


val <E> ImmutableObsList<E>.sizeProperty get() = binding { size }

val <E> ImmutableObsList<E>.isEmptyProperty get() = binding { isEmpty() }
val <E> ImmutableObsList<E>.isNotEmptyProperty get() = binding { isNotEmpty() }


/*-1 if list is empty*/
val <E> ImmutableObsList<E>.lastIndexProperty get() = sizeProperty.binding { it - 1 }

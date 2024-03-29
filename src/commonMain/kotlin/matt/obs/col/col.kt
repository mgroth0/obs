package matt.obs.col

import matt.lang.anno.JetBrainsYouTrackProject.KT
import matt.lang.anno.Open
import matt.lang.anno.YouTrackIssue
import matt.lang.weak.common.WeakRefInter
import matt.model.flowlogic.keypass.KeyPass
import matt.obs.bindhelp.BindableList
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.NonNullCollectionChange
import matt.obs.col.change.QueueChange
import matt.obs.col.change.SetChange
import matt.obs.common.MListenable
import matt.obs.common.MObservableImpl
import matt.obs.listen.CollectionListener
import matt.obs.listen.CollectionListenerBase
import matt.obs.listen.ListListener
import matt.obs.listen.ListListenerBase
import matt.obs.listen.MyListenerInter
import matt.obs.listen.NonNullCollectionListener
import matt.obs.listen.NonNullCollectionListenerBase
import matt.obs.listen.QueueListener
import matt.obs.listen.SetListener
import matt.obs.listen.SetListenerBase
import matt.obs.listen.update.CollectionUpdate
import matt.obs.listen.update.ListUpdate
import matt.obs.listen.update.NonNullCollectionUpdate
import matt.obs.listen.update.QueueUpdate
import matt.obs.listen.update.SetUpdate
import matt.obs.prop.ValProp
import matt.obs.prop.writable.VarProp

interface NonNullBasicOCollection<E, C : NonNullCollectionChange<E, out Collection<E>>, U : NonNullCollectionUpdate<E, C>, L : NonNullCollectionListenerBase<E, C, U>> :
    Collection<E>,
    MListenable<L> {
    @Open
    override fun observe(op: () -> Unit) = onChange { op() }

    @Open
    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ) = onChangeWithAlreadyWeak(w) { _, _ ->
        op()
    }

    abstract fun onChange(
        listenerName: String? = null,
        op: (C) -> Unit
    ): MyListenerInter<*>

    abstract fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, C) -> Unit
    ): MyListenerInter<*>

    abstract fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: WeakRefInter<W>,
        op: (W, C) -> Unit
    ): MyListenerInter<*>
}


interface BasicOCollection<E, C : CollectionChange<E, out Collection<E>>, U : CollectionUpdate<E, C>, L : CollectionListenerBase<E, C, U>> :

/*

TEMPORARILY commenting out Collection<E> here as I wait for this issue to be fixed


Collection<E>,*/


    @YouTrackIssue(KT, 65555)
    MListenable<L> {




    fun isEmpty(): Boolean
    val size: Int
    fun iterator(): Iterator<E>
    fun containsAll(elements: Collection<E>): Boolean
    fun contains(element: E): Boolean

    @Open
    fun tempDebugCollectionDelegate() =
        object: Collection<E> {
            override val size: Int
                get() = this@BasicOCollection.size

            override fun isEmpty(): Boolean = this@BasicOCollection.isEmpty()

            override fun iterator(): Iterator<E> = this@BasicOCollection.iterator()

            override fun containsAll(elements: Collection<E>): Boolean = this@BasicOCollection.containsAll(elements)

            override fun contains(element: E): Boolean = this@BasicOCollection.contains(element)
        }

    @Open
    override fun observe(op: () -> Unit) = onChange { op() }

    @Open
    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ) = onChangeWithAlreadyWeak(w) { _, _ ->
        op()
    }


    fun onChange(
        listenerName: String? = null,
        op: (C) -> Unit
    ): MyListenerInter<*>

    fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, C) -> Unit
    ): MyListenerInter<*>

    fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: WeakRefInter<W>,
        op: (W, C) -> Unit
    ): MyListenerInter<*>
}


typealias IBObsCol = InternallyBackedOCollection<*, *, *, *>

abstract class InternallyBackedONonNullCollection<E, C : NonNullCollectionChange<E, out Collection<E>>, U : NonNullCollectionUpdate<E, C>, L : NonNullCollectionListenerBase<E, C, U>> :
    MObservableImpl<U, L>(),
    NonNullBasicOCollection<E, C, U, L> {


    final override fun onChange(
        listenerName: String?,
        op: (C) -> Unit
    ): L {
        val l =
            createListener {
                op(it)
            }
        return addListener(
            l.also {
                if (listenerName != null) it.name = listenerName
            }
        )
    }

    protected abstract fun createListener(invoke: NonNullCollectionListener<E, C, U>.(change: C) -> Unit): L

    internal val bindWritePass = KeyPass()

    protected fun emitChange(change: C) {
        require(this !is BindableList<*> || !isBound || bindWritePass.isHeld)
        notifyListeners(updateFrom((change)))
    }

    protected abstract fun updateFrom(c: C): U

    val isEmptyProp: ValProp<Boolean> by lazy {
        VarProp(isEmpty()).apply {
            onChange {
                value = this@InternallyBackedONonNullCollection.isEmpty()
            }
        }
    }
}

abstract class InternallyBackedOCollection<E, C : CollectionChange<E, out Collection<E>>, U : CollectionUpdate<E, C>, L : CollectionListenerBase<E, C, U>> :
    MObservableImpl<U, L>(),
    BasicOCollection<E, C, U, L> {


    final override fun onChange(
        listenerName: String?,
        op: (C) -> Unit
    ): L {
        val l =
            createListener {
                op(it)
            }
        return addListener(
            l.also {
                if (listenerName != null) it.name = listenerName
            }
        )
    }

    protected abstract fun createListener(invoke: CollectionListener<E, C, U>.(change: C) -> Unit): L

    internal val bindWritePass = KeyPass()

    protected fun emitChange(change: C) {
        require(this !is BindableList<*> || !isBound || bindWritePass.isHeld)
        notifyListeners(updateFrom((change)))
    }

    protected abstract fun updateFrom(c: C): U

    val isEmptyProp: ValProp<Boolean> by lazy {
        VarProp(isEmpty()).apply {
            onChange {
                value = this@InternallyBackedOCollection.isEmpty()
            }
        }
    }
}

abstract class InternallyBackedOSet<E> internal constructor() :
    InternallyBackedOCollection<E, SetChange<E>, SetUpdate<E>, SetListenerBase<E>>() {

        final override fun updateFrom(c: SetChange<E>): SetUpdate<E> = SetUpdate(c)

        final override fun createListener(invoke: CollectionListener<E, SetChange<E>, SetUpdate<E>>.(change: SetChange<E>) -> Unit): SetListenerBase<E> {
            val l = SetListener<E>(invoke)
            return l
        }
    }

abstract class InternallyBackedOList<E> internal constructor() :
    InternallyBackedOCollection<E, ListChange<E>, ListUpdate<E>, ListListenerBase<E>>() {
        final override fun updateFrom(c: ListChange<E>): ListUpdate<E> = ListUpdate(c)

        final override fun createListener(invoke: CollectionListener<E, ListChange<E>, ListUpdate<E>>.(change: ListChange<E>) -> Unit): ListListenerBase<E> {
            val l = ListListener<E>(invoke)
            return l
        }

        abstract override val size: Int
    }

interface BasicOMutableCollection<E, C : CollectionChange<E, out Collection<E>>, U : CollectionUpdate<E, C>> :
    BasicOCollection<E, C, U, CollectionListenerBase<E, C, U>>,
    MutableCollection<E>


abstract class InternallyBackedOQueue<E : Any> internal constructor() :
    InternallyBackedONonNullCollection<E, QueueChange<E>, QueueUpdate<E>, QueueListener<E>>() {
        final override fun updateFrom(c: QueueChange<E>): QueueUpdate<E> = QueueUpdate(c)

        final override fun createListener(invoke: NonNullCollectionListener<E, QueueChange<E>, QueueUpdate<E>>.(change: QueueChange<E>) -> Unit): QueueListener<E> {
            val l = QueueListener<E>(invoke)
            return l
        }

        final override fun <W : Any> onChangeWithAlreadyWeak(
            weakRef: WeakRefInter<W>,
            op: (W, QueueChange<E>) -> Unit
        ): MyListenerInter<*> {
            TODO()
        }

        final override fun <W : Any> onChangeWithWeak(
            o: W,
            op: (W, QueueChange<E>) -> Unit
        ): MyListenerInter<*> {
            TODO()
        }
    }

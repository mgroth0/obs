package matt.obs.bindhelp

import matt.lang.anno.Open
import matt.lang.common.go
import matt.lang.convert.BiConverter
import matt.lang.convert.Converter
import matt.lang.setall.setAll
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.inSync
import matt.lang.weak.common.getValue
import matt.lang.weak.weak
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.bind.LazyBindableProp
import matt.obs.bind.binding
import matt.obs.col.IBObsCol
import matt.obs.col.change.mirror
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.oset.MutableObsSet
import matt.obs.col.oset.ObsSet
import matt.obs.common.MListenable
import matt.obs.listen.MyListenerInter
import matt.obs.prop.ObsVal
import matt.obs.prop.fx.FXBackedPropBase
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.Var

sealed interface Bindable {
    val bindManager: Bindable
    var theBind: ABind?
    fun unbind()
    @Open
    val isBound: Boolean get() = theBind != null
    @Open
    val isBoundUnidirectionally: Boolean get() = theBind is TheBind
    @Open
    val isBoundBidirectionally: Boolean get() = theBind is BiTheBind
}
sealed interface MoreTypeSafeBindable<T>: Bindable {
    override val bindManager: BindableValueHelper<T>
}

sealed class BindableImpl : Bindable, ReferenceMonitor {
    @Open
    override val bindManager get() = this
    final override var theBind: ABind? = null

    final override fun unbind() =
        inSync {
            require(this !is FXBackedPropBase || !isFXBound)
            theBind?.cut()
            theBind = null
        }
}

interface BindableList<E> : Bindable {
    @Open
    fun bind(source: ImmutableObsList<E>) = bind(source) { it }
    fun <S> bind(
        source: ImmutableObsList<S>,
        converter: (S) -> E
    )

    fun <S> bindWeakly(
        source: ImmutableObsList<S>,
        converter: (S) -> E
    )

    fun <S> bind(
        source: ObsVal<S>,
        converter: (S) -> List<E>
    )
}

interface BindableSet<E> : Bindable {
    @Open
    fun bind(source: ObsSet<E>) = bind(source) { it }
    fun <S> bind(
        source: ObsSet<S>,
        converter: (S) -> E
    )

    fun <S> bind(
        source: ObsVal<S>,
        converter: (S) -> Set<E>
    )
}

/*matt.log.todo.todo: lazily evaluated bound lists!*/
class BindableListImpl<E>(private val target: MutableObsList<E>) : BindableImpl(), BindableList<E> {

    override fun <S> bind(
        source: ImmutableObsList<S>,
        converter: (S) -> E
    ) = inSync {
        unbind()
        (target as? IBObsCol)?.bindWritePass?.hold()
        target.setAll(source.map(converter))
        (target as? IBObsCol)?.bindWritePass?.release()
        val listener =
            source.onChange {
                (target as? IBObsCol)?.bindWritePass?.hold()
                target.mirror(it, converter)
                (target as? IBObsCol)?.bindWritePass?.release()
            }
        theBind = TheBind(source = source, listener = listener)
    }

    override fun <S> bindWeakly(
        source: ImmutableObsList<S>,
        converter: (S) -> E
    ) = inSync {
        unbind()
        (target as? IBObsCol)?.bindWritePass?.hold()
        target.setAll(source.map(converter))
        (target as? IBObsCol)?.bindWritePass?.release()
        val listener =
            source.onChangeWithWeak(target) { targ, new ->
                (targ as? IBObsCol)?.bindWritePass?.hold()
                targ.mirror(new, converter)
                (targ as? IBObsCol)?.bindWritePass?.release()
            }
        theBind = TheBind(source = source, listener = listener)
    }

    override fun <S> bind(
        source: ObsVal<S>,
        converter: (S) -> List<E>
    ) = inSync {
        unbind()
        (target as? IBObsCol)?.bindWritePass?.hold()
        target.setAll(converter(source.value))
        (target as? IBObsCol)?.bindWritePass?.release()
        val listener =
            source.onChange {
                (target as? IBObsCol)?.bindWritePass?.hold()
                target.setAll(converter(it))
                (target as? IBObsCol)?.bindWritePass?.release()
            }
        theBind = TheBind(source = source, listener = listener)
    }
}

/*matt.log.todo.todo: lazily evaluated bound lists!*/
class BindableSetImpl<E>(private val target: MutableObsSet<E>) : BindableImpl(), BindableSet<E> {

    override fun <S> bind(
        source: ObsSet<S>,
        converter: (S) -> E
    ) = inSync {
        unbind()
        (target as? IBObsCol)?.bindWritePass?.hold()
        target.setAll(source.map(converter))
        (target as? IBObsCol)?.bindWritePass?.release()
        val listener =
            source.onChange {
                (target as? IBObsCol)?.bindWritePass?.hold()
                target.mirror(it, converter)
                (target as? IBObsCol)?.bindWritePass?.release()
            }
        theBind = TheBind(source = source, listener = listener)
    }

    override fun <S> bind(
        source: ObsVal<S>,
        converter: (S) -> Set<E>
    ) = inSync {
        unbind()
        (target as? IBObsCol)?.bindWritePass?.hold()
        target.setAll(converter(source.value))
        (target as? IBObsCol)?.bindWritePass?.release()
        val listener =
            source.onChange {
                (target as? IBObsCol)?.bindWritePass?.hold()
                target.setAll(converter(it))
                (target as? IBObsCol)?.bindWritePass?.release()
            }
        theBind = TheBind(source = source, listener = listener)
    }
}

interface BindableValue<T> : MoreTypeSafeBindable<T> {
    fun bind(source: ObsVal<out T>): ABind
    fun bindWeakly(source: ObsVal<out T>): ABind
    fun bindBidirectional(
        source: Var<T>,
        checkEquality: Boolean = false,
        clean: Boolean = true,
        debug: Boolean = false,
        weak: Boolean = false
    ): ABind

    fun <S> bindBidirectional(
        source: Var<S>,
        converter: BiConverter<T, S>
    ): ABind
}

/*this is the way to have a final interface fun!!!*/
fun <S, T> BindableValue<T>.bind(
    source: ObsVal<out S>,
    converter: Converter<S, T>
) =
    bind(source.binding { converter.convertToB(it) })

fun <S, T> BindableValue<T>.bindInv(
    source: ObsVal<out S>,
    converter: BiConverter<T, S>
) =
    bind(source.binding { converter.convertToA(it) })

fun <S, T> BindableValue<T>.bindBidirectionalInv(
    source: Var<S>,
    converter: BiConverter<S, T>
) =
    bindBidirectional(source, converter.invert())


/*reduce number of listeners, which contributes a huge amount to my object count*/
fun <T> bindMultipleTargetsTogether(
    targets: Collection<BindableValue<T>>,
    source: ObsVal<out T>
) {


    targets.forEach {
        require(it !is FXBackedPropBase || !it.isFXBound)
        it.unbind()

        (it.bindManager).apply {
            wProp setCorrectlyTo { source.value }
        }
    }


    val listener =
        source.observe {
            targets.forEach {
                (it.bindManager).apply {
                    wProp setCorrectlyTo { source.value }
                }
            }
        }

    val b = TheBind(source = source, listener = listener)
    targets.forEach {
        it.bindManager.theBind = b
    }
}

infix fun <TT> Var<TT>.setCorrectlyTo(new: () -> TT) {
    when (this) {
        is BindableProperty<TT> -> setFromBinding(new())
        is LazyBindableProp<TT> -> setFromBinding(new)
        else                    -> {
            value = new()
        }
    }
}

class BindableValueHelper<T>(internal val wProp: Var<T>) : BindableImpl(), BindableValue<T> {

    override val bindManager: BindableValueHelper<T>
        get() = this

    override fun bind(source: ObsVal<out T>): ABind =
        inSync {
            require(this !is FXBackedPropBase || !isFXBound)
            unbind()
            wProp setCorrectlyTo { source.value }
            val listener =
                source.observe {
                    wProp setCorrectlyTo { source.value }
                }
            val b = TheBind(source = source, listener = listener)
            theBind = b
            return b
        }


    override fun bindWeakly(source: ObsVal<out T>): TheBind =
        inSync {
            require(this !is FXBackedPropBase || !isFXBound)
            unbind()
            wProp setCorrectlyTo { source.value }
            val listener =
                source.onChangeWithWeak(wProp) { w, it ->
                    w setCorrectlyTo { it }
                }
            val b = TheBind(source = source, listener = listener)
            theBind = b
            return b
        }

    override fun bindBidirectional(
        source: Var<T>,
        checkEquality: Boolean,
        clean: Boolean,
        debug: Boolean,
        weak: Boolean
    ): ABind =
        inSync {
            if (clean) {
                unbind()
                source.unbind()
            }
            if (!checkEquality || source.value != wProp.value) {
                wProp setCorrectlyTo { source.value }
            }

            val rBlocker = RecursionBlocker()

            fun createListener(
                theSource: Var<T>,
                theTarget: Var<T>
            ): MyListenerInter<*> =
                if (weak) {
                    theSource.onChangeWithWeak(theTarget) { deRefedTarget, newValue ->
                        if (!checkEquality || newValue != deRefedTarget.value) {
                            rBlocker.with {
                                deRefedTarget setCorrectlyTo { newValue }
                            }
                        }
                    }
                } else {
                    theSource.onChange { newValue ->
                        if (!checkEquality || newValue != theTarget.value) {
                            rBlocker.with {
                                theTarget setCorrectlyTo { newValue }
                            }
                        }
                    }
                }


            val sourceListener = createListener(source, wProp)
            val targetListener = createListener(wProp, source)


            val b =
                BiTheBind(
                    source = source,
                    target = wProp,
                    sourceListener = sourceListener,
                    targetListener = targetListener,
                    debug = debug
                )
            theBind = b

            source.theBind = b
            return b
        }

    override fun <S> bindBidirectional(
        source: Var<S>,
        converter: BiConverter<T, S>
    ): BiTheBind =
        inSync {
            unbind()
            source.unbind()
            wProp setCorrectlyTo { converter.convertToA(source.value) }

            val rBlocker = RecursionBlocker()
            val sourceListener =
                source.observe {
                    rBlocker.with {
                        wProp setCorrectlyTo { converter.convertToA(source.value) }
                    }
                }
            val targetListener =
                wProp.observe {
                    rBlocker.with {
                        source setCorrectlyTo { converter.convertToB(wProp.value) }
                    }
                }

            val b =
                BiTheBind(source = source, target = wProp, sourceListener = sourceListener, targetListener = targetListener)
            theBind = b
            source.theBind = b
            return b
        }
}

interface ABind {
    fun cut()
}

class TheBind(
    source: MListenable<*>,
    listener: MyListenerInter<*>
) : ABind {
    private val source by weak(source)
    private val listener by weak(listener)
    override fun cut() {
        listener?.go { l ->
            source?.removeListener(l)
        }
    }
}


class BiTheBind(
    source: Var<*>,
    target: Var<*>,
    sourceListener: MyListenerInter<*>,
    targetListener: MyListenerInter<*>,
    private val debug: Boolean = false
) : ABind {
    private val source by weak(source)
    private val target by weak(target)
    private val sourceListener by weak(sourceListener)
    private val targetListener by weak(targetListener)
    override fun cut() {
        if (debug) println("cutting $this")
        sourceListener?.go {
            source?.removeListener(it)
        }
        targetListener?.go {
            target?.removeListener(it)
        }
        source?.theBind = null
        target?.theBind = null
    }
}

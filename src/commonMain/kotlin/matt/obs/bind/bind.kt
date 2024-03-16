package matt.obs.bind

import matt.lang.anno.Open
import matt.lang.common.err
import matt.lang.convert.BiConverter
import matt.lang.convert.Converter
import matt.lang.setall.setAll
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.inSync
import matt.lang.weak.common.WeakRefInter
import matt.lang.weak.weak
import matt.model.flowlogic.keypass.KeyPass
import matt.model.op.debug.DebugLogger
import matt.obs.bindhelp.BindableValue
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.col.BasicOCollection
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.MutableObsList
import matt.obs.common.MObservable
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.DependencyHelper
import matt.obs.lazy.DependentValue
import matt.obs.listen.InvalidListener
import matt.obs.listen.NewOrLessListener
import matt.obs.listen.WeakInvalidListener
import matt.obs.listen.update.LazyMaybeNewValueUpdate
import matt.obs.listen.update.ValueUpdate
import matt.obs.oobj.MObservableObject
import matt.obs.prop.MObservableROValBase
import matt.obs.prop.MObservableVal
import matt.obs.prop.MObservableValNewOnly
import matt.obs.prop.ObsVal
import matt.obs.prop.ValProp
import matt.obs.prop.cast.CastedWritableProp
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.Var
import matt.obs.prop.writable.VarProp
import matt.obs.prop.writable.WritableMObservableVal


infix fun <T : Any> ObsVal<T?>.coalesceNull(backup: ObsVal<T?>) =
    MyBinding(this, backup) {
        this@coalesceNull.value ?: backup.value
    }


fun <T> BindableValue<T>.smartBind(
    property: ValProp<T>,
    readonly: Boolean
) {
    if (readonly || (property !is Var<*>)) bind(property) else bindBidirectional(property as VarProp)
}

fun <T> BindableValue<T>.smartBind(
    property: MObservableVal<T, *, *>,
    readonly: Boolean,
    weak: Boolean = false
) {
    /*why did I have that requirement???

    require(property is ReadOnlyBindableProperty)*/
    if (readonly || (property !is Var<T>)) {
        if (weak) {
            bindWeakly(property)
        } else bind(property)
    } else bindBidirectional(property, weak = weak)
}

fun <T, E> ObsVal<T>.boundList(propGetter: (T) -> Iterable<E>): MutableObsList<E> =
    BasicObservableListImpl(propGetter(value)).apply {
        this@boundList.onChange {
            setAll(propGetter(it).toList())
        }
    }

fun <T, R> MObservableObject<T>.binding(
    vararg dependencies: MObservable,
    op: T.() -> R
): MyBinding<R> = MyBinding(this, *dependencies) { noLongerUncheckedThis.op() }

fun <T, R> ObsVal<T>.binding(
    vararg dependencies: MObservable,
    op: (T) -> R
) = MyBinding(this, *dependencies) { op(value) }


fun <W : Any, T, R> ObsVal<T>.weakBinding(
    w: W,
    vararg dependencies: MObservable,
    op: (W, T) -> R
) = MyWeakBinding(w, this, *dependencies) { ww -> op(ww, value) }

fun <T, R> ObsVal<T>.binding(
    vararg dependencies: MObservable,
    converter: Converter<T, R>
) = MyBinding(this, *dependencies) { converter.convertToB(value) }


fun <E, R> BasicOCollection<E, *, *, *>.binding(
    vararg dependencies: MObservable,
    op: (BasicOCollection<E, *, *, *>) -> R
): MyBinding<R> = MyBinding(this, *dependencies) { op(this) }


fun <T, R> ObsVal<T>.deepBinding(
    vararg dependencies: MObservable,
    debugLogger: DebugLogger? = null,
    propGetter: (T) -> ObsVal<R>
) = MyBinding(
    *dependencies, this
) {
    propGetter(value).value
}.apply {
    addDependency(
        mainDep = this@deepBinding,
        moreDeps = listOf(*dependencies),
        debugLogger = debugLogger,
        { propGetter(it.value) }
    )
}

fun <T, R> ObsVal<T>.deepBindingIgnoringFutureNullOuterChanges(
    vararg deps: MObservable,
    propGetter: (T) -> ObsVal<R>?
) = MyBinding(this, *deps) {
    propGetter(value)?.value
}.apply {
    addDependencyIgnoringFutureNullOuterChanges(
        this@deepBindingIgnoringFutureNullOuterChanges,
        { propGetter(it.value) }
    )
}


fun <T> MyBinding<T>.eager() =
    BindableProperty(value).also { prop ->
        onChange {
            prop.value = it
        }
    }

interface MyBindingBase<T> : MObservableValNewOnly<T>, CustomDependencies {
    open override fun removeDependency(o: MObservable) {
    }
}


abstract class MyBindingBaseImpl<T> :
    MObservableROValBase<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>, out ValueUpdate<T>>>(),
    MyBindingBase<T>,
    CustomDependencies {



    protected abstract fun calc(): T

    final override fun observe(op: () -> Unit) =
        addListener(
            InvalidListener {
                op()
            }
        )

    final override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): WeakInvalidListener<T> {


        val l =
            WeakInvalidListener<T>(w) {
                op()
            }
        addListener(l)
        return l
    }

    protected val cVal = DependentValue { calc() }
    var stopwatch by cVal::stopwatch

    final override fun markInvalid() {
        cVal.markInvalid()
        notifyListeners(LazyMaybeNewValueUpdate { value })
    }

    private val depHelper by lazy { DependencyHelper(this) }

    final override fun <O : MObservable> addDependency(
        mainDep: O,
        moreDeps: List<MObservable>?,
        debugLogger: DebugLogger?, /*vararg dependencies: MObservable,*/
        vararg deepDependencies: (O) -> MObservable?
    ) = depHelper.addDependency(mainDep = mainDep, moreDeps = moreDeps, debugLogger = debugLogger, *deepDependencies)

    final override fun <O : MObservable> addDependencyWithDeepList(
        o: O,
        deepDependencies: (O) -> List<MObservable>
    ) = depHelper.addDependencyWithDeepList(o, deepDependencies)

    final override fun <O : ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
        o: O,
        vararg deepDependencies: (O) -> MObservable?
    ) = depHelper.addDependencyIgnoringFutureNullOuterChanges(o, *deepDependencies)

    final override fun removeDependency(o: MObservable) = depHelper.removeDependency(o)

    final override fun addDependencies(vararg obs: MObservable) {
        super<CustomDependencies>.addDependencies(*obs)
    }


    fun removeAllDependencies() = depHelper.removeAllDependencies()

    final override fun <O : MObservable> addWeakDependency(
        weakRef: WeakRefInter<*>,
        mainDep: O,
        moreDeps: List<MObservable>?,
        debugLogger: DebugLogger?,
        vararg deepDependencies: (O) -> MObservable?
    ) = depHelper.addWeakDependency(
        weakRef = weakRef, mainDep = mainDep, moreDeps = moreDeps, debugLogger = debugLogger, *deepDependencies
    )
}

open class MyBinding<T>(
    vararg dependencies: MObservable,
    private val calcArg: () -> T
) : MyBindingBaseImpl<T>() {

    final override fun calc(): T = calcArg()

    init {
        addDependencies(*dependencies)
    }

    final override val value: T get() = cVal.get()
}

open class MyWeakBinding<W : Any, T>(
    w: W,
    vararg dependencies: MObservable,
    private val calcArg: (W) -> T
) : MyBindingBaseImpl<T>() {

    private val weakRef = weak(w)

    final override fun calc(): T {
        val w = weakRef.deref()
        if (w == null) {
            err("I guess this does not work yet")
        } else {
            return calcArg(w)
        }
    }

    init {

        addWeakDependencies(weakRef, *dependencies)
    }

    final override val value: T get() = cVal.get()
}


open class LazyBindableProp<T>(
    private val calcArg: () -> T
) : MyBindingBaseImpl<T>(),
    WritableMObservableVal<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>, out ValueUpdate<T>>>,
    ReferenceMonitor {

    @Open
    override fun <R> cast(cast: (T) -> R): ObsVal<R> = super<MyBindingBaseImpl>.cast(cast)

    @Open
    override fun <R> cast(
        cast: (T) -> R,
        castBack: (R) -> T
    ): CastedWritableProp<T, R> = CastedWritableProp(this, cast, castBack)


    constructor(t: T) : this({ t })

    final override fun calc(): T = calcArg()

    private val bindWritePass = KeyPass()
    final override var value: T
        get() = inSync { cVal.get() }
        set(value) {
            require(!isBound || bindWritePass.isHeld)
            cVal.setOp { value }
            markInvalid()
        }

    @Suppress("unused")
    internal fun setFromBinding(new: T) {
        bindWritePass.with {
            value = new
        }
    }

    fun setLazily(newCalc: () -> T) {
        require(!isBound || bindWritePass.isHeld)
        cVal.setOp(newCalc)
        markInvalid()
    }

    internal fun setFromBinding(newCalc: () -> T) {
        bindWritePass.with {
            setLazily(newCalc)
        }
    }

    final override val bindManager by lazy { BindableValueHelper(this) }
    final override fun bind(source: ObsVal<out T>) = bindManager.bind(source)
    final override fun bindWeakly(source: ObsVal<out T>) = bindManager.bindWeakly(source)
    final override fun bindBidirectional(
        source: Var<T>,
        checkEquality: Boolean,
        clean: Boolean,
        debug: Boolean,
        weak: Boolean
    ) = bindManager.bindBidirectional(source, checkEquality = checkEquality, clean = clean, debug = debug, weak = weak)

    final override fun <S> bindBidirectional(
        source: Var<S>,
        converter: BiConverter<T, S>
    ) = bindManager.bindBidirectional(source, converter)

    final override var theBind by bindManager::theBind
    final override fun unbind() = bindManager.unbind()
}


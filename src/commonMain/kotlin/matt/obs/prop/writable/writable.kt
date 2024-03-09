package matt.obs.prop.writable

import matt.lang.anno.Open
import matt.lang.convert.BiConverter
import matt.lang.function.Produce
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.common.inSyncOrJustRun
import matt.lang.sync.inSync
import matt.model.flowlogic.keypass.KeyPass
import matt.obs.bind.binding
import matt.obs.bindhelp.BindableValue
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.ValueListenerBase
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate
import matt.obs.prop.MObservableVal
import matt.obs.prop.ObsVal
import matt.obs.prop.ReadOnlyBindableProperty
import matt.obs.prop.cast.CastedWritableProp
import matt.obs.prop.newold.MObservableValNewAndOld
import matt.obs.prop.onNonNullChange
import matt.obs.prop.proxy.ProxyProp
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


typealias Var<T> = WritableMObservableVal<T, *, *>

interface WritableMObservableVal<T, U : ValueUpdate<T>, L : ValueListenerBase<T, U, *>> :
    MObservableVal<T, U, L>,
    BindableValue<T>,
    ReadWriteProperty<Any?, T> {


    override var value: T

    @Open
    override operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T
    ) {
        this.value = value
    }


    override fun <R> cast(): Var<R>

    @Open
    fun <R> proxy(converter: BiConverter<T, R>) = ProxyProp(this, converter)

    @Open
    fun <R> proxyInv(converter: BiConverter<R, T>) = ProxyProp(this, converter.invert())

    @Open
    infix fun v(value: T) {
        this.value = value
    }

    @Open
    fun readOnly() = binding { it }

    @Open
    fun takeNonNullChangesFrom(o: ObsVal<out T?>) {
        o.onNonNullChange {
            value = it!!
        }
    }

    @Open
    fun takeChangesFrom(o: ObsVal<out T>) {
        o.onChange {
            value = it
        }
    }

    @Open
    fun takeChangesFromWhen(
        o: ObsVal<out T>,
        predicate: () -> Boolean
    ) {
        o.onChange {
            if (predicate()) {
                value = it
            }
        }
    }
}


fun <T, O : Var<T>> O.withNonNullUpdatesFrom(o: ObsVal<out T?>): O =
    apply {
        takeNonNullChangesFrom(o)
    }

fun <T, O : Var<T>> O.withUpdatesFrom(o: ObsVal<out T>): O =
    apply {
        takeChangesFrom(o)
    }

fun <T, O : Var<T>> O.withUpdatesFromWhen(
    o: ObsVal<out T>,
    predicate: () -> Boolean
): O =
    apply {
        takeChangesFromWhen(o, predicate)
    }



interface MWritableValNewAndOld<T> :
    WritableMObservableVal<T, ValueChange<T>, OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>>,
    MObservableValNewAndOld<T>


typealias VarProp<T> = BindableProperty<T>


typealias GoodVar<T> = MWritableValNewAndOld<T>


open class BindableProperty<T>(value: T) :
    ReadOnlyBindableProperty<T>(value),
    MWritableValNewAndOld<T>,
    BindableValue<T> {

    @Open
    override fun <R> cast() = CastedWritableProp<T, R>(this)

    val monitorForSetting = SimpleReferenceMonitor()

    private val bindWritePass by lazy { KeyPass() }

    fun setIfDifferent(newValue: T) {
        if (value != newValue) {
            value = newValue
        }
    }

    open override var value = value
        public set(v) {
            if (v != field) {
                require(!isBoundUnidirectionally || bindWritePass.isHeld) {
                    "isBoundUnidirectionally=$isBoundUnidirectionally, bindWritePass.isHeld=${bindWritePass.isHeld}"
                }
                inSyncOrJustRun(monitorForSetting) {
                    val old = field
                    field = v
                    notifyListeners(ValueChange(old, v))
                }
            }
        }

    internal fun setFromBinding(new: T) {
        bindWritePass.with {
            value = new
        }
    }

    final override val bindManager by lazy { BindableValueHelper(this) }


    final override infix fun bind(source: ObsVal<out T>) = bindManager.bind(source)
    final override infix fun bindWeakly(source: ObsVal<out T>) = bindManager.bindWeakly(source)

    /*allows property to still be set by other means*/
    fun pseudoBind(source: ObsVal<out T>) {
        value = source.value
        source.onChange {
            value = it
        }
    }

    final override fun bindBidirectional(
        source: Var<T>,
        checkEquality: Boolean,
        clean: Boolean,
        debug: Boolean,
        weak: Boolean
    ) =
        bindManager.bindBidirectional(source, checkEquality = checkEquality, clean = clean, debug = debug, weak = weak)

    final override fun <S> bindBidirectional(
        source: Var<S>,
        converter: BiConverter<T, S>
    ) =
        bindManager.bindBidirectional(source, converter)

    final override var theBind by bindManager::theBind
    final override fun unbind() = bindManager.unbind()
}



class SynchronizedProperty<T>(value: T) : BindableProperty<T>(value) {
    @PublishedApi
    internal val monitor = SimpleReferenceMonitor()
    fun <R> with(op: Produce<R>) = inSync(monitor, op)


    override var value: T
        get() = inSync(monitor) { super.value }
        set(value) {
            inSync(monitor) {
                super.value = value
            }
        }
}


fun Var<Boolean>.toggle() {
    value = !value
}


/**
 * Listen to changes and update the value of the property if the given mutator results in a different value
 */
fun <T> Var<T>.mutateOnChange(mutator: (T) -> T) =
    onChange {
        val changed = mutator(value)
        if (changed != value) value = changed
    }



/*THIS SHOULD BE DISALLOWED BY MY [[NoExtensionOfAny]]*/
fun <A : Any?> A.toVarProp() = VarProp(this)

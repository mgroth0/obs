package matt.obs.prop

import matt.lang.anno.Open
import matt.lang.model.value.ValueWrapper
import matt.lang.weak.common.WeakRefInter
import matt.lang.weak.common.lazySoft
import matt.lang.weak.weak
import matt.obs.bind.binding
import matt.obs.bindings.bool.not
import matt.obs.common.MListenable
import matt.obs.common.MObservableImpl
import matt.obs.listen.ChangeListener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.NewOrLessListener
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.OldAndNewListenerImpl
import matt.obs.listen.ValueListenerBase
import matt.obs.listen.WeakChangeListenerWithNewValue
import matt.obs.listen.WeakListenerWithOld
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate
import matt.obs.prop.newold.MObservableValNewAndOld
import matt.obs.prop.writable.BindableProperty
import matt.service.scheduler.Scheduler
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.cast

typealias ObsVal<T> = MObservableVal<T, *, *>



interface MObservableVal<T, U : ValueUpdate<T>, L : ValueListenerBase<T, U, out ValueUpdate<T>>> :
    MListenable<L>,
    ValueWrapper<T>,
    ReadOnlyProperty<Any?, T> {



    override val value: T


    fun <R> cast(cast: (T) -> R): ObsVal<R>
    fun <R> cast(cast: (T) -> R, castBack: (R) -> T): ObsVal<R>


    override fun observe(op: () -> Unit): MyListenerInter<*>

    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*>



    fun onChange(op: (T) -> Unit): L


    fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, T) -> Unit
    ): MyListenerInter<*>

    fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: WeakRefInter<W>,
        op: (W, T) -> Unit
    ): MyListenerInter<*>
}








fun <T> ObsVal<T>.onNonNullChange(op: (T & Any) -> Unit) =
    onChange {
        if (it != null) op(it)
    }

fun <T> ObsVal<T>.on(
    valueCheck: T,
    op: (T) -> Unit
) = onChange {
    if (it == valueCheck) op(it)
}

fun <T> ObsVal<T>.onChangeOnce(op: (T) -> Unit) =
    onChange(op).apply {
        removeAfterInvocation = true
    }

inline fun <reified T> ObsVal<T>.onChangeUntilInclusive(
    crossinline until: (T) -> Boolean,
    crossinline op: (T) -> Unit
) = onChange {
    op(it)
}.apply {
    untilInclusive = {
        until(it.new)
    }
}
inline fun <reified T> ObsVal<T>.onChangeUntilExclusive(
    crossinline until: (T) -> Boolean,
    crossinline op: (T) -> Unit
) = onChange {
    op(it)
}.apply {
    untilExclusive = { until(it.new) }
}






infix fun <T> ObsVal<T>.eqNow(value: T) = this.value == value
infix fun <T> ObsVal<T>.eqNow(value: ObsVal<T>) = this.value == value.value
infix fun <T> ObsVal<T>.notEqNow(value: T) = this.value != value
infix fun <T> ObsVal<T>.notEqNow(value: ObsVal<T>) = this.value != value.value











fun <T, O : ObsVal<T>> O.withChangeListener(op: (T) -> Unit) =
    apply {
        onChange(op)
    }


interface MObservableValNewOnly<T> :
    MObservableVal<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>, out ValueUpdate<T>>> {


    @Open
    override fun onChange(op: (T) -> Unit) =
        addListener(
            ChangeListener { new ->
                op(new)
            }
        )

    @Open
    override fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, T) -> Unit
    ) = run {
        val weakRef = weak(o)
        onChangeWithAlreadyWeak(weakRef, op)
    }

    @Open
    override fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: WeakRefInter<W>,
        op: (W, T) -> Unit
    ) = run {
        val listener =
            WeakChangeListenerWithNewValue(weakRef) { o: W, new: T ->
                op(o, new)
            }.apply {
                removeCondition = { weakRef.deref() == null }
            }
        addListener(listener)
    }
}







abstract class MObservableROValBase<T, U : ValueUpdate<T>, L : ValueListenerBase<T, U, *>> :
    MObservableImpl<U, L>(),
    MObservableVal<T, U, L> {


    @Open
    override fun observe(op: () -> Unit): MyListenerInter<*> = onChange { op() }

    @Open
    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*> = onChangeWithAlreadyWeak(w) { _, _ -> op() }

    infix fun eq(other: ReadOnlyBindableProperty<*>) =
        binding(other) {
            it == other.value
        }


    infix fun neq(other: ReadOnlyBindableProperty<*>) = eq(other).not()

    infix fun eq(other: Any?) =
        binding {
            it == other
        }

    infix fun neq(other: Any?) = eq(other).not()


    val isNull by lazySoft {
        eq(null)
    }

    val isNotNull by lazySoft {
        isNull.not()
    }


    final override fun toStringProps() = super.toStringProps() + mapOf("value" to value.toString())



    final override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T = value


    open fun <R: T> cast(cls: KClass<R & Any>): ObsVal<R> =
        cast {
            cls.cast(it)
        }

    @Open
    override fun <R> cast(cast: (T) -> R, castBack: (R) -> T): ObsVal<R> =
        binding {
            /*
             * I GET IT!
             * The `cast` function from the kotlin standard reflection library is relatively safe because it inherently disallows casting to generic types, since it requires a class instance and you cannot have a class instance with filled type parameters! I think this applies for nullable types too!
             */
            cast(it)
        }

    @Open
    override fun <R> cast(cast: (T) -> R): ObsVal<R> =
        binding {
            cast(it)
        }
}

typealias ValProp<T> = ReadOnlyBindableProperty<T>


abstract class IntermediateROValPropIdkWhy<T> : MObservableROValBase<T, ValueChange<T>, OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>>() {
    final override fun onChange(op: (T) -> Unit) =
        addListener(
            OldAndNewListenerImpl { _, new ->
                op(new)
            }
        )
    final override fun <W : Any> onChangeWithWeak(
        o: W,
        op: (W, T) -> Unit
    ) = run {
        val weakRef = weak(o)
        onChangeWithAlreadyWeak(weakRef, op)
    }
    final override fun <W : Any> onChangeWithAlreadyWeak(
        weakRef: WeakRefInter<W>,
        op: (W, T) -> Unit
    ) = run {
        val listener =
            WeakListenerWithOld(weakRef) { o: W, _: T, new: T ->
                op(o, new)
            }.apply {
                removeCondition = { weakRef.deref() == null }
            }
        addListener(listener)
    }
}

open class ReadOnlyBindableProperty<T>(value: T) :
    IntermediateROValPropIdkWhy<T>(),
    MObservableValNewAndOld<T> {








    open override var value = value
        protected set(v) {
            if (v != field) {
                val old = v
                field = v
                notifyListeners(ValueChange(old, v))
            }
        }
}









fun <T> ObsVal<T>.wrapWithScheduledUpdates(scheduler: Scheduler? = null): ObsVal<T> {
    val w = BindableProperty(value)
    if (scheduler != null) onChange {
        scheduler.schedule {
            w.value = it
        }
    }
    else onChange {
        w.value = it
    }
    return w
}



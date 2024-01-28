package matt.obs.prop.weak

import matt.lang.convert.BiConverter
import matt.lang.function.Op
import matt.lang.weak.weak
import matt.model.op.prints.Prints
import matt.obs.bindhelp.ABind
import matt.obs.bindhelp.Bindable
import matt.obs.bindhelp.BindableValue
import matt.obs.listen.MyListenerInter
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.update.ValueChange
import matt.obs.prop.BindableProperty
import matt.obs.prop.MWritableValNewAndOld
import matt.obs.prop.ObsVal
import matt.obs.prop.Var

class WeakPropWrapper<T>(p: BindableProperty<T>) : MWritableValNewAndOld<T>,
    BindableValue<T> {

    private val weakProp = weak(p)
    private val prop get() = weakProp.deref()!!

    override fun bind(source: ObsVal<out T>) = bindWeakly(source)

    override fun bindWeakly(source: ObsVal<out T>) = prop.bindWeakly(source)

    override fun bindBidirectional(
        source: Var<T>,
        checkEquality: Boolean,
        clean: Boolean,
        debug: Boolean,
        weak: Boolean
    ) = TODO()

    override fun <S> bindBidirectional(source: Var<S>, converter: BiConverter<T, S>) = TODO()

    override fun releaseUpdatesAfter(op: Op) {
        TODO()
    }

    override fun addListener(listener: OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>): OldAndNewListener<T, ValueChange<T>, out ValueChange<T>> {
        TODO()
    }

    override var value: T
        get() = prop.value
        set(value) {
            prop.value = value
        }
    override var nam: String?
        get() = prop.nam
        set(value) {
            prop.nam = value
        }

    override fun removeListener(listener: MyListenerInter<*>) {
        prop.removeListener(listener)
    }

    override var debugger: Prints?
        get() = prop.debugger
        set(value) {
            prop.debugger = value
        }
    override val bindManager: Bindable
        get() = TODO()

    @Suppress("UNUSED_PARAMETER")
    override var theBind: ABind?
        get() = TODO()
        set(value) {
            TODO()
        }

    override fun unbind() {
        prop.unbind()
    }

}
package matt.obs.prop.weak

import matt.lang.weak.MyWeakRef
import matt.model.op.convert.Converter
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

class WeakPropWrapper<T>(p: BindableProperty<T>): MWritableValNewAndOld<T>,
												  BindableValue<T> {

  private val weakProp = MyWeakRef(p)
  private val prop get() = weakProp.deref()!!

  override fun bind(source: ObsVal<out T>) = bindWeakly(source)

  override fun bindWeakly(source: ObsVal<out T>) {
	prop.bindWeakly(source)
  }

  override fun bindBidirectional(source: Var<T>, checkEquality: Boolean, clean: Boolean, debug: Boolean, weak: Boolean) {
	TODO("Not yet implemented")
  }

  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) {
	TODO("Not yet implemented")
  }

  override fun addListener(listener: OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>): OldAndNewListener<T, ValueChange<T>, out ValueChange<T>> {
	TODO("Not yet implemented")
  }

  override var value: T
	get() = prop.value
	set(value) {
	  prop.value = value
	}
  override var nam: String?
	get() = prop.nam
	set(value) {prop.nam = value}

  override fun removeListener(listener: MyListenerInter<*>) {
	prop.removeListener(listener)
  }

  override var debugger: Prints?
	get() = prop.debugger
	set(value) {
	  prop.debugger = value
	}
  override val bindManager: Bindable
	get() = TODO("Not yet implemented")
  @Suppress("UNUSED_PARAMETER")
  override var theBind: ABind?
	get() = TODO("Not yet implemented")
	set(value) {
	  TODO("Not yet implemented")
	}

  override fun unbind() {
	prop.unbind()
  }

}
package matt.obs.bind

import matt.model.Converter
import matt.model.keypass.KeyPass
import matt.model.lazy.DependentValue
import matt.obs.MObservable
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.col.BasicOCollection
import matt.obs.invalid.UpdatesFromOutside
import matt.obs.invalid.invalidateDeeplyFrom
import matt.obs.listen.NewListener
import matt.obs.listen.update.LazyNewValueUpdate
import matt.obs.listen.update.ValueUpdate
import matt.obs.oobj.MObservableObject
import matt.obs.prop.MObservableROValBase
import matt.obs.prop.MObservableValNewOnly
import matt.obs.prop.ObsVal
import matt.obs.prop.Var
import matt.obs.prop.WritableMObservableVal
import kotlin.jvm.Synchronized


fun <T, R> MObservableObject<T>.binding(
  vararg dependencies: MObservable,
  op: T.()->R
): MyBinding<R> = MyBinding(this, *dependencies) { uncheckedThis.op() }

fun <T, R> ObsVal<T>.binding(
  vararg dependencies: MObservable,
  op: (T)->R,
) = MyBinding(this, *dependencies) { op(value) }


fun <E, R> BasicOCollection<E>.binding(
  vararg dependencies: MObservable,
  op: (BasicOCollection<E>)->R,
): MyBinding<R> = MyBinding(this, *dependencies) { op(this) }

fun <T, R> ObsVal<T>.deepBinding(propGetter: (T)->ObsVal<R>) = MyBinding(this) {
  propGetter(value).value
}.invalidateDeeplyFrom(this) { propGetter(value) }

interface MyBindingBase<T>: MObservableValNewOnly<T>, UpdatesFromOutside

abstract class MyBindingBaseImpl<T>(calc: ()->T): MObservableROValBase<T, ValueUpdate<T>, NewListener<T>>(),
												  MyBindingBase<T> {
  protected val cval = DependentValue(calc)

  @Synchronized override fun markInvalid() {
	cval.markInvalid()
	notifyListeners(LazyNewValueUpdate { value })
  }
}

class MyBinding<T>(vararg dependencies: MObservable, calc: ()->T):
  MyBindingBaseImpl<T>(calc) {

  init {
	setupDependencies(*dependencies)
  }

  override val value: T @Synchronized get() = cval.get()
}


class WritableBinding<T>(
  calc: ()->T
): MyBindingBaseImpl<T>(calc), WritableMObservableVal<T, ValueUpdate<T>, NewListener<T>> {

  private val bindWritePass = KeyPass()
  override var value: T
	@Synchronized get() = cval.get()
	set(value) {
	  require(!this.isBound || bindWritePass.isHeld)
	  cval.op = { value }
	  markInvalid()
	}

  @Suppress("unused")
  internal fun setFromBinding(new: T) {
	bindWritePass.with {
	  value = new
	}
  }

  internal fun setFromBinding(newCalc: ()->T) {
	cval.op = newCalc
	markInvalid()
  }

  override val bindManager by lazy { BindableValueHelper(this) }
  override fun bind(source: ObsVal<T>) = bindManager.bind(source)
  override fun bindBidirectional(source: Var<T>) = bindManager.bindBidirectional(source)
  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) =
	bindManager.bindBidirectional(source, converter)

  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()

}
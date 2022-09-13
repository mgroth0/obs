package matt.obs.bind

import matt.model.Converter
import matt.model.flowlogic.keypass.KeyPass
import matt.model.lazy.DependentValue
import matt.obs.MObservable
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.col.BasicOCollection
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.DependencyHelper
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
}.apply {
  addDependency(this@deepBinding, { propGetter(it.value) })
}

interface MyBindingBase<T>: MObservableValNewOnly<T>, CustomDependencies

abstract class MyBindingBaseImpl<T>(calc: ()->T): MObservableROValBase<T, ValueUpdate<T>, NewListener<T>>(),
												  MyBindingBase<T>, CustomDependencies {
  protected val cval = DependentValue(calc)

  @Synchronized override fun markInvalid() {
	cval.markInvalid()
	notifyListeners(LazyNewValueUpdate { value })
  }

  private val depHelper by lazy { DependencyHelper(this) }

  override fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable?) =
	depHelper.addDependency(o, *deepDependencies)

  override fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>) =
	depHelper.addDependencyWithDeepList(o, deepDependencies)

  override fun removeDependency(o: MObservable) = depHelper.removeDependency(o)

}

class MyBinding<T>(vararg dependencies: MObservable, calc: ()->T):
  MyBindingBaseImpl<T>(calc) {

  init {
	addDependencies(*dependencies)
  }

  override val value: T @Synchronized get() = cval.get()

}


class LazyBindableProp<T>(
  calc: ()->T
): MyBindingBaseImpl<T>(calc), WritableMObservableVal<T, ValueUpdate<T>, NewListener<T>> {

  constructor(t: T): this({ t })

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

  fun setLazily(newCalc: ()->T) {
	require(!this.isBound || bindWritePass.isHeld)
	cval.op = newCalc
	markInvalid()
  }

  internal fun setFromBinding(newCalc: ()->T) {
	bindWritePass.with {
	  setLazily(newCalc)
	}
  }

  override val bindManager by lazy { BindableValueHelper(this) }
  override fun bind(source: ObsVal<T>) = bindManager.bind(source)
  override fun bindBidirectional(source: Var<T>) = bindManager.bindBidirectional(source)
  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) =
	bindManager.bindBidirectional(source, converter)

  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()

}
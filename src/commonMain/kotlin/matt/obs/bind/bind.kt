package matt.obs.bind

import matt.lang.setAll
import matt.model.convert.Converter
import matt.model.flowlogic.keypass.KeyPass
import matt.obs.MObservable
import matt.obs.bindhelp.BindableValue
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.col.BasicOCollection
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.ObsList
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.DependencyHelper
import matt.obs.lazy.DependentValue
import matt.obs.listen.InvalidListener
import matt.obs.listen.NewOrLessListener
import matt.obs.listen.update.LazyNewValueUpdate
import matt.obs.listen.update.ValueUpdate
import matt.obs.oobj.MObservableObject
import matt.obs.prop.MObservableROValBase
import matt.obs.prop.MObservableVal
import matt.obs.prop.MObservableValNewOnly
import matt.obs.prop.ObsVal
import matt.obs.prop.ReadOnlyBindableProperty
import matt.obs.prop.ValProp
import matt.obs.prop.Var
import matt.obs.prop.VarProp
import matt.obs.prop.WritableMObservableVal
import kotlin.contracts.ExperimentalContracts
import kotlin.jvm.Synchronized

fun <T> BindableValue<T>.smartBind(property: ValProp<T>, readonly: Boolean) {
  if (readonly || (property !is VarProp<*>)) bind(property) else bindBidirectional(property as VarProp)
}

fun <T> BindableValue<T>.smartBind(property: MObservableVal<T, *, *>, readonly: Boolean) {
  require(property is ReadOnlyBindableProperty)
  if (readonly || (property !is VarProp<T>)) bind(property) else
	bindBidirectional(property)
}

fun <T, E> ObsVal<T>.boundList(propGetter: (T)->Iterable<E>): ObsList<E> =
  BasicObservableListImpl(propGetter(value)).apply {
	this@boundList.onChange {
	  setAll(propGetter(it).toList())
	}
  }

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

fun <T, R> ObsVal<T>.deepBindingIgnoringFutureNullOuterChanges(propGetter: (T)->ObsVal<R>?) = MyBinding(this) {
  propGetter(value)?.value
}.apply {
  addDependencyIgnoringFutureNullOuterChanges(this@deepBindingIgnoringFutureNullOuterChanges, { propGetter(it.value) })
}

interface MyBindingBase<T>: MObservableValNewOnly<T>, CustomDependencies

abstract class MyBindingBaseImpl<T>(calc: ()->T):
  MObservableROValBase<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>>>(),
  MyBindingBase<T>,
  CustomDependencies {


  override fun observe(op: ()->Unit) = addListener(InvalidListener {
	op()
  })

  protected val cval = DependentValue(calc)
  var stopwatch by cval::stopwatch

  @Synchronized override fun markInvalid() {
	cval.markInvalid()
	notifyListeners(LazyNewValueUpdate { value })
  }

  private val depHelper by lazy { DependencyHelper(this) }

  override fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable?) =
	depHelper.addDependency(o, *deepDependencies)

  override fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>) =
	depHelper.addDependencyWithDeepList(o, deepDependencies)

  override fun <O: ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
	o: O,
	vararg deepDependencies: (O)->MObservable?
  ) = depHelper.addDependencyIgnoringFutureNullOuterChanges(o, *deepDependencies)

  override fun removeDependency(o: MObservable) = depHelper.removeDependency(o)


}

@OptIn(ExperimentalContracts::class)
class MyBinding<T>(vararg dependencies: MObservable, calc: ()->T): MyBindingBaseImpl<T>(calc) {

  init {
	addDependencies(*dependencies)
  }

  override val value: T
	@Synchronized get() = cval.get()


}


open class LazyBindableProp<T>(
  calc: ()->T
): MyBindingBaseImpl<T>(calc), WritableMObservableVal<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>>> {

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
  override fun bind(source: ObsVal<out T>) = bindManager.bind(source)
  override fun bindBidirectional(source: Var<T>, checkEquality: Boolean) =
	bindManager.bindBidirectional(source, checkEquality = checkEquality)

  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) =
	bindManager.bindBidirectional(source, converter)

  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()

}
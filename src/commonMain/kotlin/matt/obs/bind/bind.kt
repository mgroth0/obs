package matt.obs.bind

import matt.lang.setAll
import matt.model.convert.Converter
import matt.model.debug.DebugLogger
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

fun <T, R> ObsVal<T>.binding(
  vararg dependencies: MObservable,
  converter: Converter<T, R>,
) = MyBinding(this, *dependencies) { converter.convertToB(value) }


fun <E, R> BasicOCollection<E>.binding(
  vararg dependencies: MObservable,
  op: (BasicOCollection<E>)->R,
): MyBinding<R> = MyBinding(this, *dependencies) { op(this) }


fun <T, R> ObsVal<T>.deepBinding(
  vararg dependencies: MObservable,
  debugLogger: DebugLogger? = null,
  propGetter: (T)->ObsVal<R>
) = MyBinding(
  *dependencies,
  this
) {
  propGetter(value).value
}.apply {
  addDependency(
	mainDep = this@deepBinding,
	moreDeps = listOf(*dependencies), debugLogger = debugLogger, { propGetter(it.value) })
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


  final override fun observe(op: ()->Unit) = addListener(InvalidListener {
	op()
  })

  protected val cVal = DependentValue(calc)
  var stopwatch by cVal::stopwatch

  final override fun markInvalid() {
	cVal.markInvalid()
	notifyListeners(LazyNewValueUpdate { value })
  }

  private val depHelper by lazy { DependencyHelper(this) }

  final override fun <O: MObservable> addDependency(
	mainDep: O,
	moreDeps: List<MObservable>,
	debugLogger: DebugLogger?,
	/*vararg dependencies: MObservable,*/
	vararg deepDependencies: (O)->MObservable?
  ) = depHelper.addDependency(mainDep = mainDep, moreDeps = moreDeps, debugLogger = debugLogger, *deepDependencies)

  final override fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>) =
	depHelper.addDependencyWithDeepList(o, deepDependencies)

  final override fun <O: ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
	o: O,
	vararg deepDependencies: (O)->MObservable?
  ) = depHelper.addDependencyIgnoringFutureNullOuterChanges(o, *deepDependencies)

  final override fun removeDependency(o: MObservable) = depHelper.removeDependency(o)

  final override fun addDependencies(vararg obs: MObservable) {
	super<CustomDependencies>.addDependencies(*obs)
  }

}

open class MyBinding<T>(vararg dependencies: MObservable, calc: ()->T): MyBindingBaseImpl<T>(calc) {

  init {
	addDependencies(*dependencies)
  }

  override val value: T get() = cVal.get()


}


open class LazyBindableProp<T>(
  calc: ()->T
): MyBindingBaseImpl<T>(calc), WritableMObservableVal<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>>> {

  constructor(t: T): this({ t })

  private val bindWritePass = KeyPass()
  override var value: T
	@Synchronized get() = cVal.get()
	set(value) {
	  require(!this.isBound || bindWritePass.isHeld)
	  cVal.setOp { value }
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
	cVal.setOp(newCalc)
	markInvalid()
  }

  internal fun setFromBinding(newCalc: ()->T) {
	bindWritePass.with {
	  setLazily(newCalc)
	}
  }

  final override val bindManager by lazy { BindableValueHelper(this) }
  override fun bind(source: ObsVal<out T>) = bindManager.bind(source)
  override fun bindBidirectional(source: Var<T>, checkEquality: Boolean, clean: Boolean, debug: Boolean) =
	bindManager.bindBidirectional(source, checkEquality = checkEquality, clean = clean, debug = debug)

  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) =
	bindManager.bindBidirectional(source, converter)

  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()

}
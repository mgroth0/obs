package matt.obs.prop

import matt.lang.weak.WeakRef
import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bind.MyBinding
import matt.obs.bindhelp.BindableValue
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.bindings.bool.ObsB
import matt.obs.bindings.bool.not
import matt.obs.listen.NewListener
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.ValueListener
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate
import matt.obs.prop.cast.CastedWritableProp
import kotlin.reflect.KProperty

typealias ObsVal<T> = MObservableVal<T, *, *>

sealed interface MObservableVal<T, U: ValueUpdate<T>, L: ValueListener<T, U>>: MListenable<L> {
  val value: T

  fun <R> cast(): MObservableVal<R, *, *> = binding {
	@Suppress("UNCHECKED_CAST")
	it as R
  }

  override fun observe(op: ()->Unit) = onChange { op() }

  fun onChange(op: (T)->Unit): L

  fun onNonNullChange(op: (T & Any)->Unit) = onChange {
	if (it != null) op(it)
  }


  fun onChangeOnce(op: (T)->Unit) = onChange(op).apply {
	removeAfterInvocation = true
  }

  fun onChangeWithWeak(o: Any, op: (T)->Unit) = run {
	val weakRef = WeakRef(o)
	onChange(op).apply {
	  removeCondition = { weakRef.deref() == null }
	}
  }

  fun onChangeUntil(until: (T)->Boolean, op: (T)->Unit) = onChange {
	op(it)
  }.apply {
	this.until = { until(it.new) }
  }

  fun <R> binding(
	vararg dependencies: MObservableVal<*, *, *>,
	op: (T)->R,
  ): MyBinding<R> {
	val b = MyBinding { op(value) }
	observe { b.invalidate() }
	dependencies.forEach { it.observe { b.invalidate() } }
	return b
  }

  fun <R> deepBinding(propGetter: (T)->MObservableVal<R, *, *>) {
	val b = MyBinding<R> {
	  propGetter(value).value
	}
	var lastSubListener = propGetter(value).observe {
	  b.invalidate()
	}
	onChange {
	  lastSubListener.tryRemovingListener()
	  b.invalidate()
	  lastSubListener = propGetter(value).observe {
		b.invalidate()
	  }
	}
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }

}


interface MObservableValNewAndOld<T>: MObservableVal<T, ValueChange<T>, OldAndNewListener<T>> {
  override fun onChange(op: (T)->Unit) = addListener(OldAndNewListener { _, new ->
	op(new)
  })


}

interface MObservableValNewOnly<T>: MObservableVal<T, ValueUpdate<T>, NewListener<T>> {
  override fun onChange(op: (T)->Unit) = addListener(NewListener { new ->
	op(new)
  })
}

//interface NullableVal<T>: MObservableValNewAndOld<T?> {
//  fun onNonNullChange(op: (T & Any)->Unit) = apply {
//	onChange {
//	  if (it != null) op(it)
//	}
//  }
//}


interface FXBackedPropBase {
  val isFXBound: Boolean
}

interface WritableMObservableVal<T>: MObservableValNewAndOld<T>, BindableValue<T> {


  override var value: T

  //  var boundTo: MObservableROPropBase<out T>?

  //  fun cleanBind(other: MObservableROPropBase<out T>) {
  //	unbind()
  //	bind(other)
  //  }

  //  val isBound get() = boundTo != null

  //
  //  fun bind(other: MObservableROPropBase<out T>) {
  //	require(!isBound)
  //	require((this as? FXBackedPropBase)?.isFXBound != true)
  //
  //
  //	val recursiveDeps: List<WritableMObservableVal<*>> = (other as? WritableMObservableVal<*>?)?.chain {
  //	  (it as? WritableMObservableVal<*>)?.boundTo as? WritableMObservableVal<*>
  //	}?.toList() ?: listOf()
  //
  //
  //	require(this !in recursiveDeps)
  //
  //	value = other.value
  //
  //
  //	other.onChange {
  //	  value = it
  //	}
  //
  //	other.addBoundedProp(this)
  //
  //	boundTo = other
  //  }
  //
  //  fun unbind() {
  //	boundTo?.removeBoundedProp(this)
  //	boundTo = null
  //  }
  //
  //  fun unbindBidirectional() {
  //	(boundTo as? WritableMObservableVal<*>)?.unbind()
  //	unbind()
  //  }
  //
  //  fun bindBidirectional(other: WritableMObservableVal<T>) {
  //	this.value = other.value
  //	other.addBoundedProp(this)
  //	addBoundedProp(other)
  //  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
	value = newValue
  }


  override fun <R> cast() = CastedWritableProp<T, R>(this)
}


abstract class MObservableROValBase<T, U: ValueUpdate<T>, L: ValueListener<T, U>>: MObservableImpl<U, L>(),
																				   MObservableVal<T, U, L> {


  infix fun eq(other: ReadOnlyBindableProperty<*>) = binding(other) {
	it == other.value
  }


  infix fun neq(other: ReadOnlyBindableProperty<*>) = eq(other).not()

  infix fun eq(other: Any?) = binding {
	it == other
  }

  infix fun neq(other: Any?) = eq(other).not()

  val isNull by lazy { eq(null) }
  val notNull by lazy { isNull.not() }


  override fun toString() = "[${this::class.simpleName} value=${value.toString()}]"



}


abstract class MObservableROPropBase<T>: MObservableROValBase<T, ValueChange<T>, OldAndNewListener<T>>(),
										 MObservableValNewAndOld<T> {


  fun <R> lazyBinding(
	vararg dependencies: MObservableVal<*, *, *>,
	op: (T)->R,
  ): MyBinding<R> {
	val prop = this
	return MyBinding { op(value) }.apply {
	  prop.onChange {
		invalidate()
	  }
	  dependencies.forEach {
		it.onChange {
		  invalidate()
		}
	  }
	}
  }
}

open class ReadOnlyBindableProperty<T>(value: T): MObservableROPropBase<T>() {

  override var value = value
	protected set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(ValueChange(old, v))
	  }
	}

}

infix fun <T> WritableMObservableVal<T>.v(value: T) {
  this.value = value
}

infix fun <T> WritableMObservableVal<T>.eqNow(value: T): Boolean {
  return this.value == value
}

infix fun <T> WritableMObservableVal<T>.eqNow(value: MObservableROPropBase<T>): Boolean {
  return this.value == value.value
}

infix fun <T> WritableMObservableVal<T>.notEqNow(value: T): Boolean {
  return this.value != value
}

infix fun <T> WritableMObservableVal<T>.notEqNow(value: MObservableROPropBase<T>): Boolean {
  return this.value != value.value
}


open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value), WritableMObservableVal<T>,
										  BindableValue<T> {
  override var value = value
	set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(ValueChange(old, v))
	  }
	}

  final override val bindManager = BindableValueHelper(this)
  override fun bind(source: MObservableVal<T, *, *>) = bindManager.bind(source)
  override fun bindBidirectional(source: WritableMObservableVal<T>) = bindManager.bindBidirectional(source)
  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()
}


typealias ValProp<T> = ReadOnlyBindableProperty<T>
typealias VarProp<T> = BindableProperty<T>

fun bProp(b: Boolean) = BindableProperty(b)
fun sProp(s: String) = BindableProperty(s)
fun iProp(i: Int) = BindableProperty(i)


fun ObsB.whenTrueOnce(op: ()->Unit) {
  if (value) op()
  else {
	onChangeUntil({ it }, {
	  if (it) op()
	})
  }
}


fun VarProp<Boolean>.toggle() {
  value = !value
}
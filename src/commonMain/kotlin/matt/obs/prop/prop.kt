package matt.obs.prop

import matt.lang.weak.WeakRef
import matt.model.convert.Converter
import matt.model.flowlogic.keypass.KeyPass
import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bind.binding
import matt.obs.bindhelp.BindableValue
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.bindings.bool.ObsB
import matt.obs.bindings.bool.not
import matt.obs.listen.Listener
import matt.obs.listen.NewListener
import matt.obs.listen.NewOrLessListener
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

  override fun observe(op: ()->Unit): Listener = onChange { op() }

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


  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }

  infix fun <T> eqNow(value: T) = this.value == value

  infix fun <T> eqNow(value: ObsVal<T>) = this.value == value.value

  infix fun <T> notEqNow(value: T) = this.value != value

  infix fun <T> notEqNow(value: ObsVal<T>) = this.value != value.value


}

fun <T, O: ObsVal<T>> O.withChangeListener(op: (T)->Unit) = apply {
  onChange(op)
}


interface MObservableValNewAndOld<T>: MObservableVal<T, ValueChange<T>, OldAndNewListener<T>> {
  override fun onChange(op: (T)->Unit) = addListener(OldAndNewListener { _, new ->
	op(new)
  })


}

interface MObservableValNewOnly<T>: MObservableVal<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>>> {
  override fun onChange(op: (T)->Unit) = addListener(NewListener { new ->
	op(new)
  })
}


interface FXBackedPropBase {
  val isFXBound: Boolean
}

typealias Var<T> = WritableMObservableVal<T, *, *>

interface WritableMObservableVal<T, U: ValueUpdate<T>, L: ValueListener<T, U>>: MObservableVal<T, U, L>,
																				BindableValue<T> {


  override var value: T


  operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
	value = newValue
  }


  override fun <R> cast() = CastedWritableProp<T, R>(this)


  infix fun v(value: T) {
	this.value = value
  }

  fun readOnly() = binding { it }


  fun takeNonNullChangesFrom(o: ObsVal<out T?>) {
	o.onNonNullChange {
	  value = it!!
	}
  }

  fun takeChangesFrom(o: ObsVal<out T>) {
	o.onChange {
	  value = it
	}
  }

  fun takeChangesFromWhen(o: ObsVal<out T>, predicate: ()->Boolean) {
	o.onChange {
	  if (predicate()) {
		value = it
	  }

	}
  }
}


fun <T, O: Var<T>> O.withNonNullUpdatesFrom(o: ObsVal<out T?>): O = apply {
  takeNonNullChangesFrom(o)
}

fun <T, O: Var<T>> O.withUpdatesFrom(o: ObsVal<out T>): O = apply {
  takeChangesFrom(o)
}

fun <T, O: Var<T>> O.withUpdatesFromWhen(o: ObsVal<out T>, predicate: ()->Boolean): O = apply {
  takeChangesFromWhen(o, predicate)
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
  val isNotNull by lazy { isNull.not() }


  override fun toString() = "[${this::class.simpleName} value=${value.toString()}]"


}

typealias ValProp<T> = ReadOnlyBindableProperty<T>

open class ReadOnlyBindableProperty<T>(value: T): MObservableROValBase<T, ValueChange<T>, OldAndNewListener<T>>(),
												  MObservableValNewAndOld<T> {

  override var value = value
	protected set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(ValueChange(old, v))
	  }
	}

}


typealias VarProp<T> = BindableProperty<T>

open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value),
										  WritableMObservableVal<T, ValueChange<T>, OldAndNewListener<T>>,
										  BindableValue<T> {


  private val bindWritePass = KeyPass()
  override var value = value
	set(v) {
	  require(!this.isBoundUnidirectionally || bindWritePass.isHeld)
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(ValueChange(old, v))
	  }
	}

  internal fun setFromBinding(new: T) {
	bindWritePass.with {
	  value = new
	}
  }

  final override val bindManager by lazy { BindableValueHelper(this) }
  override infix fun bind(source: ObsVal<out T>) = bindManager.bind(source)
  override fun bindBidirectional(source: Var<T>) = bindManager.bindBidirectional(source)
  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) =
	bindManager.bindBidirectional(source, converter)

  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()
}

fun ObsB.whenTrue(op: ()->Unit): Listener {
  if (value) op()
  return onChange {
	if (it) op()
  }
}

fun ObsB.whenFalse(op: ()->Unit): Listener {
  if (!value) op()
  return onChange {
	if (!it) op()
  }
}

fun ObsB.whenTrueOnce(op: ()->Unit) {
  if (value) op()
  else {
	onChangeUntil({ it }, {
	  if (it) op()
	})
  }
}


fun Var<Boolean>.toggle() {
  value = !value
}
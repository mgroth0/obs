package matt.obs.prop

import matt.lang.anno.TemporaryCode
import matt.lang.function.Produce
import matt.lang.model.value.ValueWrapper
import matt.lang.sync.inSync
import matt.lang.sync.inSyncOrJustRun
import matt.lang.weak.WeakRef
import matt.lang.weak.lazySoft
import matt.log.warn.warn
import matt.log.warn.warnOnce
import matt.model.flowlogic.keypass.KeyPass
import matt.model.op.convert.Converter
import matt.model.op.debug.DebugLogger
import matt.model.op.prints.Prints
import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bind.binding
import matt.obs.bindhelp.BindableValue
import matt.obs.bindhelp.BindableValueHelper
import matt.obs.bindings.bool.not
import matt.obs.listen.ChangeListener
import matt.obs.listen.Listener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.NewListener
import matt.obs.listen.NewOrLessListener
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.OldAndNewListenerImpl
import matt.obs.listen.ValueListener
import matt.obs.listen.WeakListenerWithNewValue
import matt.obs.listen.WeakListenerWithOld
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate
import matt.obs.prop.cast.CastedWritableProp
import matt.obs.prop.proxy.ProxyProp
import matt.service.scheduler.Scheduler
import kotlin.jvm.JvmInline
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias ObsVal<T> = MObservableVal<T, *, *>

@JvmInline
value class FakeObsVal<T>(override val value: T): MObservableValNewAndOld<T> {

  override fun addListener(listener: OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>): OldAndNewListener<T, ValueChange<T>, out ValueChange<T>> {
	warnOnce("listening to FakeObsVal")
	return listener
  }

  override var nam: String?
	get() = TODO("Not yet implemented")
	set(_) {
	  TODO("Not yet implemented")
	}

  override fun removeListener(listener: MyListenerInter<*>) {
	TODO("Not yet implemented")
  }

  override var debugger: Prints?
	get() = TODO("Not yet implemented")
	set(_) {
	  TODO("Not yet implemented")
	}

}


sealed interface MObservableVal<T, U: ValueUpdate<T>, L: ValueListener<T, U, out ValueUpdate<T>>>: MListenable<L>,
																								   ValueWrapper<T>,
																								   ReadOnlyProperty<Any?, T> {
  override val value: T

  @Suppress("UNCHECKED_CAST")
  fun <R> cast(): MObservableVal<R, *, *> = binding {
	it as R
  }

  override fun observe(op: ()->Unit): Listener = onChange { op() }

  fun onChange(op: (T)->Unit): L


  fun onNonNullChange(op: (T & Any)->Unit) = onChange {
	if (it != null) op(it)
  }

  fun on(valueCheck: T, op: (T)->Unit) = onChange {
	if (it == valueCheck) op(it)
  }


  fun onChangeOnce(op: (T)->Unit) = onChange(op).apply {
	removeAfterInvocation = true
  }


  fun onChangeUntilInclusive(until: (T)->Boolean, op: (T)->Unit) = onChange {
	op(it)
  }.apply {
	this.untilInclusive = {
	  until(it.new)
	}
  }

  fun onChangeUntilExclusive(until: (T)->Boolean, op: (T)->Unit) = onChange {
	op(it)
  }.apply {
	this.untilExclusive = { until(it.new) }
  }


  override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }


  infix fun <T> eqNow(value: T) = this.value == value

  infix fun <T> eqNow(value: ObsVal<T>) = this.value == value.value

  infix fun <T> notEqNow(value: T) = this.value != value

  infix fun <T> notEqNow(value: ObsVal<T>) = this.value != value.value

  fun <W: Any> onChangeWithWeak(o: W, op: (W, T)->Unit): Listener
  fun <W: Any> onChangeWithAlreadyWeak(weakRef: WeakRef<W>, op: (W, T)->Unit): Listener

}

fun <T, O: ObsVal<T>> O.withChangeListener(op: (T)->Unit) = apply {
  onChange(op)
}


interface MObservableValNewAndOld<T>:
	MObservableVal<T, ValueChange<T>, OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>> {

  override fun onChange(op: (T)->Unit) = addListener(OldAndNewListenerImpl { _, new ->
	op(new)
  })

  fun onChangeWithOld(op: (old: T, new: T)->Unit) = run {
	val listener = OldAndNewListenerImpl { old: T, new: T ->
	  op(old, new)
	}
	addListener(listener)
  }

  override fun <W: Any> onChangeWithWeak(o: W, op: (W, T)->Unit) = run {
	val weakRef = WeakRef(o)
	onChangeWithAlreadyWeak(weakRef, op)
  }

  override fun <W: Any> onChangeWithAlreadyWeak(weakRef: WeakRef<W>, op: (W, T)->Unit) = run {
	val listener = WeakListenerWithOld(weakRef) { o: W, _: T, new: T ->
	  op(o, new)
	}.apply {
	  removeCondition = { weakRef.deref() == null }
	}
	addListener(listener)
  }

  fun <W: Any> onChangeWithWeakAndOld(o: W, op: (W, T, T)->Unit) = run {
	val weakRef = WeakRef(o)
	val listener = WeakListenerWithOld(weakRef) { o: W, old: T, new: T ->
	  op(o, old, new)
	}.apply {
	  removeCondition = { weakRef.deref() == null }
	}
	addListener(listener)
  }

}

interface MObservableValNewOnly<T>:
	MObservableVal<T, ValueUpdate<T>, NewOrLessListener<T, ValueUpdate<T>, out ValueUpdate<T>>> {


  override fun onChange(op: (T)->Unit) = addListener(ChangeListener { new ->
	op(new)
  })

  override fun <W: Any> onChangeWithWeak(o: W, op: (W, T)->Unit) = run {
	val weakRef = WeakRef(o)
	onChangeWithAlreadyWeak(weakRef, op)
  }

  override fun <W: Any> onChangeWithAlreadyWeak(weakRef: WeakRef<W>, op: (W, T)->Unit) = run {
	val listener = WeakListenerWithNewValue(weakRef) { o: W, new: T ->
	  op(o, new)
	}.apply {
	  removeCondition = { weakRef.deref() == null }
	}
	addListener(listener)
  }

}


interface FXBackedPropBase {
  val isFXBound: Boolean
}

typealias Var<T> = WritableMObservableVal<T, *, *>

interface WritableMObservableVal<T, U: ValueUpdate<T>, L: ValueListener<T, U, *>>: MObservableVal<T, U, L>,
																				   BindableValue<T> {


  override var value: T


  operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
	value = newValue
  }


  override fun <R> cast() = CastedWritableProp<T, R>(this)
  fun <R> proxy(converter: Converter<T, R>) = ProxyProp(this, converter)

  fun <R> proxyInv(converter: Converter<R, T>) = ProxyProp(this, converter.invert())


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

abstract class MObservableROValBase<T, U: ValueUpdate<T>, L: ValueListener<T, U, *>>: MObservableImpl<U, L>(),
																					  MObservableVal<T, U, L> {


  infix fun eq(other: ReadOnlyBindableProperty<*>) = binding(other) {
	it == other.value
  }


  infix fun neq(other: ReadOnlyBindableProperty<*>) = eq(other).not()

  infix fun eq(other: Any?) = binding {
	it == other
  }

  infix fun neq(other: Any?) = eq(other).not()


  val isNull by lazySoft {
	eq(null)
  }
  val isNotNull by lazySoft {
	isNull.not()
  }


  override fun toString() = "[${this::class.simpleName} value=${value.toString()}]"


}

typealias ValProp<T> = ReadOnlyBindableProperty<T>

open class ReadOnlyBindableProperty<T>(value: T):
	MObservableROValBase<T, ValueChange<T>, OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>>(),
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


typealias GoodVar<T> = MWritableValNewAndOld<T>

interface MWritableValNewAndOld<T>:
	WritableMObservableVal<T, ValueChange<T>, OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>>,
	MObservableValNewAndOld<T>

class SynchronizedProperty<T>(value: T): BindableProperty<T>(value) {
  @PublishedApi
  internal val monitor = {}
  fun <R> with(op: Produce<R>) = inSync(monitor, op)


  override var value: T
	get() = inSync(monitor) { super.value }
	set(value) {
	  inSync(monitor) {
		super.value = value
	  }
	}
}

open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value),
										  MWritableValNewAndOld<T>,
										  BindableValue<T> {


  val monitorForSetting = object {}

  private val bindWritePass by lazy { KeyPass() }

  fun setIfDifferent(newValue: T) {
	if (value != newValue) {
	  value = newValue
	}
  }

  override var value = value
	set(v) {
	  if (v != field) {
		require(!this.isBoundUnidirectionally || bindWritePass.isHeld) {
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


  override infix fun bind(source: ObsVal<out T>) = bindManager.bind(source)

  /*allows property to still be set by other means*/
  fun pseudoBind(source: ObsVal<out T>) {
	value = source.value
	source.onChange {
	  value = it
	}
  }

  override fun bindBidirectional(source: Var<T>, checkEquality: Boolean, clean: Boolean, debug: Boolean) =
	bindManager.bindBidirectional(source, checkEquality = checkEquality, clean = clean, debug = debug)

  override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) =
	bindManager.bindBidirectional(source, converter)

  override var theBind by bindManager::theBind
  override fun unbind() = bindManager.unbind()
}


fun Var<Boolean>.toggle() {
  value = !value
}

fun <A: Any?> A.toVarProp() = VarProp<A>(this)


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


/**
 * Listen to changes and update the value of the property if the given mutator results in a different value
 */
fun <T> Var<T>.mutateOnChange(mutator: (T)->T) = onChange {
  val changed = mutator(value)
  if (changed != value) value = changed
}
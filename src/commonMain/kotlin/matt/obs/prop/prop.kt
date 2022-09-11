package matt.obs.prop

import matt.lang.B
import matt.obs.MObservable
import matt.obs.MObservableImpl
import matt.obs.bind.binding
import matt.obs.bindings.not
import matt.obs.prop.cast.CastedWritableProp
import matt.stream.recurse.recurse
import kotlin.reflect.KProperty

sealed interface MObservableVal<T>: MObservable<ListenerType<T>, (T)->Boolean> {
  val value: T
  fun addBoundedProp(p: WritableMObservableVal<in T>)
  fun removeBoundedProp(p: WritableMObservableVal<in T>)
  fun onChange(op: (T)->Unit) = onChange(NewListener { op(it) })
  fun <R> cast() = binding {
	@Suppress("UNCHECKED_CAST")
	it as R
  }
}

interface NullableVal<T>: MObservableVal<T?> {
  fun onNonNullChange(op: (T)->Unit) = apply {
	onChange {
	  if (it != null) op(it)
	}
  }
}



interface WritableMObservableVal<T>: MObservableVal<T> {

  override var value: T

  var boundTo: MObservableROValBase<out T>?

  fun bind(other: MObservableROValBase<out T>) {
	require(boundTo == null)

	val recursiveDeps: List<WritableMObservableVal<*>> = (other as? BindableProperty<*>?)?.recurse {
	  it.boundedProps.filterIsInstance<BindableProperty<*>>()
	}?.toList() ?: listOf()


	require(this !in recursiveDeps)

	this.value = other.value
	boundTo = other
	other.addBoundedProp(this)
  }

  fun unbind() {
	boundTo?.removeBoundedProp(this)
	boundTo = null
  }

  fun unbindBidirectional() {
	(boundTo as? WritableMObservableVal<*>)?.unbind()
	unbind()
  }

  fun bindBidirectional(other: WritableMObservableVal<T>) {
	this.value = other.value
	other.addBoundedProp(this)
	addBoundedProp(other)
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
	value = newValue
  }


  override fun <R> cast() = CastedWritableProp<T, R>(this)
}


sealed interface ListenerType<T>
fun interface NewListener<T>: ListenerType<T> {
  fun invoke(new: T)
}

fun interface OldAndNewListener<T>: ListenerType<T> {
  fun invoke(old: T, new: T)
}

abstract class MObservableROValBase<T>: MObservableImpl<ListenerType<T>, (T)->Boolean>(),

										MObservableVal<T> {

  protected fun notifyListeners(old: T, new: T) {
	/*gotta make a new list to prevent concurrent mod error if listeners list is edited in a listener*/
	listeners.toList().forEach {
	  it.invokeWith(old, new)
	}
  }

  fun ListenerType<T>.invokeWith(old: T, new: T) = when (this) {
	is NewListener<T>       -> invoke(new)
	is OldAndNewListener<T> -> invoke(old, new)
  }


  final override fun onChangeSimple(listener: ()->Unit) {
	onChange(NewListener {
	  listener()
	})
  }

  final override fun onChangeUntil(until: (T)->Boolean, listener: ListenerType<T>) {
	var realListener: ListenerType<T>? = null
	realListener = OldAndNewListener { old, t: T ->
	  listener.invokeWith(old, t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }

  final override fun onChangeOnce(listener: ListenerType<T>) = onChangeUntil({ true }, listener)


  internal val boundedProps = mutableSetOf<WritableMObservableVal<in T>>()

  override fun removeListener(listener: ListenerType<T>) = listeners.remove(listener)


  override fun addBoundedProp(p: WritableMObservableVal<in T>) {
	boundedProps += p
  }

  override fun removeBoundedProp(p: WritableMObservableVal<in T>) {
	boundedProps -= p
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }

  override fun toString() = "[${this::class.simpleName} value=${value.toString()}]"

  init {
	onChangeSimple {
	  val v = value
	  boundedProps.forEach {
		if (it.value != v) it.value = v
	  }
	}
  }


  val isNull by lazy {
	binding {
	  it == null
	}
  }


  infix fun eq(other: ReadOnlyBindableProperty<*>) = binding(other) {
	it == other.value
  }

  infix fun neq(other: ReadOnlyBindableProperty<*>) = eq(other).not()

  infix fun eq(other: Any) = binding {
	it == other
  }

  infix fun neq(other: Any) = eq(other).not()


}

open class ReadOnlyBindableProperty<T>(value: T): MObservableROValBase<T>() {

  override var value = value
	protected set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(old, v)
	  }
	}

}

infix fun <T> BindableProperty<T>.v(value: T) {
  this.value = value
}

infix fun <T> BindableProperty<T>.eqNow(value: T): Boolean {
  return this.value == value
}

infix fun <T> BindableProperty<T>.eqNow(value: MObservableROValBase<T>): Boolean {
  return this.value == value.value
}

infix fun <T> BindableProperty<T>.notEqNow(value: T): Boolean {
  return this.value != value
}

infix fun <T> BindableProperty<T>.notEqNow(value: MObservableROValBase<T>): Boolean {
  return this.value != value.value
}



open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value), WritableMObservableVal<T> {
  override var boundTo: MObservableROValBase<out T>? = null
  override var value = value
	set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(old, v)
	  }
	}


}


typealias ValProp<T> = ReadOnlyBindableProperty<T>
typealias VarProp<T> = BindableProperty<T>

fun bProp(b: Boolean) = BindableProperty(b)
fun sProp(s: String) = BindableProperty(s)
fun iProp(i: Int) = BindableProperty(i)


fun ValProp<B>.whenTrueOnce(op: ()->Unit) {
  if (value) op()
  else {
	onChangeUntil({ it }, NewListener {
	  if (it) op()
	})
  }
}


fun VarProp<Boolean>.toggle() {
  value = !value
}
package matt.obs.prop

import matt.lang.B
import matt.lang.weak.WeakRef
import matt.obs.MObservable
import matt.obs.MObservableImpl
import matt.obs.bind.binding
import matt.obs.bindings.not
import matt.obs.prop.cast.CastedWritableProp
import matt.obs.prop.listen.ListenerType
import matt.obs.prop.listen.NewListener
import matt.obs.prop.listen.OldAndNewListener
import matt.stream.recurse.recurse
import kotlin.reflect.KProperty

sealed interface MObservableVal<T, L: ListenerType<T>>: MObservable<L, (T)->Boolean> {
  val value: T
  fun addBoundedProp(p: WritableMObservableVal<in T>)
  fun removeBoundedProp(p: WritableMObservableVal<in T>)
  fun <R> cast() = binding {
	@Suppress("UNCHECKED_CAST")
	it as R
  }

  fun onChange(listener: (T)->Unit): NewListener<T>


}


interface MObservableValNewAndOld<T>: MObservable<ListenerType<T>, (T)->Boolean>,
									  MObservableVal<T, ListenerType<T>> { //  fun onChange(op: (T)->Unit) = onChange(matt.obs.prop.listen.NewListener { op(it) })
  override fun onChange(listener: (T)->Unit) = onChange(NewListener { listener(it) }) as NewListener<T>

  fun onChangeWithWeak(o: Any, op: (T)->Unit) = apply {
	var listener: NewListener<T>? = null
	val weakRef = WeakRef(o)
	listener = NewListener { new ->
	  if (weakRef.deref() == null) {
		removeListener(listener!!)
	  }
	  op(new)
	}
	onChange(listener)
  }
}

interface MObservableValNewOnly<T>: MObservable<NewListener<T>, (T)->Boolean>,
									MObservableVal<T, NewListener<T>> { //  fun onChange(op: (T)->Unit) = onChange(matt.obs.prop.listen.NewListener { op(it) })
  override fun onChange(listener: (T)->Unit) = onChange(NewListener { listener(it) })
} //
//@Suppress("UNCHECKED_CAST")
//inline fun <reified T, reified L: matt.obs.prop.listen.ListenerType<T>, P: MObservableVal<T, L>> P.onChange(listener: matt.obs.prop.listen.NewListener<T>) =
//  when (this) {
//	is MObservableValNewAndOld<*> -> (this as MObservableValNewAndOld<T>).onChange(listener)
//	is MObservableValNewOnly<*>   -> (this as MObservableValNewOnly<T>).onChange(listener)
//	else                          -> NEVER
//  }

interface NullableVal<T>: MObservableValNewAndOld<T?> {
  fun onNonNullChange(op: (T)->Unit) = apply {
	onChange {
	  if (it != null) op(it)
	}
  }
}


interface WritableMObservableVal<T>: MObservableValNewAndOld<T> {


  override var value: T

  var boundTo: MObservableROPropBase<out T>?

  fun cleanBind(other: MObservableROPropBase<out T>) {
	unbind()
	bind(other)
  }
  fun bind(other: MObservableROPropBase<out T>) {
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


abstract class MObservableROValBase<T, L: ListenerType<T>>: MObservableImpl<L, (T)->Boolean>(),


															MObservableVal<T, L> {
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

  override fun toString() = "[${this::class.simpleName} value=${value.toString()}]"

  internal val boundedProps = mutableSetOf<WritableMObservableVal<in T>>()

  override fun addBoundedProp(p: WritableMObservableVal<in T>) {
	boundedProps += p
  }

  override fun removeBoundedProp(p: WritableMObservableVal<in T>) {
	boundedProps -= p
  }

  final override fun onChangeSimple(listener: ()->Unit) {
	onChange { listener() }
  }

  init {
	onChangeSimple {
	  val v = value
	  boundedProps.forEach {
		if (it.value != v) it.value = v
	  }
	}
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
	return value
  }

  final override fun onChangeOnce(listener: L) = onChangeUntil({ true }, listener)


}


abstract class MObservableROPropBase<T>: MObservableROValBase<T, ListenerType<T>>(), MObservableValNewAndOld<T> {

  protected fun notifyListeners(old: T, new: T) {
	/*gotta make a new list to prevent concurrent mod error if listeners list is edited in a listener*/
	listeners.toList().forEach {
	  it.invokeWith(old, new)
	}
  }


  final override fun onChangeUntil(until: (T)->Boolean, listener: ListenerType<T>) {
	var realListener: ListenerType<T>? = null
	realListener = OldAndNewListener { old, t: T ->
	  listener.invokeWith(old, t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }


}

open class ReadOnlyBindableProperty<T>(value: T): MObservableROPropBase<T>() {

  override var value = value
	protected set(v) {
	  if (v != field) {
		val old = v
		field = v
		notifyListeners(old, v)
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


open class BindableProperty<T>(value: T): ReadOnlyBindableProperty<T>(value), WritableMObservableVal<T> {
  override var boundTo: MObservableROPropBase<out T>? = null
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
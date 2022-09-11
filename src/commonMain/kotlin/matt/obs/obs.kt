package matt.obs

import matt.lang.reflect.classForName
import matt.lang.setAll
import matt.lang.weak.WeakRef
import matt.lang.weak.getValue
import matt.log.warn
import matt.obs.bind.binding
import matt.obs.bindings.not
import matt.obs.col.CollectionChange
import matt.obs.col.mirror
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.filtered.BasicFilteredList
import matt.obs.col.olist.sorted.BasicSortedList
import matt.obs.prop.BindableProperty
import matt.obs.prop.ReadOnlyBindableProperty
import matt.obs.prop.VarProp
import matt.obs.prop.cast.CastedWritableProp
import matt.stream.recurse.recurse
import kotlin.jvm.Synchronized
import kotlin.reflect.KProperty

@DslMarker
annotation class ObservableDSL

@ObservableDSL
interface MObsBase {
  fun onChangeSimple(listener: ()->Unit)
}


sealed interface MObservable<L, B>: MObsBase {

  fun onChange(listener: L): L
  fun onChangeUntil(until: B, listener: L)
  fun onChangeOnce(listener: L)
  fun removeListener(listener: L): Boolean

}

sealed interface MObservableObject<T>: MObservable<T.()->Unit, T.()->Boolean> {
  override fun onChangeSimple(listener: ()->Unit) {
	onChange {
	  listener()
	}
  }
}

interface MObservableWithChangeObject<C>: MObservable<(C)->Unit, (C)->Boolean> {
  override fun onChangeSimple(listener: ()->Unit) {
	onChange {
	  listener()
	}
  }
}

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


interface MObsHolder: MObsBase {
  val props: List<MObsBase>
  override fun onChangeSimple(listener: ()->Unit) {
	props.forEach { it.onChangeSimple { listener() } }
  }
}


interface NullableVal<T>: MObservableVal<T?> {
  fun onNonNullChange(op: (T)->Unit) = apply {
	onChange {
	  if (it != null) op(it)
	}
  }
}

@ObservableDSL
sealed class MObservableImpl<L, B>: MObservable<L, B> {
  internal val listeners = mutableListOf<L>()

  final override fun onChange(listener: L): L {
	listeners.add(listener)
	return listener
  }

  override fun removeListener(listener: L) = listeners.remove(listener)
}


abstract class MObservableObjectImpl<T: MObservableObjectImpl<T>> internal constructor():
  MObservableImpl<T.()->Unit, T.()->Boolean>(),
  MObservableObject<T> {
  final override fun onChangeUntil(until: T.()->Boolean, listener: T.()->Unit) {
	var realListener: (T.()->Unit)? = null
	realListener = {
	  listener()
	  if (until()) listeners -= realListener!!
	}
	listeners += realListener
  }

  final override fun onChangeOnce(listener: T.()->Unit) = onChangeUntil({ true }, listener)

  protected fun emitChange() {
	listeners.forEach {
	  @Suppress("UNCHECKED_CAST")
	  it.invoke(this as T)
	}
  }
}

abstract class InternalBackedMObservableWithChangeObject<C> internal constructor():
  MObservableImpl<(C)->Unit, (C)->Boolean>(),
  MObservableWithChangeObject<C> {
  final override fun onChangeUntil(until: (C)->Boolean, listener: (C)->Unit) {
	var realListener: ((C)->Unit)? = null
	realListener = { t: C ->
	  listener(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }

  final override fun onChangeOnce(listener: (C)->Unit) = onChangeUntil({ true }, listener)

  protected fun emitChange(change: C) {
	listeners.forEach { it(change) }
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

interface BasicOCollection<E>: Collection<E>, MObservableWithChangeObject<CollectionChange<E>>

interface BasicROObservableList<E>: BasicOCollection<E> {
  fun filtered(filter: (E)->Boolean) = BasicFilteredList(this, filter)
}

fun <E: Comparable<E>> BasicROObservableList<E>.sorted() = BasicSortedList(this)
sealed interface BasicWritableObservableList<E>: MutableList<E>, BasicROObservableList<E>

abstract class BaseBasicWritableOList<E>: BasicROObservableList<E>,
										  BasicWritableObservableList<E> {

  val isEmptyProp by lazy {
	VarProp(this.isEmpty()).apply {
	  onChange {
		value = this@BaseBasicWritableOList.isEmpty()
	  }
	}
  }

  private var theBind: TheBind<*>? = null

  @Synchronized
  fun unbind() {
	theBind?.cut()
	theBind = null
  }

  @Synchronized
  fun <S> bind(source: BasicObservableListImpl<S>, converter: (S)->E) {
	unbind()
	setAll(source.map(converter))
	val listener = source.onChange {
	  mirror(it, converter)
	}
	theBind = TheBind(source = source, listener = listener)
  }
}

private class TheBind<S>(
  source: BasicObservableListImpl<S>,
  private val listener: (CollectionChange<S>)->Unit
) {
  val source by WeakRef(source)
  fun cut() = source?.removeListener(listener)
}


internal val JAVAFX_OBSERVABLE_CLASS by lazy {
  classForName("javafx.beans.Observable") ?: run {
	warn("observableClass is null")
	null
  }
}
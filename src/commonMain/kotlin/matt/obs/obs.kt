package matt.obs

import matt.obs.col.CollectionChange
import matt.obs.prop.BindableProperty
import matt.obs.prop.ReadOnlyBindableProperty
import matt.stream.recurse.recurse
import kotlin.reflect.KProperty

@DslMarker
annotation class ObservableDSL

interface MObsBase {
  fun onChangeSimple(listener: ()->Unit)
}

@ObservableDSL
sealed interface MObservable<L, B>: MObsBase {

  fun onChange(listener: L): L
  fun onChangeUntil(until: B, listener: L)
  fun onChangeOnce(listener: L)

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

sealed interface MObservableVal<T>: MObservable<(T)->Unit, (T)->Boolean> {
  fun addBoundedProp(p: WritableMObservableVal<in T>)
  fun removeBoundedProp(p: WritableMObservableVal<in T>)
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

abstract class MObservableWithChangeObjectImpl<C> internal constructor(): MObservableImpl<(C)->Unit, (C)->Boolean>(),
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

  var value: T

  var boundTo: ReadOnlyBindableProperty<out T>?

  fun bind(other: ReadOnlyBindableProperty<out T>) {
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

  protected fun notifyListeners(old: T, new: T) = listeners.forEach { it(v) }


  abstract val value: T

  final override fun onChangeSimple(listener: ()->Unit) {
	onChange {
	  listener()
	}
  }

  final override fun onChangeUntil(until: (T)->Boolean, listener: ListenerType<T>) {
	var realListener: ((T)->Unit)? = null
	realListener = { t: T ->
	  listener(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }

  final override fun onChangeOnce(listener: T.()->Unit) = onChangeUntil({ true }, listener)


  internal val boundedProps = mutableSetOf<WritableMObservableVal<in T>>()

  fun removeListener(listener: ListenerType<T>) {
	listeners -= listener
  }

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
	onChange { v ->
	  boundedProps.forEach {
		if (it.value != v) it.value = v
	  }
	}
  }
}


interface BasicObservableList<E>: List<E>, MObservableWithChangeObject<CollectionChange<E>>

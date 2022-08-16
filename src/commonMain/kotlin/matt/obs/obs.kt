package matt.obs

import matt.obs.prop.ReadOnlyBindableProperty
import kotlin.reflect.KProperty

@DslMarker
annotation class ObservableDSL

@ObservableDSL
sealed interface MObservable<L, B> {
  fun onChange(listener: L): L
  fun onChangeUntil(until: B, listener: L)
  fun onChangeOnce(listener: L)
}

sealed interface MObservableObject<T>: MObservable<T.()->Unit, T.()->Boolean>
sealed interface MObservableWithChangeObject<C>: MObservable<(C)->Unit, (C)->Boolean>
sealed interface MObservableVal<T>: MObservable<(T)->Unit, (T)->Boolean> {
  fun addBoundedProp(p: WritableMObservableVal<in T>)
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

  fun bind(other: ReadOnlyBindableProperty<out T>) {
	this.value = other.value
	other.addBoundedProp(this)
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

abstract class MObservableROValBase<T>: MObservableImpl<(T)->Unit, (T)->Boolean>(),

										MObservableVal<T> {

  protected fun notifyListeners(v: T) = listeners.forEach { it(v) }

  abstract val value: T

  final override fun onChangeUntil(until: (T)->Boolean, listener: (T)->Unit) {
	var realListener: ((T)->Unit)? = null
	realListener = { t: T ->
	  listener(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }

  final override fun onChangeOnce(listener: T.()->Unit) = onChangeUntil({ true }, listener)


  private val boundedProps = mutableSetOf<WritableMObservableVal<in T>>()

  fun removeListener(listener: (T)->Unit) {
	listeners -= listener
  }

  override fun addBoundedProp(p: WritableMObservableVal<in T>) {
	boundedProps += p
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
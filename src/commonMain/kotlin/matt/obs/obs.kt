package matt.obs

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
sealed interface MObservableVal<T>: MObservable<(T)->Unit, (T)->Boolean>

@ObservableDSL
sealed class MObservableImpl<L, B>: MObservable<L, B> {
  internal val listeners = mutableListOf<L>()
  final override fun onChange(listener: L): L {
	listeners.add(listener)
	return listener
  }
}



abstract class MObservableObjectImpl<T: MObservableObjectImpl<T>> internal constructor(): MObservableImpl<T.()->Unit, T.()->Boolean>(),
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

abstract class MObservableROValImpl<T> internal constructor(value: T): MObservableImpl<(T)->Unit, (T)->Boolean>(),
																	   MObservableVal<T> {
  open var value = value
	protected set(v) {
	  if (v != field) {
		field = v
		listeners.forEach { it(v) }
	  }
	}

  final override fun onChangeUntil(until: (T)->Boolean, listener: (T)->Unit) {
	var realListener: ((T)->Unit)? = null
	realListener = { t: T ->
	  listener(t)
	  if (until(t)) listeners -= realListener!!
	}
	listeners += realListener
  }

  final override fun onChangeOnce(listener: T.()->Unit) = onChangeUntil({ true }, listener)
}

abstract class MObservableVarImpl<T> internal constructor(value: T): MObservableROValImpl<T>(value) {
  final override var value = value
	public set(v) {
	  super.value = v
	  if (v != field) {
		field = v
		listeners.forEach { it(v) }
	  }
	}
}
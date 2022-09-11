package matt.obs

import matt.lang.go
import matt.lang.reflect.classForName
import matt.lang.reflect.isSubTypeOf
import matt.log.warn
import kotlin.contracts.ExperimentalContracts

@DslMarker annotation class ObservableDSL

@ObservableDSL interface MObsBase {
  fun onChangeSimple(listener: ()->Unit)
}

interface MObservable<L, B>: MObsBase {

  fun onChange(listener: L): L
  fun onChangeUntil(until: B, listener: L)
  fun onChangeOnce(listener: L)
  fun removeListener(listener: L): Boolean

}

abstract class MObservableImpl<L, B> internal constructor(): MObservable<L, B> {
  internal val listeners = mutableListOf<L>()

  final override fun onChange(listener: L): L {
	listeners.add(listener)
	return listener
  }

  override fun removeListener(listener: L) = listeners.remove(listener)
}


internal val JAVAFX_OBSERVABLE_CLASS by lazy {
  classForName("javafx.beans.Observable") ?: run {
	warn("observableClass is null")
	null
  }
}

@OptIn(ExperimentalContracts::class)
internal fun <E> Collection<E>.requireNotObservable() = apply {
  JAVAFX_OBSERVABLE_CLASS?.go {
	require(!this::class.isSubTypeOf(it)) {
	  "this is the wrong way to make a BasicObservableList from an ObservableList if you want them to be synced"
	}
  }
}
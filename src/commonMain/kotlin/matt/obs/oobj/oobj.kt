package matt.obs.oobj

import matt.obs.MObservable
import matt.obs.MObservableImpl



interface MObservableObject<T>: MObservable<T.()->Unit, T.()->Boolean> {
  override fun onChangeSimple(listener: ()->Unit) {
	onChange {
	  listener()
	}
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

open class ObservableObject<T: ObservableObject<T>>: MObservableObjectImpl<T>()
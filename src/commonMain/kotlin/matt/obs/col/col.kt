package matt.obs.col

import matt.obs.MObservable
import matt.obs.MObservableImpl
import matt.obs.col.change.CollectionChange
import matt.obs.prop.VarProp

interface MObservableWithChangeObject<C>: MObservable<(C)->Unit, (C)->Boolean> {
  override fun onChangeSimple(listener: ()->Unit) {
	onChange {
	  listener()
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

interface BasicOCollection<E>: Collection<E>, MObservableWithChangeObject<CollectionChange<E>>

abstract class ObservableCollectionImpl<E>: InternalBackedMObservableWithChangeObject<CollectionChange<E>>(), BasicOCollection<E> {
  val isEmptyProp by lazy {
	VarProp(this.isEmpty()).apply {
	  onChange {
		value = this@ObservableCollectionImpl.isEmpty()
	  }
	}
  }
}



interface BasicOMutableCollection<E>: BasicOCollection<E>, MutableCollection<E>
package matt.obs.col

import matt.obs.MObservable
import matt.obs.MObservableImpl
import matt.obs.col.change.CollectionChange
import matt.obs.listen.CollectionListener
import matt.obs.listen.update.CollectionUpdate
import matt.obs.prop.MObservableVal
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp

interface BasicOCollection<E>: Collection<E>, MObservable<CollectionListener<E>> {
  fun onChange(op: (CollectionChange<E>)->Unit): CollectionListener<E>
}

abstract class InternallyBackedOCollection<E> internal constructor():
  MObservableImpl<CollectionUpdate<E>, CollectionListener<E>>(), BasicOCollection<E> {

  override fun onChange(op: (CollectionChange<E>)->Unit): CollectionListener<E> {
	return addListener(CollectionListener {
	  op(it)
	})
  }

  protected fun emitChange(change: CollectionChange<E>) {
	notifyListeners(CollectionUpdate((change)))
  }

  val isEmptyProp: ValProp<Boolean> by lazy {
	VarProp(this.isEmpty()).apply {
	  onChange {
		value = this@InternallyBackedOCollection.isEmpty()
	  }
	}
  }


  inline fun <C, R> binding(
	vararg dependencies: MObservableVal<*, *, *>,
	crossinline op: (BasicOCollection<E>)->R,
  ): ValProp<R> {
	val prop = this
	return VarProp(op(prop)).apply {
	  prop.onChange {
		value = op(prop)
	  }
	  dependencies.forEach {
		it.onChange {
		  value = op(prop)
		}
	  }
	}
  }
}

interface BasicOMutableCollection<E>: BasicOCollection<E>, MutableCollection<E>
package matt.obs.col

import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bind.MyBinding
import matt.obs.col.change.CollectionChange
import matt.obs.listen.CollectionListener
import matt.obs.listen.update.CollectionUpdate
import matt.obs.prop.MObservableVal
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp

interface BasicOCollection<E>: Collection<E>, MListenable<CollectionListener<E>> {
  override fun observe(op: ()->Unit) = onChange { op() }
  fun onChange(op: (CollectionChange<E>)->Unit): CollectionListener<E>
  fun <R> binding(
	vararg dependencies: MObservableVal<*, *, *>,
	op: (BasicOCollection<E>)->R,
  ): MyBinding<R> {
	val b = MyBinding { op(this) }
	observe { b.invalidate() }
	dependencies.forEach { it.observe { b.invalidate() } }
	return b
  }
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

}

interface BasicOMutableCollection<E>: BasicOCollection<E>, MutableCollection<E>
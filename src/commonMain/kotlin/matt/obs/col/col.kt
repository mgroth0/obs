package matt.obs.col

import matt.model.keypass.KeyPass
import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bindhelp.BindableList
import matt.obs.col.change.CollectionChange
import matt.obs.listen.CollectionListener
import matt.obs.listen.update.CollectionUpdate
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp

interface BasicOCollection<E>: Collection<E>, MListenable<CollectionListener<E>> {
  override fun observe(op: ()->Unit) = onChange { op() }
  fun onChange(op: (CollectionChange<E>)->Unit): CollectionListener<E>

}

abstract class InternallyBackedOCollection<E> internal constructor():
  MObservableImpl<CollectionUpdate<E>, CollectionListener<E>>(), BasicOCollection<E> {

  override fun onChange(op: (CollectionChange<E>)->Unit): CollectionListener<E> {
	return addListener(CollectionListener {
	  op(it)
	})
  }

  internal val bindWritePass = KeyPass()
  protected fun emitChange(change: CollectionChange<E>) {
	require (this !is BindableList<*> || !this.isBound || bindWritePass.isHeld)
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
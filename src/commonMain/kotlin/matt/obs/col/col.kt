package matt.obs.col

import matt.model.flowlogic.keypass.KeyPass
import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bindhelp.BindableList
import matt.obs.col.change.CollectionChange
import matt.obs.listen.CollectionListener
import matt.obs.listen.CollectionListenerBase
import matt.obs.listen.update.CollectionUpdate
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp

interface BasicOCollection<E, C: CollectionChange<E, out Collection<E>>>: Collection<E>, MListenable<CollectionListenerBase<E,C>> {
  override fun observe(op: ()->Unit) = onChange { op() }
  fun onChange(listenerName: String? = null, op: (C)->Unit): CollectionListenerBase<E,C>
}

abstract class InternallyBackedOCollection<E, C: CollectionChange<E,Collection<E>>> internal constructor(): MObservableImpl<CollectionUpdate<E>, CollectionListenerBase<E,*>>(), BasicOCollection<E,C> {

  override fun onChange(listenerName: String?, op: (CollectionChange<E>)->Unit): CollectionListenerBase<E,C> {
	return addListener(CollectionListener {    //	  println("addListener c = $it")
	  op(it)
	}.also {
	  if (listenerName != null) it.name = listenerName
	})
  }

  internal val bindWritePass = KeyPass()
  protected fun emitChange(change: CollectionChange<E>) {
	require(this !is BindableList<*> || !this.isBound || bindWritePass.isHeld)
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



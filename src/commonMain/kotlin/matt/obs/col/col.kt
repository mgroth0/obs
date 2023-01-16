package matt.obs.col

import matt.model.flowlogic.keypass.KeyPass
import matt.obs.MListenable
import matt.obs.MObservableImpl
import matt.obs.bindhelp.BindableList
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.SetChange
import matt.obs.listen.CollectionListener
import matt.obs.listen.CollectionListenerBase
import matt.obs.listen.ListListener
import matt.obs.listen.SetListener
import matt.obs.listen.update.CollectionUpdate
import matt.obs.listen.update.ListUpdate
import matt.obs.listen.update.SetUpdate
import matt.obs.prop.ValProp
import matt.obs.prop.VarProp


interface BasicOCollection<E, C: CollectionChange<E, out Collection<E>>, U: CollectionUpdate<E, C>, L: CollectionListenerBase<E, C, U>>:
	Collection<E>,
	MListenable<L> {
  override fun observe(op: ()->Unit) = onChange { op() }
  fun onChange(listenerName: String? = null, op: (C)->Unit): L
}


typealias IBObsCol = InternallyBackedOCollection<*, *, *, *>

sealed class InternallyBackedOCollection<E, C: CollectionChange<E, out Collection<E>>, U: CollectionUpdate<E, C>, L: CollectionListenerBase<E, C, U>>:
	MObservableImpl<U, L>(),
	BasicOCollection<E, C, U, L> {


  override fun onChange(listenerName: String?, op: (C)->Unit): L {
	val l = createListener {
	  //	  println("addListener c = $it")
	  op(it)
	}
	return addListener(l.also {
	  if (listenerName != null) it.name = listenerName
	})
  }

  protected abstract fun createListener(invoke: CollectionListener<E, C, U>.(change: C)->Unit): L

  internal val bindWritePass = KeyPass()

  protected fun emitChange(change: C) {
	require(this !is BindableList<*> || !this.isBound || bindWritePass.isHeld)
	notifyListeners(updateFrom((change)))
  }

  protected abstract fun updateFrom(c: C): U

  val isEmptyProp: ValProp<Boolean> by lazy {
	VarProp(this.isEmpty()).apply {
	  onChange {
		value = this@InternallyBackedOCollection.isEmpty()
	  }
	}
  }
}

abstract class InternallyBackedOSet<E> internal constructor(): InternallyBackedOCollection<E, SetChange<E>, SetUpdate<E>, SetListener<E>>() {

  override fun updateFrom(c: SetChange<E>): SetUpdate<E> {
	return SetUpdate(c)
  }

  override fun createListener(invoke: CollectionListener<E, SetChange<E>, SetUpdate<E>>.(change: SetChange<E>)->Unit): SetListener<E> {
	val l = SetListener<E>(invoke)
	return l
  }
}

abstract class InternallyBackedOList<E> internal constructor(): InternallyBackedOCollection<E, ListChange<E>, ListUpdate<E>, ListListener<E>>() {
  override fun updateFrom(c: ListChange<E>): ListUpdate<E> {
	return ListUpdate(c)
  }

  override fun createListener(invoke: CollectionListener<E, ListChange<E>, ListUpdate<E>>.(change: ListChange<E>)->Unit): ListListener<E> {
	val l = ListListener<E>(invoke)
	return l
  }
}

interface BasicOMutableCollection<E, C: CollectionChange<E, out Collection<E>>, U: CollectionUpdate<E, C>>:
	BasicOCollection<E, C, U, CollectionListenerBase<E, C, U>>,
	MutableCollection<E>



package matt.obs.listen

import matt.lang.NEVER
import matt.lang.ifTrue
import matt.obs.MListenable
import matt.obs.col.change.CollectionChange
import matt.obs.listen.update.CollectionUpdate
import matt.obs.listen.update.ContextUpdate
import matt.obs.listen.update.ObsHolderUpdate
import matt.obs.listen.update.Update
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate

@DslMarker
annotation class ListenerDSL

@ListenerDSL
sealed class MyListener<U: Update> {

  var removeCondition: (()->Boolean)? = null
  var removeAfterInvocation: Boolean = false

  internal var currentObservable: MListenable<*>? = null
  internal fun removeListener() = currentObservable!!.removeListener(this)
  internal fun tryRemovingListener() = currentObservable?.removeListener(this) ?: false


  internal fun preInvocation(): Boolean {
	var r = true
	removeCondition?.invoke()?.ifTrue {
	  removeListener()
	  r = false
	}
	return r
  }

  abstract fun notify(update: U)

  internal fun postInvocation() {
	if (removeAfterInvocation) {
	  removeListener()
	} else removeCondition?.invoke()?.ifTrue {
	  removeListener()
	}
  }
}

internal fun <U, L: MyListener<U>> L.moveTo(o: MListenable<L>) {
  tryRemovingListener()
  o.addListener(this)
}


sealed class ValueListener<T, U: ValueUpdate<T>>: MyListener<U>() {
  var until: ((U)->Boolean)? = null
  final override fun notify(update: U) {
	subNotify(update)
	until?.invoke(update)?.ifTrue { removeListener() }
  }

  abstract fun subNotify(update: U)
}

class NewListener<T>(private val invoke: NewListener<T>.(new: T)->Unit):
  ValueListener<T, ValueUpdate<T>>() {
  override fun subNotify(update: ValueUpdate<T>) = invoke(update.new)
}

class OldAndNewListener<T>(internal val invoke: OldAndNewListener<T>.(old: T, new: T)->Unit):
  ValueListener<T, ValueChange<T>>() {
  override fun subNotify(update: ValueChange<T>) = invoke(update.old, update.new)
}

class CollectionListener<E>(internal val invoke: CollectionListener<E>.(change: CollectionChange<E>)->Unit):
  MyListener<CollectionUpdate<E>>() {
  override fun notify(update: CollectionUpdate<E>) = invoke(update.change)
}

class ContextListener<C>(private val obj: C, private val invocation: C.()->Unit): MyListener<ContextUpdate>() {
  final override fun notify(update: ContextUpdate) {
	obj.invocation()
  }
}

class ObsHolderListener: MyListener<ObsHolderUpdate>() {
  internal val subListeners = mutableListOf<MyListener<*>>()
  final override fun notify(update: ObsHolderUpdate) = NEVER
}
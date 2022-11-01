package matt.obs.listen

import matt.lang.NEVER
import matt.lang.ifTrue
import matt.lang.weak.WeakRef
import matt.model.prints.Prints
import matt.model.tostringbuilder.toStringBuilder
import matt.obs.MListenable
import matt.obs.col.change.CollectionChange
import matt.obs.listen.update.CollectionUpdate
import matt.obs.listen.update.ContextUpdate
import matt.obs.listen.update.MapUpdate
import matt.obs.listen.update.ObsHolderUpdate
import matt.obs.listen.update.Update
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate
import matt.obs.map.change.MapChange

@DslMarker annotation class ListenerDSL

typealias Listener = MyListener<*>


@ListenerDSL sealed class MyListener<U: Update> {

  var name: String? = null

  override fun toString() = toStringBuilder("name" to name)

  var removeCondition: (()->Boolean)? = null
  var removeAfterInvocation: Boolean = false

  internal var currentObservable: WeakRef<MListenable<*>>? = null
  fun removeListener() = currentObservable!!.deref()!!.removeListener(this)
  fun tryRemovingListener() = currentObservable?.deref()?.removeListener(this) ?: false


  internal fun preInvocation(): Boolean {
	var r = true
	removeCondition?.invoke()?.ifTrue {
	  removeListener()
	  r = false
	}
	return r
  }

  abstract fun notify(update: U, debugger: Prints? = null)

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
  var untilInclusive: ((U)->Boolean)? = null
  var untilExclusive: ((U)->Boolean)? = null
  final override fun notify(update: U, debugger: Prints?) {
	debugger?.println("ValueListener.notify 1")
	untilExclusive?.invoke(update)?.ifTrue {
	  removeListener()
	  return
	}
	debugger?.println("ValueListener.notify 2")
	subNotify(update, debugger)
	debugger?.println("ValueListener.notify 3")
	untilInclusive?.invoke(update)?.ifTrue { removeListener() }
	debugger?.println("ValueListener.notify 4")
  }

  abstract fun subNotify(update: U, debugger: Prints? = null)
}

sealed class NewOrLessListener<T, U: ValueUpdate<T>>: ValueListener<T, U>()

class InvalidListener<T>(private val invoke: InvalidListener<T>.()->Unit): NewOrLessListener<T, ValueUpdate<T>>() {
  var listenerDebugger: Prints? = null
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) {
	listenerDebugger = debugger
	debugger?.println("subNotify 1")
	invoke()
	debugger?.println("subNotify 2")
	listenerDebugger = null
  }
}

class NewListener<T>(private val invoke: NewListener<T>.(new: T)->Unit): NewOrLessListener<T, ValueUpdate<T>>() {
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) = invoke(update.new)
}

class OldAndNewListener<T>(internal val invoke: OldAndNewListener<T>.(old: T, new: T)->Unit):
  ValueListener<T, ValueChange<T>>() {
  override fun subNotify(update: ValueChange<T>, debugger: Prints?) = invoke(update.old, update.new)
}

class CollectionListener<E>(internal val invoke: CollectionListener<E>.(change: CollectionChange<E>)->Unit):
  MyListener<CollectionUpdate<E>>() {
  override fun notify(update: CollectionUpdate<E>, debugger: Prints?) = invoke(update.change)
}

class MapListener<K, V>(internal val invoke: MapListener<K, V>.(change: MapChange<K, V>)->Unit):
  MyListener<MapUpdate<K, V>>() {
  override fun notify(update: MapUpdate<K, V>, debugger: Prints?) = invoke(update.change)
}

class ContextListener<C>(private val obj: C, private val invocation: C.()->Unit): MyListener<ContextUpdate>() {
  final override fun notify(update: ContextUpdate, debugger: Prints?) {
	obj.invocation()
  }
}

class ObsHolderListener: MyListener<ObsHolderUpdate>() {
  internal val subListeners = mutableListOf<MyListener<*>>()
  override fun notify(update: ObsHolderUpdate, debugger: Prints?) = NEVER
}
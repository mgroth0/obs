package matt.obs.listen

import matt.lang.NEVER
import matt.lang.ifTrue
import matt.lang.model.value.Value
import matt.lang.weak.WeakRef
import matt.model.obj.tostringbuilder.toStringBuilder
import matt.model.op.prints.Prints
import matt.obs.MListenable
import matt.obs.col.change.CollectionChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.SetChange
import matt.obs.listen.update.CollectionUpdate
import matt.obs.listen.update.ContextUpdate
import matt.obs.listen.update.MapUpdate
import matt.obs.listen.update.ObsHolderUpdate
import matt.obs.listen.update.Update
import matt.obs.listen.update.ValueChange
import matt.obs.listen.update.ValueUpdate
import matt.obs.listen.update.ValueUpdateWithWeakObj
import matt.obs.listen.update.ValueUpdateWithWeakObjAndOld
import matt.obs.map.change.MapChange
import matt.obs.prop.ObsVal
import kotlin.jvm.Synchronized

@DslMarker annotation class ListenerDSL

typealias Listener = MyListener<*>

interface MyListenerInter

@ListenerDSL abstract class MyListener<U: Update>: MyListenerInter {

  var name: String? = null

  override fun toString() = toStringBuilder("name" to name)

  var removeCondition: (()->Boolean)? = null
  var removeAfterInvocation: Boolean = false

  internal var currentObservable: WeakRef<MListenable<*>>? = null
  fun removeListener() = currentObservable!!.deref()!!.removeListener(this)
  fun tryRemovingListener() = currentObservable?.deref()?.removeListener(this) ?: false


  internal fun preInvocation(update: U): U? {
	removeCondition?.invoke()?.ifTrue {
	  removeListener()
	  return null
	}
	return update
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

class InvalidListener<T>(private val invoke: InvalidListener<T>.()->Unit):
	NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>>() {
  var listenerDebugger: Prints? = null
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) {
	listenerDebugger = debugger
	invoke()
	listenerDebugger = null
  }

  override fun transformUpdate(u: ValueUpdate<T>) = u
}

sealed class ValueListener<T, U_IN: ValueUpdate<T>, U_OUT: ValueUpdate<T>>: MyListener<U_IN>() {
  var untilInclusive: ((U_OUT)->Boolean)? = null
  var untilExclusive: ((U_OUT)->Boolean)? = null
  protected abstract fun transformUpdate(u: U_IN): U_OUT?


  final override fun notify(update: U_IN, debugger: Prints?) {

	//	val ss = stackSize()
	//	println("ss=$ss")
	//	if (ss > 1000) {
	//	  error("here?")
	//	}

	val u = transformUpdate(update)
	if (u == null) removeListener()
	else {
	  untilExclusive?.invoke(u)?.ifTrue {
		removeListener()
		return
	  }
	  subNotify(u, debugger)
	  untilInclusive?.invoke(u)?.ifTrue { removeListener() }
	}
  }


  abstract fun subNotify(update: U_OUT, debugger: Prints? = null)
}


sealed class NewOrLessListener<T, U_IN: ValueUpdate<T>, U_OUT: ValueUpdate<T>>: ValueListener<T, U_IN, U_OUT>()

class NewListener<T>(private val invoke: NewListener<T>.(new: T)->Unit):
	NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>>() {
  override fun transformUpdate(u: ValueUpdate<T>) = u
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) = invoke(update.new)
}


class ChangeListener<T>(private val invoke: ChangeListener<T>.(new: T)->Unit):
	NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>>() {

  private var lastUpdate: Value<T>? = null

  override fun transformUpdate(u: ValueUpdate<T>) = u

  @Synchronized
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) {
	val new = update.new
	val last = lastUpdate
	if (last == null || last.value != new) {
	  lastUpdate = Value(new)
	  invoke(update.new)
	}
  }
}


interface MyWeakListener: MyListenerInter {
  fun shouldBeCleaned(): Boolean
}

class WeakCollectionListener<W: Any, E>(
  private val wref: WeakRef<W>,
  private val invoke: MyListener<*>.(ref: W, change: CollectionChange<E>)->Unit
): CollectionListenerBase<E>(), MyWeakListener {

  override fun subNotify(change: CollectionChange<E>) {
	val w = wref.deref()
	if (w == null) removeListener()
	else invoke(this, w, change)
  }

  override fun shouldBeCleaned() = wref.deref() == null

}


class WeakListenerWithNewValue<W: Any, T>(
  private val wref: WeakRef<W>,
  internal val invoke: WeakListenerWithNewValue<W, T>.(ref: W, new: T)->Unit
): NewOrLessListener<T, ValueUpdate<T>, ValueUpdateWithWeakObj<W, T>>(), MyWeakListener {

  override fun shouldBeCleaned() = wref.deref() == null

  override fun transformUpdate(u: ValueUpdate<T>): ValueUpdateWithWeakObj<W, T>? {
	return wref.deref()?.let {
	  ValueUpdateWithWeakObj(u.new, it)
	}
  }

  override fun subNotify(update: ValueUpdateWithWeakObj<W, T>, debugger: Prints?) {
	invoke(this, update.weakObj, update.new)
  }

}

typealias OldNewListener<T> = OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>

abstract class OldAndNewListener<T, U_IN: ValueChange<T>, U_OUT: ValueChange<T>>: ValueListener<T, U_IN, U_OUT>()

class WeakListenerWithOld<W: Any, T>(
  private val wref: WeakRef<W>, internal val invoke: WeakListenerWithOld<W, T>.(ref: W, old: T, new: T)->Unit
): OldAndNewListener<T, ValueChange<T>, ValueUpdateWithWeakObjAndOld<W, T>>(), MyWeakListener {

  override fun shouldBeCleaned() = wref.deref() == null

  override fun transformUpdate(u: ValueChange<T>): ValueUpdateWithWeakObjAndOld<W, T>? {
	return wref.deref()?.let {
	  ValueUpdateWithWeakObjAndOld(new = u.new, old = u.old, weakObj = it)
	}
  }

  override fun subNotify(update: ValueUpdateWithWeakObjAndOld<W, T>, debugger: Prints?) {
	invoke(this, update.weakObj, update.old, update.new)
  }

}

class OldAndNewListenerImpl<T>(internal val invoke: OldAndNewListenerImpl<T>.(old: T, new: T)->Unit):
	OldAndNewListener<T, ValueChange<T>, ValueChange<T>>() {
  override fun transformUpdate(u: ValueChange<T>) = u
  override fun subNotify(update: ValueChange<T>, debugger: Prints?) = invoke(update.old, update.new)
}

abstract class CollectionListenerBase<E,C: CollectionChange<E, out Collection<E>>>(): MyListener<CollectionUpdate<E>>() {
  final override fun notify(update: CollectionUpdate<E>, debugger: Prints?) = subNotify(update.change)
  abstract fun subNotify(change: C)
}

sealed class CollectionListener<E, C: CollectionChange<E,out Collection<E>>>(internal val invoke: CollectionListener<E,C>.(change: C)->Unit): CollectionListenerBase<E,C>() {
  override fun subNotify(change: C) = invoke(change)
}
class SetListener<E>(invoke: CollectionListener<E,SetChange<E>>.(change: SetChange<E>)->Unit): CollectionListener<E,SetChange<E>>(invoke)
class ListListener<E>(invoke: CollectionListener<E,ListChange<E>>.(change: ListChange<E>)->Unit): CollectionListener<E,ListChange<E>>(invoke)


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


fun <T> ObsVal<T>.whenEqualsOnce(t: T, op: ()->Unit) {
  if (value == t) op()
  else {
	onChangeUntilInclusive({ it == t }, {
	  if (it == t) op()
	})
  }
}


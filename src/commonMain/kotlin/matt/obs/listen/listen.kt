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
import matt.obs.listen.update.ListUpdate
import matt.obs.listen.update.MapUpdate
import matt.obs.listen.update.ObsHolderUpdate
import matt.obs.listen.update.SetUpdate
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

interface MyListenerInter<U: Update> {
  fun notify(update: U, debugger: Prints? = null)
  fun removeListener()
  fun tryRemovingListener()
  var removeAfterInvocation: Boolean
  var name: String?
}

@ListenerDSL abstract class MyListener<U: Update>: MyListenerInter<U> {

  override var name: String? = null

  override fun toString() = toStringBuilder("name" to name)

  var removeCondition: (()->Boolean)? = null
  override var removeAfterInvocation: Boolean = false

  internal var currentObservable: WeakRef<MListenable<*>>? = null
  override fun removeListener() = currentObservable!!.deref()!!.removeListener(this)
  override fun tryRemovingListener() {
	currentObservable?.deref()?.removeListener(this)
  }


  internal fun preInvocation(update: U): U? {
	removeCondition?.invoke()?.ifTrue {
	  removeListener()
	  return null
	}
	return update
  }

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
	NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>>, ValueListener<T, ValueUpdate<T>, ValueUpdate<T>>() {
  var listenerDebugger: Prints? = null
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) {
	listenerDebugger = debugger
	invoke()
	listenerDebugger = null
  }

  override fun transformUpdate(u: ValueUpdate<T>) = u
}

sealed interface ValueListenerBase<T, U: ValueUpdate<T>>: MyListenerInter<U>

sealed class ValueListener<T, U_IN: ValueUpdate<T>, U_OUT: ValueUpdate<T>>: MyListener<U_IN>(),
																			ValueListenerBase<T, U_IN> {
  var untilInclusive: ((U_OUT)->Boolean)? = null
  var untilExclusive: ((U_OUT)->Boolean)? = null
  protected abstract fun transformUpdate(u: U_IN): U_OUT?
  protected open fun shouldRemove() = false
  final override fun notify(update: U_IN, debugger: Prints?) {
	val u = transformUpdate(update)
	if (shouldRemove()) removeListener()
	else if (u == null) removeListener()
	else {
	  if (shouldNotify(u)) {
		untilExclusive?.invoke(u)?.ifTrue {
		  removeListener()
		  return
		}
		subNotify(u, debugger)
		untilInclusive?.invoke(u)?.ifTrue { removeListener() }
	  }
	}
  }

  open fun shouldNotify(u: U_OUT) = true
  abstract fun subNotify(update: U_OUT, debugger: Prints? = null)
}


sealed interface NewOrLessListener<T, U_IN: ValueUpdate<T>, U_OUT: ValueUpdate<T>>: ValueListenerBase<T, U_IN>

class NewListener<T>(private val invoke: NewListener<T>.(new: T)->Unit):
	ValueListener<T, ValueUpdate<T>, ValueUpdate<T>>(), NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>> {
  override fun transformUpdate(u: ValueUpdate<T>) = u
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) = invoke(update.new)
}


class ChangeListener<T>(private val invoke: ChangeListener<T>.(new: T)->Unit):
	ValueListener<T, ValueUpdate<T>, ValueUpdate<T>>(), NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>> {

  private var lastUpdate: Value<T>? = null

  override fun transformUpdate(u: ValueUpdate<T>) = u

  override fun shouldNotify(u: ValueUpdate<T>): Boolean {
	val new = u.new
	val last = lastUpdate
	if (last == null || last.value != new) {
	  lastUpdate = Value(new)
	  return true
	}
	return false
  }

  @Synchronized
  override fun subNotify(update: ValueUpdate<T>, debugger: Prints?) = invoke(update.new)
}

interface MyWeakListener<U: Update>: MyListenerInter<U> {
  fun shouldBeCleaned(): Boolean
}

abstract class WeakListenerBase<W: Any, U: Update>(): MyListener<U>(), MyWeakListener<U> {
  protected abstract val wref: WeakRef<W>
  final override fun shouldBeCleaned() = wref.deref() == null
}

sealed class WeakValueListenerBase<W: Any, T>: ValueListener<T, ValueUpdate<T>, ValueUpdateWithWeakObj<W, T>>(),
											   MyWeakListener<ValueUpdate<T>> {
  protected abstract val wref: WeakRef<W>
  final override fun shouldBeCleaned() = wref.deref() == null
  override fun shouldRemove(): Boolean {
	return shouldBeCleaned()
  }
}

sealed class WeakChangeListenerBase<W: Any, T>: ValueListener<T, ValueUpdate<T>, ValueUpdateWithWeakObj<W, T>>(),
												MyWeakListener<ValueUpdate<T>> {
  protected abstract val wref: WeakRef<W>
  private var lastUpdate: Value<T>? = null
  override fun shouldNotify(u: ValueUpdateWithWeakObj<W, T>): Boolean {
	val new = u.new
	val last = lastUpdate
	if (last == null || last.value != new) {
	  lastUpdate = Value(new)
	  return true
	}
	return false
  }

  final override fun shouldBeCleaned() = wref.deref() == null
  override fun shouldRemove(): Boolean {
	return shouldBeCleaned()
  }
}

abstract class WeakCollectionListener<W: Any, E, C: CollectionChange<E, out Collection<E>>, U: CollectionUpdate<E, C>>(
  override val wref: WeakRef<W>,
  private val invoke: MyListenerInter<*>.(ref: W, change: C)->Unit
): WeakListenerBase<W, U>(), CollectionListenerBase<E, C, U> {

  override fun subNotify(change: C) {
	val w = wref.deref()
	if (w == null) removeListener()
	else invoke(this, w, change)
  }

}

class WeakListListener<W: Any, E>(
  wref: WeakRef<W>,
  invoke: MyListenerInter<*>.(ref: W, change: ListChange<E>)->Unit
): WeakCollectionListener<W, E, ListChange<E>, ListUpdate<E>>(wref, invoke), ListListenerBase<E>

class WeakSetListener<W: Any, E>(
  wref: WeakRef<W>,
  invoke: MyListenerInter<*>.(ref: W, change: SetChange<E>)->Unit
): WeakCollectionListener<W, E, SetChange<E>, SetUpdate<E>>(wref, invoke)


class WeakChangeListenerWithNewValue<W: Any, T>(
  override val wref: WeakRef<W>,
  internal val invoke: WeakChangeListenerWithNewValue<W, T>.(ref: W, new: T)->Unit
): WeakChangeListenerBase<W, T>(), NewOrLessListener<T, ValueUpdate<T>, ValueUpdate<T>> {

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
): OldAndNewListener<T, ValueChange<T>, ValueUpdateWithWeakObjAndOld<W, T>>(), MyWeakListener<ValueChange<T>> {

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

interface CollectionListenerBase<E, C: CollectionChange<E, out Collection<E>>, U: CollectionUpdate<E, C>>:
	MyListenerInter<U> {
  override fun notify(update: U, debugger: Prints?) = subNotify(update.change)
  abstract fun subNotify(change: C)
}

abstract class CollectionListener<E, C: CollectionChange<E, out Collection<E>>, U: CollectionUpdate<E, C>>(
  internal val invoke: CollectionListener<E, C, U>.(change: C)->Unit
): MyListener<U>(), CollectionListenerBase<E, C, U> {
  override fun subNotify(change: C) = invoke(change)
}

class SetListener<E>(invoke: CollectionListener<E, SetChange<E>, SetUpdate<E>>.(change: SetChange<E>)->Unit):
	CollectionListener<E, SetChange<E>, SetUpdate<E>>(
	  invoke
	)


interface ListListenerBase<E>: CollectionListenerBase<E, ListChange<E>, ListUpdate<E>>
class ListListener<E>(invoke: CollectionListener<E, ListChange<E>, ListUpdate<E>>.(change: ListChange<E>)->Unit):
	CollectionListener<E, ListChange<E>, ListUpdate<E>>(
	  invoke
	), ListListenerBase<E>


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
  internal val subListeners = mutableListOf<MyListenerInter<*>>()
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


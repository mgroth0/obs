package matt.obs.prop.listen

import matt.lang.ifTrue
import matt.obs.MObservable

sealed class MyListener {

  var removeCondition: (()->Boolean)? = null
  var removeAfterInvocation: Boolean = false

  internal var currentObservable: MObservable<*>? = null
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

  internal fun postInvocation() {
	if (removeAfterInvocation) {
	  removeListener()
	} else removeCondition?.invoke()?.ifTrue {
	  removeListener()
	}
  }
}

internal fun <L: MyListener> L.moveTo(o: MObservable<L>) {
  tryRemovingListener()
  o.addListener(this)
}


sealed class ValueListener<T>: MyListener() {
  fun invokeWith(old: T, new: T) = when (this) {
	is NewListener<T>       -> invoke(new)
	is OldAndNewListener<T> -> invoke(old, new)
  }
}

class NewListener<T>(internal val invoke: NewListener<T>.(new: T)->Unit):
  ValueListener<T>()

class OldAndNewListener<T>(internal val invoke: OldAndNewListener<T>.(old: T, new: T)->Unit):
  ValueListener<T>()
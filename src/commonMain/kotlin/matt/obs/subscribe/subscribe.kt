package matt.obs.subscribe

import matt.lang.function.Consume
import matt.lang.function.Op
import kotlin.jvm.Synchronized

class Channel<U> {
  fun post(u: U) {
	val removeSafeCopy = subscribers.toList()
	removeSafeCopy.forEach {
	  it.update(u)
	}
  }

  fun subscribe(): Subscription<U> {
	val sub = Subscription(this)
	subscribers += sub
	return sub
  }

  internal val subscribers = mutableListOf<Subscription<U>>()
}

class Subscription<U>(
  private val channel: Channel<U>,
) {
  internal val notifications = mutableListOf<U>()

  @Synchronized
  internal fun update(u: U) {
	notifications += u
	val removeSafeCopy = listeners.toList()
	removeSafeCopy.forEach {
	  it(u)
	}
  }

  private val listeners = mutableListOf<Consume<U>>()

  fun unsubscribe() {
	channel.subscribers.remove(this)
  }

  @Synchronized
  fun whenItHasAtLeastOneNotification(op: Op) {
	if (notifications.size >= 1) {
	  op()
	} else {
	  var listener: Consume<U>? = null
	  listener = {
		op()
		listeners.remove(listener!!)
	  }
	  listeners += listener
	}
  }

}
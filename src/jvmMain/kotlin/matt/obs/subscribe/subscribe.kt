package matt.obs.subscribe

import matt.model.flowlogic.latch.SimpleLatch
import matt.obs.listen.event.Subscription

fun Subscription<*>.waitForThereToBeAtLeastOneNotificationThenUnsubscribe() {
  val gate = SimpleLatch()
  whenItHasAtLeastOneNotification {
	unsubscribe()
	gate.open()
  }
  gate.await()
}
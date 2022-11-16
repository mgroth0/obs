package matt.obs.subscribe

import matt.model.latch.SimpleLatch

fun Subscription<*>.waitForThereToBeAtLeastOneNotificationThenUnsubscribe() {
  val gate = SimpleLatch()
  whenItHasAtLeastOneNotification {
	unsubscribe()
	gate.open()
  }
  gate.await()
}
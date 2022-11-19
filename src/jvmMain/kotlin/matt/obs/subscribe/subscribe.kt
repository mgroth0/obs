package matt.obs.subscribe

import matt.model.flowlogic.latch.SimpleLatch

fun Subscription<*>.waitForThereToBeAtLeastOneNotificationThenUnsubscribe() {
  val gate = SimpleLatch()
  whenItHasAtLeastOneNotification {
	unsubscribe()
	gate.open()
  }
  gate.await()
}
package matt.obs.watch

import matt.obs.col.olist.ObsList
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.time.dur.sleep
import kotlin.concurrent.thread
import kotlin.time.Duration


fun <T> watchProp(checkInterval: Duration, op: ()->T): ObsVal<T> {
  val prop = BindableProperty(op())
  thread(isDaemon = true) {
	while (true) {
	  prop.value = op()
	  sleep(checkInterval)
	}
  }
  return prop
}

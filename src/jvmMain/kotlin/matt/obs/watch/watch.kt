package matt.obs.watch

import matt.lang.function.Op
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.obs.prop.Var
import matt.time.dur.sleep
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


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

fun <T: Any> ObsVal<T>.watchedFrom(watcher: PropertyWatcher) = watcher.watch(this)

abstract class PropertyWatcher() {

  abstract fun runOps(ops: List<Op>)

  private val watches = mutableListOf<PropertyWatch<*>>()

  var refreshInterval = 1.seconds

  fun <T: Any> watch(watching: ObsVal<T>): ObsVal<T> {

	val watch = PropertyWatch(watching, BindableProperty(watching.value), watching.value)
	watches += watch
	watching.onChange {
	  synchronized(this) {
		watch.update = it
	  }
	}
	return watch.updating.readOnly()
  }

  init {
	thread(isDaemon = true) {
	  while (true) {
		val updates = synchronized(this@PropertyWatcher) {
		  watches.mapNotNull {

			it.prepareUpdateOp()
		  }
		}
		runOps(updates)
		sleep(refreshInterval)
	  }
	}
  }

  private class PropertyWatch<T: Any>(val watching: ObsVal<T>, val updating: Var<T>, var update: T? = null) {
	fun prepareUpdateOp(): Op? = update?.let {
	  {
		updating.value = it
	  }
	}?.also {
	  update = null
	}
  }
}


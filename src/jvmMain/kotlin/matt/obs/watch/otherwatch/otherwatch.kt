package matt.obs.watch.otherwatch

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import matt.lang.function.Op
import matt.lang.service.ThreadProvider
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.obs.prop.Var
import matt.time.dur.sleep
import kotlin.time.Duration.Companion.seconds


fun <T : Any> ObsVal<T>.watchedFrom(watcher: PropertyWatcher) = watcher.watch(this)

@OptIn(InternalCoroutinesApi::class)
abstract class PropertyWatcher(threadProvider: ThreadProvider) {

    abstract fun runOps(ops: List<Op>)

    private val watches = mutableListOf<PropertyWatch<*>>()

    var refreshInterval = 1.seconds

    fun <T : Any> watch(watching: ObsVal<T>): ObsVal<T> {

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
        threadProvider.namedThread(isDaemon = true, name = "PropertyWatcher Thread") {
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
}


private class PropertyWatch<T : Any>(

    val watching: ObsVal<T>,
    val updating: Var<T>,
    var update: T? = null
) {
    fun prepareUpdateOp(): Op? = update?.let {
        {
            updating.value = it
        }
    }?.also {
        update = null
    }
}
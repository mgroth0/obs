package matt.obs.watch

import matt.async.thread.namedThread
import matt.lang.function.Op
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.obs.prop.Var
import matt.time.UnixTime
import matt.time.dur.sleep
import java.util.concurrent.Semaphore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


fun <T> watchProp(
    checkInterval: Duration,
    op: () -> T
): ObsVal<T> {
    val prop = BindableProperty(op())
    /*  thread(isDaemon = true) {
        while (true) {
          prop.value = op()
          sleep(checkInterval)
        }
      }*/
    Watcher.add(
        prop,
        checkInterval,
        op
    )
    return prop
}


/*this is so old and broken...*/
private object Watcher {
    private val watchProps = mutableListOf<WatchProp<*>>()

    private class WatchProp<T>(
        val prop: BindableProperty<T>,
        val interval: Duration,
        val updateOp: () -> T,
    ) {
        var lastUpdate: UnixTime? = null
            set(value) {
                field = value
                nextUpdate = lazy { (lastUpdate ?: UnixTime.EPOCH) + interval }
            }
        var nextUpdate = lazy { (lastUpdate ?: UnixTime.EPOCH) + interval }
            private set

        fun update() {
            prop.value = updateOp()
        }
    }

    private val interruptSem = Semaphore(1)
    fun <T> add(
        prop: BindableProperty<T>,
        interval: Duration,
        updateOp: () -> T
    ) {
        synchronized(watchProps) {
            watchProps.add(
                WatchProp(
                    prop = prop,
                    interval = interval,
                    updateOp = updateOp
                )
            )
            sort()
        }
        interruptSem.acquire()
        myThread.interrupt()
    }

    private fun sort() {
        watchProps.sortBy { it.nextUpdate.value }
    }

    private val myThread = namedThread(name = "Watcher", isDaemon = true) {
        while (true) {
            try {
                if (interruptSem.availablePermits() == 0) {
                    interruptSem.release()
                }
                val next = synchronized(watchProps) {
                    watchProps.first()
                }
                val nextUpdate = next.nextUpdate.value
                while (UnixTime() < nextUpdate) {
                    sleep(nextUpdate - UnixTime()) /*should only happen once*/
                }
                next.update()
                next.lastUpdate = UnixTime()
                synchronized(watchProps) {
                    sort()
                }
            } catch (e: InterruptedException) {
                /*do nothing*/
            }
        }
    }
}

fun <T : Any> ObsVal<T>.watchedFrom(watcher: PropertyWatcher) = watcher.watch(this)

abstract class PropertyWatcher() {

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
        namedThread(isDaemon = true, name = "PropertyWatcher Thread") {
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
}


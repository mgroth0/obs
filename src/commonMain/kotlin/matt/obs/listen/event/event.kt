package matt.obs.listen.event

import matt.lang.common.go
import matt.lang.function.Op
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.inSync
import matt.model.op.prints.Prints
import matt.obs.common.MObservable
import matt.obs.listen.MyListener
import matt.obs.listen.update.Event
import matt.obs.subscribe.Channel

abstract class MyEventListener<E : Event> : MyListener<E>()

class BasicEventListener<E : Event>(private val op: BasicEventListener<E>.(E) -> Unit) : MyEventListener<E>() {
    override fun notify(
        update: E,
        debugger: Prints?
    ) {
        op(update)
    }
}

class Curator<E : Event, CE : E, L : MyEventListener<in CE>>(
    private val filter: (E) -> CE?,
    private val channel: Channel<CE> = Channel()
) : MyEventListener<E>(), MObservable by channel {


    override fun notify(
        update: E,
        debugger: Prints?
    ) {
        filter(update)?.go {
            channel.post(it)
        }
    }
}


class Subscription<E : Event>(
    private val channel: Channel<E> = Channel()
) : MyEventListener<E>(), ReferenceMonitor {
    private val notifications = mutableListOf<E>()
    fun unsubscribe() = removeListener()

    fun whenItHasAtLeastOneNotification(op: Op) =
        inSync {
            if (notifications.size >= 1) op()
            else channel.addListener(
                BasicEventListener {
                    op()
                    removeListener()
                }
            )
        }

    override fun notify(
        update: E,
        debugger: Prints?
    ) {
        notifications += update
        channel.broadcast(update)
    }
}

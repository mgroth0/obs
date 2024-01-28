package matt.obs.subscribe

import matt.lang.function.Consume
import matt.lang.weak.WeakRefInter
import matt.obs.MObservableImpl
import matt.obs.listen.MyListenerInter
import matt.obs.listen.event.BasicEventListener
import matt.obs.listen.event.MyEventListener
import matt.obs.listen.event.Subscription
import matt.obs.listen.update.Beep
import matt.obs.listen.update.Event
import matt.obs.listen.update.PagerMessage

open class Channel<E : Event> : MObservableImpl<E, MyEventListener<in E>>() {

    fun post(u: E) = notifyListeners(u)
    fun broadcast(e: E) = post(e)


    fun subscribe() = addListener(Subscription()) as Subscription

    final override fun observe(op: () -> Unit) = addListener(BasicEventListener { op() })
    final override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*> {
        TODO()
    }

}


class Beeper : Channel<Beep>() {
    fun beep() = broadcast(Beep)
}

class Pager<M> : Channel<PagerMessage<M>>() {
    fun page(message: M) = broadcast(PagerMessage(message))
    fun listen(op: Consume<M>) = addListener(BasicEventListener {
        op(it.message)
    })
}
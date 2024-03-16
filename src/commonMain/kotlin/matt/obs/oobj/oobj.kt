package matt.obs.oobj

import matt.lang.anno.Open
import matt.lang.weak.common.WeakRefInter
import matt.obs.common.MListenable
import matt.obs.common.MObservableImpl
import matt.obs.invalid.CustomInvalidations
import matt.obs.listen.ContextListener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.update.ContextUpdate


interface MObservableObject<T> : MListenable<ContextListener<T>>, CustomInvalidations {

    fun cast(o: Any?): T

    @Open
    val noLongerUncheckedThis get() = cast(this)

    @Open
    override fun observe(op: () -> Unit) = onChange { op() }

    @Open
    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*> {
        TODO()
    }
    @Open
    fun onChange(op: T.() -> Unit) =
        addListener(
            ContextListener(noLongerUncheckedThis) {
                op()
            }
        )
}

abstract class ObservableObject<T : ObservableObject<T>> :
    MObservableImpl<ContextUpdate, ContextListener<T>>(),
    MObservableObject<T> {

    final override fun markInvalid() {
        notifyListeners(ContextUpdate)
    }
}

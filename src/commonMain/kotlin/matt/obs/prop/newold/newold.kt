package matt.obs.prop.newold

import matt.lang.anno.Open
import matt.lang.weak.common.WeakRefInter
import matt.lang.weak.weak
import matt.obs.listen.OldAndNewListener
import matt.obs.listen.OldAndNewListenerImpl
import matt.obs.listen.WeakListenerWithOld
import matt.obs.listen.update.ValueChange
import matt.obs.prop.MObservableVal


interface MObservableValNewAndOld<T> :
    MObservableVal<T, ValueChange<T>, OldAndNewListener<T, ValueChange<T>, out ValueChange<T>>> {






    @Open
    fun onChangeWithOld(op: (old: T, new: T) -> Unit) =
        run {
            val listener =
                OldAndNewListenerImpl { old: T, new: T ->
                    op(old, new)
                }
            addListener(listener)
        }




    @Open
    fun <W : Any> onChangeWithAlreadyWeakAndOld(
        weakRef: WeakRefInter<W>,
        op: (W, o: T, n: T) -> Unit
    ) = run {
        val listener =
            WeakListenerWithOld(weakRef) { o: W, old: T, new: T ->
                op(o, old, new)
            }.apply {
                removeCondition = { weakRef.deref() == null }
            }
        addListener(listener)
    }

    @Open
    fun <W : Any> onChangeWithWeakAndOld(
        o: W,
        op: (W, T, T) -> Unit
    ) = run {
        val weakRef = weak(o)
        val listener =
            WeakListenerWithOld(weakRef) { o: W, old: T, new: T ->
                op(o, old, new)
            }.apply {
                removeCondition = { weakRef.deref() == null }
            }
        addListener(listener)
    }
}

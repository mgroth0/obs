package matt.obs

import matt.lang.weak.MyWeakRef
import matt.lang.weak.onGarbageCollected
import matt.obs.listen.MyWeakListener

actual fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean {
    val weakObj = wl.wref.deref() ?: return false

    val wlRef = MyWeakRef(wl)
    weakObj.onGarbageCollected(Unit) {
        wlRef.deref()?.tryRemovingListener()
    }


    return true
}

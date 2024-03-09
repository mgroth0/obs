package matt.obs

import matt.lang.weak.js.onGarbageCollected
import matt.lang.weak.weak
import matt.obs.listen.MyWeakListener

actual fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean {
    val weakObj = wl.wref.deref() ?: return false

    val wlRef = weak(wl)
    weakObj.onGarbageCollected(Unit) {
        wlRef.deref()?.tryRemovingListener()
    }


    return true
}

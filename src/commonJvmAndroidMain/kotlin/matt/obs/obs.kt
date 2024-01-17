package matt.obs

import matt.async.thread.TheThreadProvider
import matt.lang.weak.JvmDump
import matt.lang.weak.MyWeakRef
import matt.lang.weak.onGarbageCollected
import matt.obs.listen.MyWeakListener


val TheDump by lazy {
    JvmDump(TheThreadProvider)
}


actual fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean {
    val weakObj = wl.wref.deref() ?: return false
    val wlRef = MyWeakRef(wl)
    weakObj.onGarbageCollected(TheDump) {
        wlRef.deref()?.tryRemovingListener()
    }
    return true
}


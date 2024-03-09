package matt.obs

import matt.async.thread.TheThreadProvider
import matt.lang.weak.j.JvmDump
import matt.lang.weak.j.onGarbageCollected
import matt.lang.weak.weak
import matt.obs.listen.MyWeakListener


private val TheDump by lazy {
    JvmDump(TheThreadProvider)
}


actual fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean {
    val weakObj = wl.wref.deref() ?: return false
    val wlRef = weak(wl)
    weakObj.onGarbageCollected(TheDump) {
        wlRef.deref()?.tryRemovingListener()
    }
    return true
}


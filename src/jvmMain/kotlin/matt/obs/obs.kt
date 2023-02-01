package matt.obs

import matt.lang.weak.MyWeakRef
import matt.lang.weak.onGarbageCollected
import matt.obs.listen.MyWeakListener

actual fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean {
  val weakObj = wl.wref.deref()
  val wlRef = MyWeakRef(wl)
  weakObj?.onGarbageCollected {
//	println("trying to remove listener from ref queue!!!")
	wlRef.deref()?.tryRemovingListener()
  } ?: return false
  return true
}


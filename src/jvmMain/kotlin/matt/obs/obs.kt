package matt.obs

import matt.async.thread.TheThreadProvider
import matt.lang.weak.Dump
import matt.lang.weak.MyWeakRef
import matt.lang.weak.onGarbageCollected
import matt.obs.listen.MyWeakListener

val TheDump by lazy {
  Dump(TheThreadProvider)
}

actual fun maybeRemoveByRefQueue(wl: MyWeakListener<*>): Boolean {
  val weakObj = wl.wref.deref()
  val wlRef = MyWeakRef(wl)
  weakObj?.onGarbageCollected(TheDump) {
//	println("trying to remove listener from ref queue!!!")
	wlRef.deref()?.tryRemovingListener()
  } ?: return false
  return true
}


package matt.obs.hold

import matt.lang.setAll
import matt.obs.MListenable
import matt.obs.MObservable
import matt.obs.listen.MyListener
import matt.obs.listen.ObsHolderListener

interface MObsHolder: MObservable {
  val props: List<MListenable<*>>

  override fun observe(op: ()->Unit) = ObsHolderListener().apply {
	subListeners.setAll(props.map { it.observe(op) })
  }

  override fun removeListener(listener: MyListener<*>): Boolean {
	return (listener as? ObsHolderListener)?.subListeners?.map { it.tryRemovingListener() }?.any { it } ?: false
  }
}


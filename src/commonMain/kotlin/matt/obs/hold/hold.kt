package matt.obs.hold

import matt.obs.MObservable
import matt.obs.listen.ObsHolderListener

interface MObsHolder: MObservable<ObsHolderListener> {
  val props: List<MObservable<*>>

//  override fun onChange(op: ()->Unit) {
//	props.forEach { it.onChange( { op() } }
//  }
}

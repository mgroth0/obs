package matt.obs.hold

import matt.obs.MObsBase

interface MObsHolder: MObsBase {
  val props: List<MObsBase>
  override fun onChangeSimple(listener: ()->Unit) {
	props.forEach { it.onChangeSimple { listener() } }
  }
}

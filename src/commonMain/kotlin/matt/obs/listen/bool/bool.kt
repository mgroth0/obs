package matt.obs.listen.bool

import matt.obs.bindings.bool.ObsB
import matt.obs.listen.Listener

fun ObsB.whenTrue(op: ()->Unit): Listener {
  if (value) op()
  return onChange {
	if (it) op()
  }
}

fun ObsB.whenFalse(op: ()->Unit): Listener {
  if (!value) op()
  return onChange {
	if (!it) op()
  }
}

fun ObsB.whenTrueOnce(op: ()->Unit) {
  if (value) op()
  else {
	onChangeUntilInclusive({ it }, {
	  if (it) op()
	})
  }
}
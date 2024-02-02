package matt.obs.listen.bool

import matt.obs.bindings.bool.ObsB
import matt.obs.listen.awaitThisToBe

fun ObsB.awaitThisToBeTrue() = awaitThisToBe(true)
fun ObsB.awaitThisToBeFalse() = awaitThisToBe(false)

package matt.obs.listen.bool.j

import matt.obs.bindings.bool.ObsB
import matt.obs.listen.j.awaitThisToBe

fun ObsB.awaitThisToBeTrue() = awaitThisToBe(true)
fun ObsB.awaitThisToBeFalse() = awaitThisToBe(false)

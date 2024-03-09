package matt.obs.math.double

import matt.lang.common.D
import matt.obs.math.reduction
import matt.obs.prop.ObsVal

typealias ObsD = ObsVal<D>

fun sum(vararg values: ObsD) =
    reduction(*values) {
        it.sumOf { it.value }
    }

fun mean(vararg values: ObsD) =
    reduction(*values) {
        it.sumOf { it.value } / it.size
    }

fun max(vararg values: ObsD) =
    reduction(*values) {
        it.maxOf { it.value }
    }

fun min(vararg values: ObsD) =
    reduction(*values) {
        it.minOf { it.value }
    }


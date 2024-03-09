package matt.obs.math.int

import matt.lang.common.I
import matt.obs.math.reduction
import matt.obs.prop.ObsVal

typealias ObsI = ObsVal<I>

fun sum(vararg values: ObsI) =
    reduction(*values) {
        it.sumOf { it.value }
    }

fun mean(vararg values: ObsI) =
    reduction(*values) {
        it.sumOf { it.value } / it.size
    }

fun max(vararg values: ObsI) =
    reduction(*values) {
        it.maxOf { it.value }
    }

fun min(vararg values: ObsI) =
    reduction(*values) {
        it.minOf { it.value }
    }

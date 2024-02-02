package matt.obs.math.float

import matt.lang.F
import matt.obs.math.reduction
import matt.obs.prop.ObsVal

typealias ObsF = ObsVal<F>

fun sum(vararg values: ObsF) = reduction(*values) {
    it.sumOf { it.value.toDouble() }.toFloat()
}

fun mean(vararg values: ObsF) = reduction(*values) {
    (it.sumOf { it.value.toDouble() }/it.size).toFloat()
}


fun max(vararg values: ObsF) = reduction(*values) {
    it.maxOf { it.value }
}

fun min(vararg values: ObsF) = reduction(*values) {
    it.minOf { it.value }
}

package matt.obs.math.long

import matt.lang.L
import matt.obs.math.reduction
import matt.obs.prop.ObsVal

typealias ObsL = ObsVal<L>

fun sum(vararg values: ObsL) = reduction(*values) {
  it.sumOf { it.value }
}

fun mean(vararg values: ObsL) = reduction(*values) {
  it.sumOf { it.value }/it.size
}

fun max(vararg values: ObsL) = reduction(*values) {
  it.maxOf { it.value }
}

fun min(vararg values: ObsL) = reduction(*values) {
  it.minOf { it.value }
}
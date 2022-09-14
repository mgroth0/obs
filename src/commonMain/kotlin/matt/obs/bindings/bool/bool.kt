package matt.obs.bindings.bool

import matt.collect.weak.lazyWeakMap
import matt.lang.B
import matt.obs.bind.binding
import matt.obs.prop.ObsVal

typealias ObsB = ObsVal<B>

private val notProps = lazyWeakMap<ObsB, ObsB> { prop ->
  prop.binding {
	!it
  }
}

fun ObsB.not() = notProps[this]


infix fun ObsB.and(other: ObsB) = binding(other) {
  it && other.value
}

infix fun ObsB.and(other: Boolean) = binding {
  it && other
}
infix fun ObsB.or(other: ObsB) = binding(other) {
  it || other.value
}

infix fun ObsB.or(other: Boolean) = binding {
  it || other
}

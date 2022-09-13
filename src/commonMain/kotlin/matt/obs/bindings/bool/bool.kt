package matt.obs.bindings.bool

import matt.collect.weak.lazyWeakMap
import matt.lang.B
import matt.obs.prop.ObsVal

private val notProps = lazyWeakMap<ObsVal<B>, ObsVal<B>> { prop ->
  prop.binding {
	!it
  }
}

fun ObsVal<B>.not() = notProps[this]


infix fun ObsVal<B>.and(other: ObsVal<B>) = binding(other) {
  it && other.value
}

infix fun ObsVal<B>.or(other: ObsVal<B>) = binding(other) {
  it || other.value
}

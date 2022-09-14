package matt.obs.bindings.comp

import matt.obs.bind.binding
import matt.obs.prop.ObsVal




infix fun <T: Comparable<T>> ObsVal<T>.ge(other: ObsVal<T>) = binding(other) {
  it >= other.value
}

infix fun <T: Comparable<T>> ObsVal<T>.le(other: ObsVal<T>) = binding(other) {
  it <= other.value
}

infix fun <T: Comparable<T>> ObsVal<T>.lt(other: ObsVal<T>) = binding(other) {
  it < other.value
}

infix fun <T: Comparable<T>> ObsVal<T>.gt(other: ObsVal<T>) = binding(other) {
  it > other.value
}


infix fun <T: Comparable<T>> ObsVal<T>.ge(other: T) = binding {
  it >= other
}

infix fun <T: Comparable<T>> ObsVal<T>.le(other: T) = binding {
  it <= other
}

infix fun <T: Comparable<T>> ObsVal<T>.lt(other: T) = binding {
  it < other
}

infix fun <T: Comparable<T>> ObsVal<T>.gt(other: T) = binding {
  it > other
}

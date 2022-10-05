package matt.obs.bindings.comp

import matt.obs.bind.binding
import matt.obs.prop.ObsVal

operator fun <T: Comparable<T>> ObsVal<T>.compareTo(other: ObsVal<out T>): Int = value.compareTo(other.value)
operator fun <T: Comparable<T>> ObsVal<T>.compareTo(other: T): Int = value.compareTo(other)

/*not sure whether to use out or in generic here*/
infix fun <T> ObsVal<T>.eq(other: ObsVal<T>) = binding(other) {
  it == other.value
}

/*not sure whether to use out or in generic here*/
infix fun <T> ObsVal<T>.neq(other: ObsVal<T>) = binding(other) {
  it != other.value
}

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


/*not sure whether to use out or in generic here*/
infix fun <T> ObsVal<T>.eq(other: T) = binding {
  it == other
}


/*not sure whether to use out or in generic here*/
infix fun <T> ObsVal<T>.neq(other: T) = binding {
  it != other
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


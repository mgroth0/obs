package matt.obs.math.mathable.op

import matt.model.mathable.Mathable
import matt.obs.bind.binding
import matt.obs.prop.ObsVal
import matt.obs.prop.Var


/*operator fun <T: Mathable<T>> T.times(other: ObsVal<T>): ObsVal<T> = other.binding {
  this.times(it)
}*/

operator fun <T: Mathable<T>> T.minus(other: ObsVal<T>): ObsVal<T> = other.binding {
  this - it
}

operator fun <T: Mathable<T>> T.plus(other: ObsVal<T>): ObsVal<T> = other.binding {
  this + it
}

/*operator fun <T: Mathable<T>> T.div(other: ObsVal<T>): ObsVal<T> = other.binding {
  this/it
}*/


/*operator fun <T: Mathable<T>> ObsVal<T>.times(other: ObsVal<T>): ObsVal<T> = binding(other) {
  it*other.value
}*/

operator fun <T: Mathable<T>> ObsVal<T>.minus(other: ObsVal<T>): ObsVal<T> = binding(other) {
  it - other.value
}

operator fun <T: Mathable<T>> ObsVal<T>.plus(other: ObsVal<T>): ObsVal<T> = binding(other) {
  it + other.value
}

/*
operator fun <T: Mathable<T>> ObsVal<T>.div(other: ObsVal<T>): ObsVal<T> = binding(other) {
  it/other.value
}
*/


/*operator fun <T: Mathable<T>> Var<T>.timesAssign(other: ObsVal<T>) {
  value *= other.value
}*/

operator fun <T: Mathable<T>> Var<T>.minusAssign(other: ObsVal<T>) {
  value -= other.value
}

operator fun <T: Mathable<T>> Var<T>.plusAssign(other: ObsVal<T>) {
  value += other.value
}

/*operator fun <T: Mathable<T>> Var<T>.divAssign(other: ObsVal<T>) {
  value /= other.value
}*/


/*operator fun <T: Mathable<T>> ObsVal<T>.times(other: T): ObsVal<T> = binding {
  it*other
}*/

operator fun <T: Mathable<T>> ObsVal<T>.minus(other: T): ObsVal<T> = binding {
  it - other
}

operator fun <T: Mathable<T>> ObsVal<T>.plus(other: T): ObsVal<T> = binding {
  it + other
}

/*operator fun <T: Mathable<T>> ObsVal<T>.div(other: T): ObsVal<T> = binding {
  it/other
}*/





operator fun <T: Mathable<T>> ObsVal<T>.times(other: Number): ObsVal<T> = binding {
  it*other
}

/*
operator fun <T: Mathable<T>> ObsVal<T>.minus(other: Number): ObsVal<T> = binding {
  it - other
}

operator fun <T: Mathable<T>> ObsVal<T>.plus(other: Number): ObsVal<T> = binding {
  it + other
}
*/

operator fun <T: Mathable<T>> ObsVal<T>.div(other: Number): ObsVal<T> = binding {
  it/other
}






/*operator fun <T: Mathable<T>> Var<T>.timesAssign(other: T) {
  value *= other
}*/

operator fun <T: Mathable<T>> Var<T>.minusAssign(other: T) {
  value -= other
}

operator fun <T: Mathable<T>> Var<T>.plusAssign(other: T) {
  value += other
}

/*operator fun <T: Mathable<T>> Var<T>.divAssign(other: T) {
  value /= other
}*/








operator fun <T: Mathable<T>> Var<T>.timesAssign(other: Number) {
  value *= other
}

/*operator fun <T: Mathable<T>> Var<T>.minusAssign(other: Number) {
  value -= other
}

operator fun <T: Mathable<T>> Var<T>.plusAssign(other: Number) {
  value += other
}*/

operator fun <T: Mathable<T>> Var<T>.divAssign(other: Number) {
  value /= other
}
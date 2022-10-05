package matt.obs.math

import matt.lang.Num
import matt.math.neg.unaryMinus
import matt.math.op.div
import matt.math.op.minus
import matt.math.op.plus
import matt.math.op.rem
import matt.math.op.times
import matt.obs.bind.binding
import matt.obs.prop.ObsVal
import matt.obs.prop.Var

typealias ObsNum = ObsVal<Number>

operator fun Number.times(other: ObsNum): ObsNum = other.binding {
  this*it
}

operator fun Number.minus(other: ObsNum): ObsNum = other.binding {
  this - it
}

operator fun Number.plus(other: ObsNum): ObsNum = other.binding {
  this + it
}

operator fun Number.div(other: ObsNum): ObsNum = other.binding {
  this/it
}

operator fun Number.rem(other: ObsNum): ObsNum = other.binding {
  this%it
}


/*operator fun <N: Number> ObsVal<N>.unaryPlus(): ObsVal<N> = binding { it }*/
operator fun <N: Number> ObsVal<N>.unaryMinus(): ObsVal<N> = binding { -it }.cast()

operator fun ObsNum.times(other: ObsNum): ObsNum = binding(other) {
  it*other.value
}

operator fun ObsNum.minus(other: ObsNum): ObsNum = binding(other) {
  it - other.value
}

operator fun ObsNum.plus(other: ObsNum): ObsNum = binding(other) {
  it + other.value
}

operator fun ObsNum.div(other: ObsNum): ObsNum = binding(other) {
  it/other.value
}

operator fun ObsNum.rem(other: ObsNum): ObsNum = binding(other) {
  it%other.value
}

operator fun Var<Num>.timesAssign(other: ObsNum) {
  value *= other.value
}

operator fun Var<Num>.minusAssign(other: ObsNum) {
  value -= other.value
}

operator fun Var<Num>.plusAssign(other: ObsNum) {
  value += other.value
}

operator fun Var<Num>.divAssign(other: ObsNum) {
  value /= other.value
}

operator fun Var<Num>.remAssign(other: ObsNum) {
  value /= other.value
}


operator fun ObsNum.times(other: Number): ObsNum = binding {
  it*other
}

operator fun ObsNum.minus(other: Number): ObsNum = binding {
  it - other
}

operator fun ObsNum.plus(other: Number): ObsNum = binding {
  it + other
}

operator fun ObsNum.div(other: Number): ObsNum = binding {
  it/other
}

operator fun ObsNum.rem(other: Number): ObsNum = binding {
  it%other
}

operator fun Var<Num>.timesAssign(other: Number) {
  value *= other
}

operator fun Var<Num>.minusAssign(other: Number) {
  value -= other
}

operator fun Var<Num>.plusAssign(other: Number) {
  value += other
}

operator fun Var<Num>.divAssign(other: Number) {
  value /= other
}

operator fun Var<Num>.remAssign(other: Number) {
  value /= other
}

fun <N: Num> reduction(vararg values: ObsVal<N>, op: (Array<out ObsVal<N>>)->N): ObsVal<N> {
  require(values.isNotEmpty())
  return if (values.size == 1) values[0]
  else values[0].binding(*values.drop(1).toTypedArray()) {
	op(values)
  }
}





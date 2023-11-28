package matt.obs.math.op

import matt.lang.Num
import matt.math.langg.arithmetic.op.div
import matt.math.langg.arithmetic.op.minus
import matt.math.langg.arithmetic.op.plus
import matt.math.langg.arithmetic.op.rem
import matt.math.langg.arithmetic.op.times
import matt.obs.bind.binding
import matt.obs.math.ObsNum
import matt.obs.prop.Var

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
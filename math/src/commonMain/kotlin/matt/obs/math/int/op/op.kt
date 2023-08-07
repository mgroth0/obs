package matt.obs.math.int.op

import matt.obs.bind.binding
import matt.obs.math.int.ObsI
import matt.obs.prop.Var

operator fun Int.times(other: ObsI): ObsI = other.binding {
    this * it
}

operator fun Int.minus(other: ObsI): ObsI = other.binding {
    this - it
}

operator fun Int.plus(other: ObsI): ObsI = other.binding {
    this + it
}

operator fun Int.div(other: ObsI): ObsI = other.binding {
    this / it
}

operator fun Int.rem(other: ObsI): ObsI = other.binding {
    this % it
}


operator fun ObsI.times(other: ObsI): ObsI = binding(other) {
    it * other.value
}

operator fun ObsI.minus(other: ObsI): ObsI = binding(other) {
    it - other.value
}

operator fun ObsI.plus(other: ObsI): ObsI = binding(other) {
    it + other.value
}

operator fun ObsI.div(other: ObsI): ObsI = binding(other) {
    it / other.value
}

operator fun ObsI.rem(other: ObsI): ObsI = binding(other) {
    it % other.value
}

operator fun Var<Int>.timesAssign(other: ObsI) {
    value *= other.value
}

operator fun Var<Int>.minusAssign(other: ObsI) {
    value -= other.value
}

operator fun Var<Int>.plusAssign(other: ObsI) {
    value += other.value
}

operator fun Var<Int>.divAssign(other: ObsI) {
    value /= other.value
}

operator fun Var<Int>.remAssign(other: ObsI) {
    value %= other.value
}


operator fun ObsI.times(other: Int): ObsI = binding {
    it * other
}

operator fun ObsI.minus(other: Int): ObsI = binding {
    it - other
}

operator fun ObsI.plus(other: Int): ObsI = binding {
    it + other
}

operator fun ObsI.div(other: Int): ObsI = binding {
    it / other
}

operator fun ObsI.rem(other: Int): ObsI = binding {
    it % other
}

operator fun Var<Int>.timesAssign(other: Int) {
    value *= other
}

operator fun Var<Int>.minusAssign(other: Int) {
    value -= other
}

operator fun Var<Int>.plusAssign(other: Int) {
    value += other
}

operator fun Var<Int>.divAssign(other: Int) {
    value /= other
}

operator fun Var<Int>.remAssign(other: Int) {
    value %= other
}
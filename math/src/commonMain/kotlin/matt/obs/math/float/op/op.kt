package matt.obs.math.float.op

import matt.obs.bind.binding
import matt.obs.math.float.ObsF
import matt.obs.prop.Var

operator fun Float.times(other: ObsF): ObsF = other.binding {
    this*it
}

operator fun Float.minus(other: ObsF): ObsF = other.binding {
    this - it
}

operator fun Float.plus(other: ObsF): ObsF = other.binding {
    this + it
}

operator fun Float.div(other: ObsF): ObsF = other.binding {
    this/it
}

operator fun Float.rem(other: ObsF): ObsF = other.binding {
    this%it
}


operator fun ObsF.times(other: ObsF): ObsF = binding(other) {
    it*other.value
}

operator fun ObsF.minus(other: ObsF): ObsF = binding(other) {
    it - other.value
}

operator fun ObsF.plus(other: ObsF): ObsF = binding(other) {
    it + other.value
}

operator fun ObsF.div(other: ObsF): ObsF = binding(other) {
    it/other.value
}

operator fun ObsF.rem(other: ObsF): ObsF = binding(other) {
    it%other.value
}

operator fun Var<Float>.timesAssign(other: ObsF) {
    value *= other.value
}

operator fun Var<Float>.minusAssign(other: ObsF) {
    value -= other.value
}

operator fun Var<Float>.plusAssign(other: ObsF) {
    value += other.value
}

operator fun Var<Float>.divAssign(other: ObsF) {
    value /= other.value
}

operator fun Var<Float>.remAssign(other: ObsF) {
    value %= other.value
}


operator fun ObsF.times(other: Float): ObsF = binding {
    it*other
}

operator fun ObsF.minus(other: Float): ObsF = binding {
    it - other
}

operator fun ObsF.plus(other: Float): ObsF = binding {
    it + other
}

operator fun ObsF.div(other: Float): ObsF = binding {
    it/other
}

operator fun ObsF.rem(other: Float): ObsF = binding {
    it%other
}

operator fun Var<Float>.timesAssign(other: Float) {
    value *= other
}

operator fun Var<Float>.minusAssign(other: Float) {
    value -= other
}

operator fun Var<Float>.plusAssign(other: Float) {
    value += other
}

operator fun Var<Float>.divAssign(other: Float) {
    value /= other
}

operator fun Var<Float>.remAssign(other: Float) {
    value %= other
}

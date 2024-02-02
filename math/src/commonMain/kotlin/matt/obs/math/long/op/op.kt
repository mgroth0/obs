package matt.obs.math.long.op

import matt.obs.bind.binding
import matt.obs.math.long.ObsL
import matt.obs.prop.Var

operator fun Long.times(other: ObsL): ObsL = other.binding {
    this*it
}

operator fun Long.minus(other: ObsL): ObsL = other.binding {
    this - it
}

operator fun Long.plus(other: ObsL): ObsL = other.binding {
    this + it
}

operator fun Long.div(other: ObsL): ObsL = other.binding {
    this/it
}

operator fun Long.rem(other: ObsL): ObsL = other.binding {
    this%it
}


operator fun ObsL.times(other: ObsL): ObsL = binding(other) {
    it*other.value
}

operator fun ObsL.minus(other: ObsL): ObsL = binding(other) {
    it - other.value
}

operator fun ObsL.plus(other: ObsL): ObsL = binding(other) {
    it + other.value
}

operator fun ObsL.div(other: ObsL): ObsL = binding(other) {
    it/other.value
}

operator fun ObsL.rem(other: ObsL): ObsL = binding(other) {
    it%other.value
}

operator fun Var<Long>.timesAssign(other: ObsL) {
    value *= other.value
}

operator fun Var<Long>.minusAssign(other: ObsL) {
    value -= other.value
}

operator fun Var<Long>.plusAssign(other: ObsL) {
    value += other.value
}

operator fun Var<Long>.divAssign(other: ObsL) {
    value /= other.value
}

operator fun Var<Long>.remAssign(other: ObsL) {
    value %= other.value
}


operator fun ObsL.times(other: Long): ObsL = binding {
    it*other
}

operator fun ObsL.minus(other: Long): ObsL = binding {
    it - other
}

operator fun ObsL.plus(other: Long): ObsL = binding {
    it + other
}

operator fun ObsL.div(other: Long): ObsL = binding {
    it/other
}

operator fun ObsL.rem(other: Long): ObsL = binding {
    it%other
}

operator fun Var<Long>.timesAssign(other: Long) {
    value *= other
}

operator fun Var<Long>.minusAssign(other: Long) {
    value -= other
}

operator fun Var<Long>.plusAssign(other: Long) {
    value += other
}

operator fun Var<Long>.divAssign(other: Long) {
    value /= other
}

operator fun Var<Long>.remAssign(other: Long) {
    value %= other
}

package matt.obs.math.double.op

import matt.obs.bind.binding
import matt.obs.math.double.ObsD
import matt.obs.prop.writable.Var

operator fun Double.times(other: ObsD): ObsD =
    other.binding {
        this * it
    }

operator fun Double.minus(other: ObsD): ObsD =
    other.binding {
        this - it
    }

operator fun Double.plus(other: ObsD): ObsD =
    other.binding {
        this + it
    }

operator fun Double.div(other: ObsD): ObsD =
    other.binding {
        this / it
    }

operator fun Double.rem(other: ObsD): ObsD =
    other.binding {
        this % it
    }


operator fun ObsD.times(other: ObsD): ObsD =
    binding(other) {
        it * other.value
    }

operator fun ObsD.minus(other: ObsD): ObsD =
    binding(other) {
        it - other.value
    }

operator fun ObsD.plus(other: ObsD): ObsD =
    binding(other) {
        it + other.value
    }

operator fun ObsD.div(other: ObsD): ObsD =
    binding(other) {
        it / other.value
    }

operator fun ObsD.rem(other: ObsD): ObsD =
    binding(other) {
        it % other.value
    }

operator fun Var<Double>.timesAssign(other: ObsD) {
    value *= other.value
}

operator fun Var<Double>.minusAssign(other: ObsD) {
    value -= other.value
}

operator fun Var<Double>.plusAssign(other: ObsD) {
    value += other.value
}

operator fun Var<Double>.divAssign(other: ObsD) {
    value /= other.value
}

operator fun Var<Double>.remAssign(other: ObsD) {
    value %= other.value
}


operator fun ObsD.times(other: Double): ObsD =
    binding {
        it * other
    }

operator fun ObsD.minus(other: Double): ObsD =
    binding {
        it - other
    }

operator fun ObsD.plus(other: Double): ObsD =
    binding {
        it + other
    }

operator fun ObsD.div(other: Double): ObsD =
    binding {
        it / other
    }

operator fun ObsD.rem(other: Double): ObsD =
    binding {
        it % other
    }

operator fun Var<Double>.timesAssign(other: Double) {
    value *= other
}

operator fun Var<Double>.minusAssign(other: Double) {
    value -= other
}

operator fun Var<Double>.plusAssign(other: Double) {
    value += other
}

operator fun Var<Double>.divAssign(other: Double) {
    value /= other
}

operator fun Var<Double>.remAssign(other: Double) {
    value %= other
}

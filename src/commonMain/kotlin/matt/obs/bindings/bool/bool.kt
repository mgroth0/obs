package matt.obs.bindings.bool

import matt.lang.common.B
import matt.obs.bind.binding
import matt.obs.prop.ObsVal

typealias ObsB = ObsVal<B>

/*private val notProps = lazyWeakMap<ObsB, ObsB> { prop ->

}*/

fun ObsB.not() =
    binding {
        !it
    }


infix fun ObsB.and(other: ObsB) =
    binding(other) {
        it && other.value
    }

infix fun ObsB.and(other: Boolean) =
    binding {
        it && other
    }

infix fun ObsB.or(other: ObsB) =
    binding(other) {
        it || other.value
    }

infix fun ObsB.or(other: Boolean) =
    binding {
        it || other
    }

infix fun ObsB.xor(other: ObsB) =
    binding(other) {
        it xor other.value
    }

infix fun ObsB.xor(other: Boolean) =
    binding {
        it xor other
    }

package matt.obs.bindings.str

import matt.lang.I
import matt.lang.S
import matt.obs.bind.binding
import matt.obs.bindings.comp.compareTo
import matt.obs.prop.ObsVal
import matt.obs.prop.Var
import matt.prim.str.loweq

typealias ObsS = ObsVal<S>
typealias VarS = Var<S>

fun ObsS.length() = binding {
    value.length
}

fun ObsS.isEmpty() = binding {
    value.isEmpty()
}

fun ObsS.isNotEmpty() = binding {
    value.isNotEmpty()
}

fun ObsS.isBlank() = binding {
    value.isBlank()
}

fun ObsS.isNotBlank() = binding {
    value.isNotBlank()
}

fun ObsS.reversed() = binding {
    value.reversed()
}

operator fun ObsS.unaryMinus() = reversed()

operator fun ObsS.plus(other: String): ObsS = binding {
    it + other
}

operator fun ObsS.plus(other: ObsS): ObsS = binding(other) {
    it + other.value
}

operator fun VarS.plusAssign(other: String) {
    value += other
}

operator fun VarS.plusAssign(other: ObsS) {
    value += other.value
}

/*
Shouldn't do this because string + Any is already defined!
operator fun String.plus(other: ObsS): ObsS = other.binding {
  this@plus + it
}
*/


/*not sure whether to use out or in generic here*/
infix fun ObsS.eqIgnoreCase(other: ObsS) = binding(other) {
    it loweq other.value
}

/*not sure whether to use out or in generic here*/
infix fun ObsS.eqIgnoreCase(other: String) = binding {
    it loweq other
}


operator fun ObsS.get(index: Int): ObsVal<Char?> = binding(this) {
    if (index < value.length)
        value[index]
    else
        null
}

operator fun ObsS.get(index: ObsVal<I>): ObsVal<Char?> = binding(this, index) {

    if (index < value.length)
        value[index.value]
    else
        null
}

operator fun ObsS.get(start: Int, end: Int): ObsS = binding(this) {
    value.subSequence(start, end).toString()
}

operator fun ObsS.get(start: ObsVal<I>, end: Int): ObsS = binding(this, start) {
    value.subSequence(start.value, end).toString()
}

operator fun ObsS.get(start: Int, end: ObsVal<I>): ObsS = binding(this, end) {
    value.subSequence(start, end.value).toString()
}

operator fun ObsS.get(start: ObsVal<I>, end: ObsVal<I>): ObsS = binding(this, start, end) {
    value.subSequence(start.value, end.value).toString()
}

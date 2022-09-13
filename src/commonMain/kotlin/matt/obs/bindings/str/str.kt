package matt.obs.bindings.str

import matt.lang.S
import matt.obs.prop.ObsVal

typealias ObsS = ObsVal<S>

fun ObsS.length() = binding {
  value.length
}

fun ObsS.isEmpty() = binding {
  value.isEmpty()
}

fun ObsS.isNotEmpty() = binding {
  value.isNotEmpty()
}

operator fun ObsS.plus(other: String): ObsS = binding {
  it + other
}

operator fun ObsS.plus(other: ObsS): ObsS =
  binding(other) {
	it + other.value
  }

/*

Shouldn't do this because string + Any is already defined!

operator fun String.plus(other: ObsS): ObsS = other.binding {
  this@plus + it
}

*/




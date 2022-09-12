package matt.obs.bindings.str

import matt.obs.bind.binding
import matt.obs.prop.MObservableROPropBase
import matt.obs.prop.ValProp

fun MObservableROPropBase<String>.length() = binding {
  value.length
}

fun MObservableROPropBase<String>.isEmpty() = binding {
  value.isEmpty()
}

fun MObservableROPropBase<String>.isNotEmpty() = binding {
  value.isNotEmpty()
}

operator fun MObservableROPropBase<String>.plus(other: String): ValProp<String> = binding {
  it + other
}

operator fun MObservableROPropBase<String>.plus(other: MObservableROPropBase<String>): ValProp<String> =
  binding(other) {
	it + other.value
  }

//operator fun String.plus(other: MObservableROPropBase<String>): ValProp<String> = other.binding {
//  this@plus + it
//}
package matt.obs.bindings.comp

import matt.obs.bind.binding
import matt.obs.prop.MObservableROPropBase
import matt.obs.prop.ReadOnlyBindableProperty

infix fun <T: Comparable<T>> MObservableROPropBase<T>.ge(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it >= other.value
}

infix fun <T: Comparable<T>> MObservableROPropBase<T>.le(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it <= other.value
}

infix fun <T: Comparable<T>> MObservableROPropBase<T>.lt(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it < other.value
}

infix fun <T: Comparable<T>> MObservableROPropBase<T>.gt(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it > other.value
}



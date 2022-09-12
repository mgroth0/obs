package matt.obs.bindings

import matt.collect.weak.lazyWeakMap
import matt.obs.bind.binding
import matt.obs.prop.BindableProperty
import matt.obs.prop.MObservableROPropBase
import matt.obs.prop.ReadOnlyBindableProperty

private val notProps = lazyWeakMap<BindableProperty<Boolean>, ReadOnlyBindableProperty<Boolean>> { prop ->
  prop.binding {
	!it
  }
}

fun MObservableROPropBase<Boolean>.not() = notProps[this]!!


infix fun MObservableROPropBase<Boolean>.and(other: ReadOnlyBindableProperty<Boolean>) = binding(other) {
  it && other.value
}

infix fun MObservableROPropBase<Boolean>.or(other: ReadOnlyBindableProperty<Boolean>) = binding(other) {
  it || other.value
}


fun MObservableROPropBase<String>.length() = binding {
  value.length
}

fun MObservableROPropBase<String>.isEmpty() = binding {
  value.isEmpty()
}
fun MObservableROPropBase<String>.isNotEmpty() = binding {
  value.isNotEmpty()
}


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



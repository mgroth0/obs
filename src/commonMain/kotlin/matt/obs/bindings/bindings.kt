package matt.obs.bindings

import matt.collect.weak.lazyWeakMap
import matt.obs.MObservableROValBase
import matt.obs.bind.binding
import matt.obs.prop.BindableProperty
import matt.obs.prop.ReadOnlyBindableProperty

private val notProps = lazyWeakMap<BindableProperty<Boolean>, ReadOnlyBindableProperty<Boolean>> { prop ->
  prop.binding {
	!it
  }
}

fun MObservableROValBase<Boolean>.not() = notProps[this]!!


infix fun MObservableROValBase<Boolean>.and(other: ReadOnlyBindableProperty<Boolean>) = binding(other) {
  it && other.value
}

infix fun MObservableROValBase<Boolean>.or(other: ReadOnlyBindableProperty<Boolean>) = binding(other) {
  it || other.value
}


fun MObservableROValBase<String>.length() = binding {
  value.length
}

fun MObservableROValBase<String>.isEmpty() = binding {
  value.isEmpty()
}
fun MObservableROValBase<String>.isNotEmpty() = binding {
  value.isNotEmpty()
}


infix fun <T: Comparable<T>> MObservableROValBase<T>.ge(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it >= other.value
}

infix fun <T: Comparable<T>> MObservableROValBase<T>.le(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it <= other.value
}

infix fun <T: Comparable<T>> MObservableROValBase<T>.lt(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it < other.value
}

infix fun <T: Comparable<T>> MObservableROValBase<T>.gt(other: ReadOnlyBindableProperty<T>) = binding(other) {
  it > other.value
}



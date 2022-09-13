package matt.obs.bindings.bool

import matt.collect.weak.lazyWeakMap
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

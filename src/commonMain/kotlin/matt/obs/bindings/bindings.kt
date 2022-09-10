package matt.obs.bindings

import matt.collect.weak.lazyWeakMap
import matt.lang.B
import matt.obs.bind.binding
import matt.obs.col.BasicObservableCollection
import matt.obs.prop.BindableProperty
import matt.obs.prop.ReadOnlyBindableProperty
import matt.obs.prop.VarProp

private val isNullProps = lazyWeakMap<BindableProperty<*>, ReadOnlyBindableProperty<Boolean>> { prop ->
  prop.binding {
	it == null
  }
}

fun ReadOnlyBindableProperty<*>.isNull() = isNullProps[this]!!

private val notProps = lazyWeakMap<BindableProperty<Boolean>, ReadOnlyBindableProperty<Boolean>> { prop ->
  prop.binding {
	!it
  }
}

fun ReadOnlyBindableProperty<Boolean>.not() = notProps[this]!!


infix fun ReadOnlyBindableProperty<Boolean>.and(other: ReadOnlyBindableProperty<Boolean>) = binding(other) {
  it && other.value
}

infix fun ReadOnlyBindableProperty<Boolean>.or(other: ReadOnlyBindableProperty<Boolean>) = binding(other) {
  it || other.value
}


infix fun ReadOnlyBindableProperty<*>.eq(other: ReadOnlyBindableProperty<*>) = binding(other) {
  it == other.value
}

infix fun ReadOnlyBindableProperty<*>.neq(other: ReadOnlyBindableProperty<*>) = eq(other).not()

infix fun ReadOnlyBindableProperty<*>.eq(other: Any) = binding {
  it == other
}

infix fun ReadOnlyBindableProperty<*>.neq(other: Any) = eq(other).not()


private val isEmptyProps = lazyWeakMap<BasicObservableCollection<*>, ReadOnlyBindableProperty<Boolean>> { li ->
  VarProp<B>(li.isEmpty()).apply {
	li.onChange {
	  value = li.isEmpty()
	}
  }
}

fun BasicObservableCollection<*>.isEmptyProperty() = isEmptyProps[this]


package matt.obs.bindings.math

import matt.obs.prop.MObservableROPropBase
import matt.obs.prop.ReadOnlyBindableProperty
import matt.obs.prop.ValProp

operator fun MObservableROPropBase<Double>.unaryPlus(): ValProp<Double> = binding { it }
operator fun MObservableROPropBase<Double>.unaryMinus(): ValProp<Double> = binding { -it }

operator fun MObservableROPropBase<Double>.times(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it*other.value
  }

operator fun MObservableROPropBase<Double>.minus(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it - other.value
  }

operator fun MObservableROPropBase<Double>.plus(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it + other.value
  }

operator fun MObservableROPropBase<Double>.div(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it/other.value
  }

private operator fun Number.plus(n: Number) = toDouble() + n.toDouble()
private operator fun Number.times(n: Number) = toDouble()*n.toDouble()
private operator fun Number.minus(n: Number) = toDouble() - n.toDouble()
private operator fun Number.div(n: Number) = toDouble()/n.toDouble()


operator fun MObservableROPropBase<Double>.times(other: Number): ValProp<Double> = binding {
  it*other
}

operator fun MObservableROPropBase<Double>.minus(other: Number): ValProp<Double> = binding {
  it - other
}

operator fun MObservableROPropBase<Double>.plus(other: Number): ValProp<Double> = binding {
  it + other
}

operator fun MObservableROPropBase<Double>.div(other: Number): ValProp<Double> = binding {
  it/other
}

operator fun Number.times(other: MObservableROPropBase<Double>): ValProp<Double> = other.binding {
  it*this
}

operator fun Number.minus(other: MObservableROPropBase<Double>): ValProp<Double> = other.binding {
  it - this
}

operator fun Number.plus(other: MObservableROPropBase<Double>): ValProp<Double> = other.binding {
  it + this
}

operator fun Number.div(other: MObservableROPropBase<Double>): ValProp<Double> = other.binding {
  it/this
}

fun min(vararg values: MObservableROPropBase<Double>): MObservableROPropBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.minOf { it.value }
  }
}

fun max(vararg values: MObservableROPropBase<Double>): MObservableROPropBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.maxOf { it.value }
  }
}

fun mean(vararg values: MObservableROPropBase<Double>): MObservableROPropBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.sumOf { it.value }/values.size
  }
}

fun sum(vararg values: MObservableROPropBase<Double>): MObservableROPropBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.sumOf { it.value }
  }
}


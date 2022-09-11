package matt.obs.bindings.math

import matt.obs.MObservableROValBase
import matt.obs.bind.binding
import matt.obs.prop.ReadOnlyBindableProperty
import matt.obs.prop.ValProp

operator fun MObservableROValBase<Double>.times(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it*other.value
  }

operator fun MObservableROValBase<Double>.minus(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it - other.value
  }

operator fun MObservableROValBase<Double>.plus(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it + other.value
  }

operator fun MObservableROValBase<Double>.div(other: ReadOnlyBindableProperty<Double>): ValProp<Double> =
  binding(other) {
	it/other.value
  }

private operator fun Number.plus(n: Number) = toDouble() + n.toDouble()
private operator fun Number.times(n: Number) = toDouble()*n.toDouble()
private operator fun Number.minus(n: Number) = toDouble() - n.toDouble()
private operator fun Number.div(n: Number) = toDouble()/n.toDouble()


operator fun MObservableROValBase<Double>.times(other: Number): ValProp<Double> = binding {
  it*other
}

operator fun MObservableROValBase<Double>.minus(other: Number): ValProp<Double> = binding {
  it - other
}

operator fun MObservableROValBase<Double>.plus(other: Number): ValProp<Double> = binding {
  it + other
}

operator fun MObservableROValBase<Double>.div(other: Number): ValProp<Double> = binding {
  it/other
}

operator fun Number.times(other: MObservableROValBase<Double>): ValProp<Double> = other.binding {
  it*this
}

operator fun Number.minus(other: MObservableROValBase<Double>): ValProp<Double> = other.binding {
  it - this
}

operator fun Number.plus(other: MObservableROValBase<Double>): ValProp<Double> = other.binding {
  it + this
}

operator fun Number.div(other: MObservableROValBase<Double>): ValProp<Double> = other.binding {
  it/this
}

fun min(vararg values: MObservableROValBase<Double>): MObservableROValBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.minOf { it.value }
  }
}

fun max(vararg values: MObservableROValBase<Double>): MObservableROValBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.maxOf { it.value }
  }
}

fun mean(vararg values: MObservableROValBase<Double>): MObservableROValBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.sumOf { it.value }/values.size
  }
}

fun sum(vararg values: MObservableROValBase<Double>): MObservableROValBase<Double> {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.sumOf { it.value }
  }
}


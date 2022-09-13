package matt.obs.bindings.math

import matt.lang.D
import matt.obs.prop.ObsVal

typealias ObsD = ObsVal<D>

operator fun ObsD.unaryPlus(): ObsD = binding { it }
operator fun ObsD.unaryMinus(): ObsD = binding { -it }

operator fun ObsD.times(other: ObsD): ObsD =
  binding(other) {
	it*other.value
  }

operator fun ObsD.minus(other: ObsD): ObsD =
  binding(other) {
	it - other.value
  }

operator fun ObsD.plus(other: ObsD): ObsD =
  binding(other) {
	it + other.value
  }

operator fun ObsD.div(other: ObsD): ObsD =
  binding(other) {
	it/other.value
  }

private operator fun Number.plus(n: Number) = toDouble() + n.toDouble()
private operator fun Number.times(n: Number) = toDouble()*n.toDouble()
private operator fun Number.minus(n: Number) = toDouble() - n.toDouble()
private operator fun Number.div(n: Number) = toDouble()/n.toDouble()


operator fun ObsD.times(other: Number): ObsD = binding {
  it*other
}

operator fun ObsD.minus(other: Number): ObsD = binding {
  it - other
}

operator fun ObsD.plus(other: Number): ObsD = binding {
  it + other
}

operator fun ObsD.div(other: Number): ObsD = binding {
  it/other
}

operator fun Number.times(other: ObsD): ObsD = other.binding {
  it*this
}

operator fun Number.minus(other: ObsD): ObsD = other.binding {
  it - this
}

operator fun Number.plus(other: ObsD): ObsD = other.binding {
  it + this
}

operator fun Number.div(other: ObsD): ObsD = other.binding {
  it/this
}

fun min(vararg values: ObsD): ObsD {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.minOf { it.value }
  }
}

fun max(vararg values: ObsD): ObsD {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.maxOf { it.value }
  }
}

fun mean(vararg values: ObsD): ObsD {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.sumOf { it.value }/values.size
  }
}

fun sum(vararg values: ObsD): ObsD {
  require(values.size > 0)
  if (values.size == 1) return values[0]
  else return values[0].binding(*values.drop(1).toTypedArray()) {
	values.sumOf { it.value }
  }
}


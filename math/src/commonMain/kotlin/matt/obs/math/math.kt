package matt.obs.math

import matt.lang.Num
import matt.lang.require.requireNotEmpty
import matt.math.neg.unaryMinus
import matt.obs.bind.binding
import matt.obs.prop.ObsVal

typealias ObsNum = ObsVal<out Num>

/*operator fun <N: Number> ObsVal<N>.unaryPlus(): ObsVal<N> = binding { it }*/
operator fun <N: Number> ObsVal<N>.unaryMinus(): ObsVal<N> = binding { -it }.cast()


fun <N: Num> reduction(vararg values: ObsVal<N>, op: (Array<out ObsVal<N>>)->N): ObsVal<N> {
  requireNotEmpty(values)
  return if (values.size == 1) values[0]
  else values[0].binding(*values.drop(1).toTypedArray()) {
	op(values)
  }
}


fun  ObsNum.asInt() = binding { it.toInt() }
fun  ObsNum.asLong() = binding { it.toLong() }
fun ObsNum.asShort() = binding { it.toShort() }
fun  ObsNum.asFloat() = binding { it.toFloat() }
fun  ObsNum.asDouble() = binding { it.toDouble() }

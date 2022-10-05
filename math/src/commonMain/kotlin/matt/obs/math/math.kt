package matt.obs.math

import matt.lang.Num
import matt.math.neg.unaryMinus
import matt.math.op.div
import matt.math.op.minus
import matt.math.op.plus
import matt.math.op.rem
import matt.math.op.times
import matt.obs.bind.binding
import matt.obs.prop.ObsVal
import matt.obs.prop.Var

typealias ObsNum = ObsVal<out Num>

/*operator fun <N: Number> ObsVal<N>.unaryPlus(): ObsVal<N> = binding { it }*/
operator fun <N: Number> ObsVal<N>.unaryMinus(): ObsVal<N> = binding { -it }.cast()



fun <N: Num> reduction(vararg values: ObsVal<N>, op: (Array<out ObsVal<N>>)->N): ObsVal<N> {
  require(values.isNotEmpty())
  return if (values.size == 1) values[0]
  else values[0].binding(*values.drop(1).toTypedArray()) {
	op(values)
  }
}





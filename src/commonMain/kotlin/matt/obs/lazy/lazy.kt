package matt.obs.lazy

import kotlinx.datetime.Clock
import matt.lang.go
import matt.lang.sync.inSync
import kotlin.jvm.Synchronized

private object EMPTY

class DependentValue<V>(private var op: ()->V) {

  var stopwatch: String? = null


  @Synchronized fun setOp(op: ()->V) {
	this.op = op
	markInvalid()
  }

  fun markInvalid() {
	inSync(invalidationDuringGetMonitor) {
	  valid = false
	  justMarkedInvalid = true
	}
  }


  private val invalidationDuringGetMonitor = object {}
  private var justMarkedInvalid = false
  @Synchronized @Suppress("UNCHECKED_CAST") fun get(): V {
	val momentValid = inSync(invalidationDuringGetMonitor) {
	  justMarkedInvalid = false
	  valid
	}
	return if (momentValid) lastCalculated as V else {
	  calc()
	  lastCalculated as V
	}
  }


  private fun calc() {
	val t = if (stopwatch != null) Clock.System.now() else null
	lastCalculated = op()
	t?.go {
	  println("stopwatch\t${(Clock.System.now() - t)}\t$stopwatch")
	}
	inSync(invalidationDuringGetMonitor) {
	  if (!justMarkedInvalid) valid = true
	}
  }

  private var lastCalculated: Any? = EMPTY
  private var valid: Boolean = false

}
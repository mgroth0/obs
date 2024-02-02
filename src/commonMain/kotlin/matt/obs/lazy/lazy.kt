package matt.obs.lazy

import kotlinx.datetime.Clock
import matt.lang.go
import matt.lang.sync.ReferenceMonitor
import matt.lang.sync.SimpleReferenceMonitor
import matt.lang.sync.inSync

private object EMPTY

class DependentValue<V>(private var op: () -> V) : ReferenceMonitor {

    var stopwatch: String? = null


    fun setOp(op: () -> V) = inSync {
        this.op = op
        markInvalid()
    }

    fun markInvalid() {
        inSync(invalidationDuringGetMonitor) {
            valid = false
            justMarkedInvalid = true
        }
    }


    private val invalidationDuringGetMonitor = SimpleReferenceMonitor()
    private var justMarkedInvalid = false
    @Suppress("UNCHECKED_CAST")
    fun get(): V = inSync {
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

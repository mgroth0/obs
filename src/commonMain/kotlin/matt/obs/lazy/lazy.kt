package matt.obs.lazy

import kotlinx.datetime.Clock
import matt.lang.common.go
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.common.inSync
import matt.lang.sync.inSync
import kotlin.jvm.JvmInline

private sealed interface CalculatedValue<out V> {
    fun assume(): V
}
private data object EMPTY: CalculatedValue<Nothing> {
    override fun assume(): Nothing {
        error("bad assumption")
    }
}

@JvmInline
private value class Value<V>(val v: V): CalculatedValue<V> {
    override fun assume(): V = v
}

class DependentValue<V>(private var op: () -> V) : ReferenceMonitor {

    var stopwatch: String? = null


    fun setOp(op: () -> V) =
        inSync {
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

    fun get(): V =
        inSync {
            val momentValid =
                inSync(invalidationDuringGetMonitor) {
                    justMarkedInvalid = false
                    valid
                }
            return if (momentValid) lastCalculated.assume() else {
                calc()
                lastCalculated.assume()
            }
        }


    private fun calc() {
        val t = if (stopwatch != null) Clock.System.now() else null
        lastCalculated = Value(op())
        t?.go {
            println("stopwatch\t${(Clock.System.now() - t)}\t$stopwatch")
        }
        inSync(invalidationDuringGetMonitor) {
            if (!justMarkedInvalid) valid = true
        }
    }

    private var lastCalculated: CalculatedValue<V> = EMPTY
    private var valid: Boolean = false
}

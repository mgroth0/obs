@file:JvmName("BoolJvmKt")

package matt.obs.listen.bool

import matt.obs.bindings.bool.ObsB
import matt.obs.listen.MyListenerInter
import matt.obs.listen.whenEqualsOnce
import kotlin.jvm.JvmName

fun ObsB.whenTrue(op: ()->Unit): MyListenerInter<*> {
    if (value) op()
    return onChange {
        if (it) op()
    }
}

fun ObsB.whenFalse(op: ()->Unit): MyListenerInter<*> {
    if (!value) op()
    return onChange {
        if (!it) op()
    }
}

fun ObsB.whenTrueOnce(op: ()->Unit) = whenEqualsOnce(true, op)
fun ObsB.whenFalseOnce(op: ()->Unit) = whenEqualsOnce(false, op)

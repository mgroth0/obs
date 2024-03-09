package matt.obs.watch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import matt.obs.prop.ObsVal
import matt.obs.prop.writable.BindableProperty
import kotlin.time.Duration

fun <T> CoroutineScope.launchWatchProperty(
    checkInterval: Duration,
    op: () -> T
): ObsVal<T> {
    val prop = BindableProperty(op())
    launch {
        while (true) {
            prop.value = op()
            delay(checkInterval)
        }
    }
    return prop
}









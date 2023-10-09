package matt.obs.watch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import kotlin.time.Duration

fun <T> CoroutineScope.launchWatchProperty(
    checkInterval: Duration,
    op: () -> T
): ObsVal<T> {
    val prop = BindableProperty(op())
    launch {
        supervisorScope {  }
        while (true) {
            prop.value = op()
            delay(checkInterval)
        }
    }
    return prop
}



package matt.obs.test.prop

import matt.lang.assertions.require.requireEquals
import matt.lang.assertions.require.requireOne
import matt.obs.listen.OldAndNewListenerImpl
import matt.obs.prop.writable.VarProp
import kotlin.test.Test

class ObsPropTests {

    @Test
    fun testOldNewUpdates() {


        val prop = VarProp(1)
        prop.addListener(
            OldAndNewListenerImpl { old, new ->
                requireOne(old)
                requireEquals(new, 2)
            }
        )
        prop.value = 2
    }
}

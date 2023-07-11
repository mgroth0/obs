package matt.obs.prop

import matt.lang.require.requireEquals
import matt.lang.require.requireOne
import matt.obs.listen.OldAndNewListenerImpl
import matt.test.yesIUseTestLibs
import org.junit.jupiter.api.Test

class ObsPropTests {

    @Test
    fun testOldNewUpdates() {

        yesIUseTestLibs()

        val prop = VarProp(1)
        prop.addListener(OldAndNewListenerImpl { old, new ->
            requireOne(old)
            requireEquals(new, 2)
        })
        prop.value = 2


    }
}
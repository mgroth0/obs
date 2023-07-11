package matt.obs.bind

import matt.lang.require.requireOne
import matt.test.yesIUseTestLibs
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread


class ObsBindTests {

    @Test
    fun deadlock() {

        yesIUseTestLibs()

        val d = thread(isDaemon = true) {
            var binding: MyBinding<Int>? = null
            binding = MyBinding {
                thread {
                    binding!!.markInvalid()
                }.join()
                1
            }
            requireOne(binding.value)
        }
        d.join(1000)
        if (d.isAlive) {
            error("found deadlock!")
        }


    }
}
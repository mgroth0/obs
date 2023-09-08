package matt.obs.bind

import matt.async.thread.namedThread
import matt.lang.require.requireOne
import matt.test.yesIUseTestLibs
import org.junit.jupiter.api.Test


class ObsBindTests {

    @Test
    fun deadlock() {

        yesIUseTestLibs()

        val d = namedThread(isDaemon = true,name = "ObsBindTests deadlock thread 1") {
            var binding: MyBinding<Int>? = null
            binding = MyBinding {
                namedThread(name="ObsBindTests deadlock thread 2") {
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
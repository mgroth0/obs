package matt.obs.bind

import matt.test.yesIUseTestLibs
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread


class SomeTests {

  @Test fun deadlock() {

	yesIUseTestLibs()

	val d = thread(isDaemon = true) {
	  var binding: MyBinding<Int>? = null
	  binding = MyBinding {
		thread {
		  binding!!.markInvalid()
		}.join()
		1
	  }
	  require(binding.value == 1)
	}
	d.join(1000)
	if (d.isAlive) {
	  error("found deadlock!")
	}


  }
}
package matt.obs.prop

import matt.obs.listen.OldAndNewListener
import matt.test.yesIUseTestLibs
import org.junit.jupiter.api.Test

class SomeTests {

  @Test fun testUpdates() {

	yesIUseTestLibs()

	val prop = VarProp(1)
	prop.addListener(OldAndNewListener { old, new ->
	  require(old == 0)
	  require(new == 2)
	})
	prop.value = 2


  }
}
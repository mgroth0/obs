package matt.obs.col.change

import org.junit.jupiter.api.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KClass
import matt.test.yesIUseTestLibs

class SomeTests {

  @Test @ExperimentalContracts fun testWrappers() {

	yesIUseTestLibs()

	error("yes I'm testing")

  }
}
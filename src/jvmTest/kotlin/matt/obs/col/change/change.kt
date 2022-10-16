package matt.obs.col.change

import matt.test.yesIUseTestLibs
import org.junit.jupiter.api.Test
import kotlin.contracts.ExperimentalContracts

class SomeTests {

  @Test @ExperimentalContracts fun testWrappers() {

	yesIUseTestLibs()

	error("yes I'm testing")

  }
}
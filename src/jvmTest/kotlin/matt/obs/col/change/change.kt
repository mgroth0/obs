package matt.obs.col.change

import org.junit.jupiter.api.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KClass
import kotlin.test.Test

class SomeTests {

  @Tes @ExperimentalContracts fun testWrappers() {

	yesIUseTestLibs()

	error("yes I'm testing")

  }
}
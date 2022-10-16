package matt.obs.col.change

import matt.reflect.reflections.mattSubClasses
import matt.test.yesIUseTestLibs
import kotlin.reflect.full.functions
import kotlin.test.Test
import kotlin.test.assertEquals

class SomeTests {

  @Test fun testWrappers() {

	yesIUseTestLibs()

	CollectionChange::class.mattSubClasses().filter { !it.isAbstract }.forEach {
	  val classifier = it.functions.first { it.name == "convert" }.returnType.classifier
	  if (classifier != it) {
		println("classifier is not $it")
		error("error")
	  }
	}

	//	assertEquals(1, 2)
	//	assertEquals(1, 1)


  }
}
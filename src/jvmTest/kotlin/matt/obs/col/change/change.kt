package matt.obs.col.change

import matt.reflect.scan.mattSubClasses
import matt.reflect.scan.systemScope
import kotlin.reflect.full.functions
import kotlin.test.Test

class ObsColChangeTests {

    @Test
    fun testWrappers() {


        with(systemScope(includePlatformClassloader=false).usingClassGraph()) {

            CollectionChange::class.mattSubClasses().filter { !it.isAbstract && !it.isSealed }.forEach {
                val classifier = it.functions.first { it.name == "convert" }.returnType.classifier
                if (classifier != it) {
                    println("$classifier is not $it")
                    error("error")
                }
            }

        }

        //	assertEquals(1, 2)
        //	assertEquals(1, 1)


    }
}
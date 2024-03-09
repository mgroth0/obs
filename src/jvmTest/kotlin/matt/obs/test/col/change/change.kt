package matt.obs.test.col.change

import matt.obs.col.change.CollectionChange
import matt.reflect.scan.jcommon.systemScope
import matt.reflect.scan.jcommon.usingClassGraph
import matt.reflect.scan.mattSubClasses
import kotlin.reflect.full.functions
import kotlin.test.Test

class ObsColChangeTests {

    @Test
    fun testWrappers() {


        with(systemScope(includePlatformClassloader = false).usingClassGraph()) {

            CollectionChange::class.mattSubClasses().filter { !it.isAbstract && !it.isSealed }.forEach {
                val classifier = it.functions.first { it.name == "convert" }.returnType.classifier
                if (classifier != it) {
                    println("$classifier is not $it")
                    error("error")
                }
            }
        }
    }
}

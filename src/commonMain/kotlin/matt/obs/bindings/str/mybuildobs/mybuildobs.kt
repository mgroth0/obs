package matt.obs.bindings.str.mybuildobs

import matt.obs.bind.MyBinding
import matt.obs.bindings.str.ObsS
import matt.obs.col.change.ListAdditionBase
import matt.obs.col.change.ListRemovalBase
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.ObsVal
import matt.obs.prop.writable.BindableProperty
import matt.prim.str.mybuild.api.StringDslLike


fun obsString(op: MyObsStringDSL.() -> Unit): ObsS = MyObsStringDSL().apply(op).stringO

class MyObsStringDSL : StringDslLike {

    private val delimiter = BindableProperty("")
    private val parts = basicMutableObservableListOf<ObsVal<*>>()


    private val stringM =
        MyBinding {
            parts.joinToString(separator = delimiter.value) { it.value.toString() }
        }
    val stringO: ObsS = stringM

    init {
        stringM.addDependency(delimiter)
        parts.onChange { c ->
            stringM.markInvalid()
            (c as? ListAdditionBase)?.addedElements?.forEach {
                stringM.addDependency(it)
            }
            (c as? ListRemovalBase)?.removedElements?.forEach {
                stringM.removeDependency(it)
            }
        }
    }

    fun append(a: ObsVal<*>) {
        parts += a
    }

    operator fun ObsVal<*>.unaryPlus() {
        append(this)
    }

    fun appendStatic(a: Any?) = append(BindableProperty(a))


    fun words(op: MyObsStringDSL.() -> Unit) = spaceDelimited(op)
    fun spaceDelimited(op: MyObsStringDSL.() -> Unit) {
        val subDSL = MyObsStringDSL()
        subDSL.delimiter.value = " "
        subDSL.apply(op)
        +subDSL.stringO
    }

    fun parenthesis(op: MyObsStringDSL.() -> Unit) {
        val subDSL = MyObsStringDSL()
        subDSL.apply(op)
        appendStatic("(")
        +subDSL.stringO
        appendStatic(")")
    }
}

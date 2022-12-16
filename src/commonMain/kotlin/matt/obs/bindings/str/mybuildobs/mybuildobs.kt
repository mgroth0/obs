package matt.obs.bindings.str.mybuildobs

import matt.obs.bind.MyBinding
import matt.obs.bindings.str.ObsS
import matt.obs.col.change.AdditionBase
import matt.obs.col.change.RemovalBase
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal
import matt.prim.str.mybuild.StringDSL


fun obsString(op: MyObsStringDSL.()->Unit): ObsS = MyObsStringDSL().apply(op).string

class MyObsStringDSL: StringDSL {

  private val delimiter = BindableProperty("")
  private val parts = basicMutableObservableListOf<ObsVal<*>>()


  private val stringM = MyBinding {
	parts.joinToString(separator = delimiter.value) { it.value.toString() }
  }
  val string: ObsS = stringM

  init {
	stringM.addDependency(delimiter)
	parts.onChange { c ->
	  stringM.markInvalid()
	  (c as? AdditionBase)?.addedElements?.forEach {
		stringM.addDependency(it)
	  }
	  (c as? RemovalBase)?.removedElements?.forEach {
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


  fun words(op: MyObsStringDSL.()->Unit) = spaceDelimited(op)
  fun spaceDelimited(op: MyObsStringDSL.()->Unit) {
	val subDSL = MyObsStringDSL()
	subDSL.delimiter.value = " "
	subDSL.apply(op)
	+subDSL.string
  }

  fun parenthesis(op: MyObsStringDSL.()->Unit) {
	val subDSL = MyObsStringDSL()
	subDSL.apply(op)
	appendStatic("(")
	+subDSL.string
	appendStatic(")")
  }

}

package matt.obs.prop.proxy

import matt.model.convert.Converter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.prop.BindableProperty
import matt.obs.prop.Var

class ProxyProp<S, C>(source: Var<S>, converter: Converter<S, C>):
  BindableProperty<C>(converter.convertToB(source.value)) {
  init {
	val rBlocker = RecursionBlocker()
	onChange {
	  rBlocker.with {
		source.value = converter.convertToA(it)
	  }
	}
	source.onChange {
	  rBlocker.with {
		value = converter.convertToB(it)
	  }
	}
  }
}
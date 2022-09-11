@file:Suppress("UNCHECKED_CAST")

package matt.obs.prop.cast

import matt.model.recursionblocker.RecursionBlocker
import matt.obs.prop.BindableProperty
import matt.obs.prop.WritableMObservableVal

class CastedWritableProp<S, C>(source: WritableMObservableVal<S>): BindableProperty<C>(source.value as C) {
  init {
	val rBlocker = RecursionBlocker()
	onChange {
	  rBlocker.with {
		@Suppress("UNCHECKED_CAST")
		source.value = it as S
	  }
	}
	source.onChange {
	  rBlocker.with {
		@Suppress("UNCHECKED_CAST")
		value = it as C
	  }
	}
  }
}
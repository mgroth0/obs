package matt.obs.prop.proxy

import matt.lang.convert.BiConverter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.bind.LazyBindableProp
import matt.obs.prop.writable.Var

class ProxyProp<S, C>(source: Var<S>, converter: BiConverter<S, C>):
    LazyBindableProp<C>({ converter.convertToB(source.value) }) {
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

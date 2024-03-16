
package matt.obs.prop.cast

import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.prop.writable.BindableProperty
import matt.obs.prop.writable.Var




class CastedWritableProp<S, C>(source: Var<S>, cast: (S) -> C, castBack: (C) -> S): BindableProperty<C>(cast(source.value)) {
    init {
        val rBlocker = RecursionBlocker()
        onChange {
            rBlocker.with {
                source.value = castBack(it)
            }
        }
        source.onChange {
            rBlocker.with {

                value = cast(it)
            }
        }
    }
}

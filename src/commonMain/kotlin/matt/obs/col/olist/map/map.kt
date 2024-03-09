package matt.obs.col.olist.map

import matt.obs.col.change.ClearSet
import matt.obs.col.change.MultiAddIntoSet
import matt.obs.col.change.RemoveElementsFromSet
import matt.obs.col.change.RetainAllSet
import matt.obs.col.change.SetAddition
import matt.obs.col.change.SetRemoval
import matt.obs.col.oset.ObsSet
import matt.obs.map.BasicOMap
import matt.obs.map.BasicOMutableMapImpl

fun <V, K> ObsSet<V>.toMappedMap(keySelectorFun: (V) -> K): BasicOMap<K, V> {



    val map = BasicOMutableMapImpl<K, V>()
    onChange {
        when (it) {
            is SetAddition           -> map[keySelectorFun(it.added)] = it.added
            is MultiAddIntoSet       -> it.addedElements.forEach { map[keySelectorFun(it)] }
            /* is Replacement           -> {
                 map[keySelectorFun(it.added)] = it.added
                 map.remove(keySelectorFun(it.removed))
             }*/

            is ClearSet              -> map.clear()
            is RemoveElementsFromSet ->
                it.removed.forEach {
                    map.remove(keySelectorFun(it))
                }

            is SetRemoval            -> map.remove(keySelectorFun(it.removed))

            /*is AtomicListChange      -> TODO("AtomicListChange")*/
            is RetainAllSet          -> TODO()
        }
    }
    return map
}

package matt.obs.col.olist.map

import matt.obs.col.change.Addition
import matt.obs.col.change.Clear
import matt.obs.col.change.MultiAddition
import matt.obs.col.change.MultiRemoval
import matt.obs.col.change.Removal
import matt.obs.col.change.Replacement
import matt.obs.col.olist.ObsList
import matt.obs.map.BasicOMap
import matt.obs.map.BasicOMutableMapImpl

fun <V, K> ObsList<V>.toMappedMap(keySelectorFun: (V)->K): BasicOMap<K, V> {
  /*WARNING/TODO: this must be limited to sets to work as expected...*/
  val map = BasicOMutableMapImpl<K, V>()
  onChange {
	when (it) {
	  is Addition      -> map[keySelectorFun(it.added)] = it.added
	  is MultiAddition -> it.added.forEach { map[keySelectorFun(it)] }
	  is Replacement   -> {
		map[keySelectorFun(it.added)] = it.added
		map.remove(keySelectorFun(it.removed))
	  }

	  is Clear         -> map.clear()
	  is MultiRemoval  -> it.removed.forEach {
		map.remove(keySelectorFun(it))
	  }

	  is Removal       -> map.remove(keySelectorFun(it.removed))
	}
  }
  return map
}

//class MappedMap<O, K>(
//  sourceList: BasicROObservableList<O>,
//  keySelectorFun: (O)->K,
//  map: MutableMap<K, O> = mutableMapOf()
//): Map<K, O> by map, BasicOCollection {
//  init {
//	map.clear()
//	map.putAll(sourceList.associateBy(keySelectorFun))
//	sourceList.onChange {
//	  while (it.next()) {
//		map.putAll(it.addedSubList.associateBy(keySelectorFun))
//		it.removed.forEach {
//		  map.remove(keySelectorFun(it))
//		}
//	  }
//	}
//  }
//}

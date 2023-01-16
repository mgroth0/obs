package matt.obs.col.olist.map

import matt.obs.col.change.ClearList
import matt.obs.col.change.ListAddition
import matt.obs.col.change.ListRemoval
import matt.obs.col.change.MultiAdditionIntoList
import matt.obs.col.change.MultiRemovalFromList
import matt.obs.col.change.Replacement
import matt.obs.col.olist.MutableObsList
import matt.obs.map.BasicOMap
import matt.obs.map.BasicOMutableMapImpl

fun <V, K> MutableObsList<V>.toMappedMap(keySelectorFun: (V)->K): BasicOMap<K, V> {
  /*WARNING/TODO: this must be limited to sets to work as expected...*/
  val map = BasicOMutableMapImpl<K, V>()
  onChange {
	when (it) {
	  is ListAddition          -> map[keySelectorFun(it.added)] = it.added
	  is MultiAdditionIntoList -> it.addedElements.forEach { map[keySelectorFun(it)] }
	  is Replacement           -> {
		map[keySelectorFun(it.added)] = it.added
		map.remove(keySelectorFun(it.removed))
	  }

	  is ClearList             -> map.clear()
	  is MultiRemovalFromList  -> it.removed.forEach {
		map.remove(keySelectorFun(it))
	  }

	  is ListRemoval           -> map.remove(keySelectorFun(it.removed))
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

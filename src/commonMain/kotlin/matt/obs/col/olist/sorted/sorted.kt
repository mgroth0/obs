package matt.obs.col.olist.sorted

import matt.lang.setAll
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.BasicROObservableList
import matt.obs.col.olist.basicROObservableListOf

private fun <E: Comparable<E>> BasicObservableListImpl<E>.getSortedFrom(blist: BasicROObservableList<E>): BasicObservableListImpl<E> {
  blist.onChange {
	setAll(blist.sorted())
  }
  return this
}

class BasicSortedList<E: Comparable<E>>(
  private val blist: BasicROObservableList<E> = basicROObservableListOf<E>(),
):
  BasicROObservableList<E> by BasicObservableListImpl(blist.sorted()).getSortedFrom(blist)
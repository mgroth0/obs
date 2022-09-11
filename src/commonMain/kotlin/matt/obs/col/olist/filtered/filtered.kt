package matt.obs.col.olist.filtered

import matt.lang.setAll
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.BasicROObservableList
import matt.obs.col.olist.basicROObservableListOf

class BasicFilteredList<E> private constructor(
  private val blist: BasicROObservableList<E> = basicROObservableListOf<E>(),
):
  BasicROObservableList<E> by blist {
  constructor(source: BasicROObservableList<E>, filter: (E)->Boolean): this(
	BasicObservableListImpl(source.filter(filter)).apply {
	  source.onChange {
		setAll(source.filter(filter))
	  }
	}
  )
}
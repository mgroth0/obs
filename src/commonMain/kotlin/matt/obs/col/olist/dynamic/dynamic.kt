package matt.obs.col.olist.dynamic

import matt.lang.setAll
import matt.obs.col.olist.BasicROObservableList
import matt.obs.col.olist.BasicWritableObservableList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.prop.BindableProperty

interface BasicFilteredList<E>: BasicROObservableList<E> {
  val filter: BindableProperty<((E)->Boolean)?>
}

interface BasicSortedList<E>: BasicROObservableList<E> {
  val comparator: BindableProperty<Comparator<E>?>
}

class DynamicList<E>(
  private val source: BasicROObservableList<E>,
  filter: ((E)->Boolean)? = null,
  comparator: Comparator<E>? = null,
  private val target: BasicWritableObservableList<E> = basicMutableObservableListOf()
): BasicROObservableList<E> by target, BasicFilteredList<E>, BasicSortedList<E> {


  override val filter = BindableProperty(filter)
  override val comparator = BindableProperty(comparator)

  private fun refresh() {
	target.setAll(source.filter { filter.value?.invoke(it) ?: true }.let {
	  val c = comparator.value
	  if (c != null) sortedWith(c)
	  else it
	})
  }

  init {
	refresh()
	this.filter.observe { refresh() }
	this.comparator.observe { refresh() }
	source.observe { refresh() }

  }
}
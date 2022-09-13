package matt.obs.col.olist.dynamic

import matt.lang.setAll
import matt.obs.MObservable
import matt.obs.col.olist.BasicROObservableList
import matt.obs.col.olist.BasicWritableObservableList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.CustomInvalidations
import matt.obs.invalid.DependencyHelper
import matt.obs.prop.BindableProperty

interface BasicFilteredList<E>: BasicROObservableList<E>, CustomInvalidations {
  val filter: BindableProperty<((E)->Boolean)?>
}

interface BasicSortedList<E>: BasicROObservableList<E>, CustomInvalidations {
  val comparator: BindableProperty<Comparator<in E>?>
}

class DynamicList<E>(
  private val source: BasicROObservableList<E>,
  filter: ((E)->Boolean)? = null,
  comparator: Comparator<in E>? = null,
  private val target: BasicWritableObservableList<E> = basicMutableObservableListOf()
): BasicROObservableList<E> by target, BasicFilteredList<E>, BasicSortedList<E>, CustomDependencies {


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

  fun addDependency(o: MObservable) {
	o.observe {
	  refresh()
	}
  }

  override fun markInvalid() {
	refresh()
  }

  private val dependencyHelper by lazy { DependencyHelper(this) }

  override fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable) =
	dependencyHelper.addDependency(o, *deepDependencies)

  override fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>) =
	dependencyHelper.addDependencyWithDeepList(o, deepDependencies)

  override fun removeDependency(o: MObservable) = dependencyHelper.removeDependency(o)


}
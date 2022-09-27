package matt.obs.col.olist.dynamic

import matt.collect.map.lazyMap
import matt.lang.setAll
import matt.model.tostringbuilder.toStringBuilder
import matt.obs.MObservable
import matt.obs.bindings.bool.ObsB
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.ObsList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.DependencyHelper
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal

interface BasicFilteredList<E>: ObsList<E>, CustomDependencies, List<E> {
  val predicate: BindableProperty<((E)->Boolean)?>
}

interface BasicSortedList<E>: ObsList<E>, CustomDependencies, List<E> {
  val comparator: BindableProperty<Comparator<in E>?>
}

class DynamicList<E>(
  private val source: ObsList<E>,
  filter: ((E)->Boolean)? = null,
  private val dynamicFilter: ((E)->ObsB)? = null,
  comparator: Comparator<in E>? = null,
  private val target: MutableObsList<E> = basicMutableObservableListOf()
): ObsList<E> by target, BasicFilteredList<E>, BasicSortedList<E>, CustomDependencies {


  override fun toString() = toStringBuilder()

  override val predicate = BindableProperty(filter)
  override val comparator = BindableProperty(comparator)

  /*TODO: this probably needs weak refrences and other fancy techniques or its likely going to be a memory/cpu leak in multiple ways*/
  private val dynamicPredicates = dynamicFilter?.let { filtOp ->
	lazyMap<E, ObsB> {
	  filtOp(it).apply {
		observe {
		  if (it in this@DynamicList.source) this@DynamicList.refresh()
		}
	  }
	}
  }

  private fun refresh() {
	require(predicate.value == null || dynamicFilter == null)
	target.setAll(source.filter { predicate.value?.invoke(it) ?: dynamicPredicates?.get(it)?.value ?: true }.let {
	  val c = comparator.value
	  if (c != null) sortedWith(c)
	  else it
	})
  }


  init {
	refresh()
	this.predicate.observe { refresh() }
	this.comparator.observe { refresh() }
	source.observe { refresh() }.apply {
	  name = "listener for ${this@DynamicList}"
	}
  }

  override fun markInvalid() {
	refresh()
  }

  private val dependencyHelper by lazy { DependencyHelper(this) }

  override fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable?) =
	dependencyHelper.addDependency(o, *deepDependencies)

  override fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>) =
	dependencyHelper.addDependencyWithDeepList(o, deepDependencies)

  override fun <O: ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
	o: O,
	vararg deepDependencies: (O)->MObservable?
  ) {
	return dependencyHelper.addDependencyIgnoringFutureNullOuterChanges(o, *deepDependencies)
  }

  override fun removeDependency(o: MObservable) = dependencyHelper.removeDependency(o)


}
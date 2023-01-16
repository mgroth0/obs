package matt.obs.col.olist.dynamic

import matt.collect.map.lazyMap
import matt.lang.setall.setAllOneByOneNeverAllowingDuplicates
import matt.model.obj.tostringbuilder.toStringBuilder
import matt.model.op.debug.DebugLogger
import matt.obs.MObservable
import matt.obs.bindings.bool.ObsB
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.DependencyHelper
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal

interface CalculatedList<E>: ImmutableObsList<E> {
  fun refresh()
}

interface BasicFilteredList<E>: CalculatedList<E>, CustomDependencies, List<E> {
  val predicate: BindableProperty<((E)->Boolean)?>
}

interface BasicSortedList<E>: CalculatedList<E>, CustomDependencies, List<E> {
  val comparator: BindableProperty<Comparator<in E>?>
}

class DynamicList<E>(
  private val source: ImmutableObsList<E>,
  filter: ((E)->Boolean)? = null,
  private val dynamicFilter: ((E)->ObsB)? = null,
  comparator: Comparator<in E>? = null,
  private val target: MutableObsList<E> = basicMutableObservableListOf()
): ImmutableObsList<E> by target, BasicFilteredList<E>, BasicSortedList<E>, CustomDependencies {


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

  override fun refresh() {
	require(predicate.value == null || dynamicFilter == null)


	target.atomicChange {
	  println("starting atomic change of ${this@DynamicList}")


	  /*THERE MUST NEVER BE DUPLICATES GOING TO NODE LISTS
	  *
	  * 1. NOT ONLY DOES JAVAFX THROW A BUG FOR ADDING DUPLICATE NODES
	  * 2. BUT ALSO, IF YOU "MOVE" A NODE BY ADDING IT SOMEWHERE THEN REMOVING IT SOMEWHERE ELSE, THE CODE COULD INTERPRET THE FINAL ACTION AS IT BEING REMOVED
	  *
	  * */
	  setAllOneByOneNeverAllowingDuplicates(
		this@DynamicList.source
		  .filter {
			this@DynamicList.predicate.value?.invoke(it)
			?: this@DynamicList.dynamicPredicates?.get(it)?.value ?: true
		  }
		  .let {
			val c = this@DynamicList.comparator.value
			if (c != null) sortedWith(c)
			else it
		  }
	  )
	  println("finishing atomic change of ${this@DynamicList}")
	}


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

  override fun <O: MObservable> addDependency(
	mainDep: O,
	moreDeps: List<MObservable>,
	debugLogger: DebugLogger?,
	vararg deepDependencies: (O)->MObservable?
  ) =
	dependencyHelper.addDependency(mainDep = mainDep, moreDeps = moreDeps, debugLogger = debugLogger, *deepDependencies)

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
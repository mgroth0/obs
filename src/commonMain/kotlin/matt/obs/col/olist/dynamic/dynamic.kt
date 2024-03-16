package matt.obs.col.olist.dynamic

import matt.obs.col.olist.ImmutableObsList
import matt.obs.invalid.CustomDependencies
import matt.obs.prop.writable.BindableProperty

interface InterestingList

interface CalculatedList<E> : ImmutableObsList<E> {
    fun refresh()
}

interface CustomDependenciesList<E>: List<E>, CustomDependencies

interface CalculatedCustomDependenciesList<E>: CalculatedList<E>, CustomDependenciesList<E>

interface BasicFilteredList<E> : CalculatedCustomDependenciesList<E> {
    val predicate: BindableProperty<((E) -> Boolean)?>
}

interface BasicSortedList<E> : CalculatedCustomDependenciesList<E> {
    val comparator: BindableProperty<Comparator<in E>?>
}


interface SortedFilteredList<E>: BasicFilteredList<E>, BasicSortedList<E>

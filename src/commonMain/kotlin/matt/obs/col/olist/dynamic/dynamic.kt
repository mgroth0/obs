package matt.obs.col.olist.dynamic

import matt.obs.col.olist.ImmutableObsList
import matt.obs.invalid.CustomDependencies
import matt.obs.prop.writable.BindableProperty

interface InterestingList

interface CalculatedList<E> : ImmutableObsList<E> {
    fun refresh()
}

interface BasicFilteredList<E> : CalculatedList<E>, CustomDependencies, List<E> {
    val predicate: BindableProperty<((E) -> Boolean)?>
}

interface BasicSortedList<E> : CalculatedList<E>, CustomDependencies, List<E> {
    val comparator: BindableProperty<Comparator<in E>?>
}


package matt.obs.col.olist.dynamic

import matt.collect.list.setAllOneByOneNeverAllowingDuplicates
import matt.lang.model.value.Value
import matt.lang.tostring.mehToStringBuilder
import matt.lang.weak.MyWeakRef
import matt.model.op.debug.DebugLogger
import matt.obs.MObservable
import matt.obs.bindings.bool.ObsB
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.invalid.CustomDependencies
import matt.obs.invalid.DependencyHelper
import matt.obs.listen.MyListenerInter
import matt.obs.prop.BindableProperty
import matt.obs.prop.ObsVal

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

class DynamicList<E>(
    private val source: ImmutableObsList<E>,
    filter: ((E) -> Boolean)? = null,
    private val dynamicFilter: ((E) -> ObsB)? = null,
    comparator: Comparator<in E>? = null,
    private val target: MutableObsList<E> = basicMutableObservableListOf()
) : ImmutableObsList<E> by target, BasicFilteredList<E>, BasicSortedList<E>, CustomDependencies, InterestingList {


    override fun toString() = mehToStringBuilder("name" to nam)

    override val predicate = BindableProperty(filter)
    override val comparator = BindableProperty(comparator)


    /*needs so much work to avoid CPU and memory issues*/
    /*small issue: full refresh is excessive*/
    /*big issue: are more weak references needed?*/
    private val dynamicPredicates = dynamicFilter?.let { _ ->
        require(filter == null)
        mutableMapOf<E, ObsB>()
    }

    private fun newDynamicPredicate(element: E): ObsB {
        require(predicate.value == null)
        val bProp = dynamicFilter!!(element)
        return bProp.apply {
            var listener: MyListenerInter<*>? = null
            listener = onChangeWithWeak(Value(element)) { weakElement, _ ->
                val e = weakElement.value
                if (e in this@DynamicList.source) {
                    this@DynamicList.refresh()
                } else {
                    listener!!.removeListener()
                    this@DynamicList.dynamicPredicates!!.remove(e)
                }
            }
        }
    }

//    private val cachedPredValues = WeakMap<E, Boolean>()

    override fun refresh() {
        val predValue = predicate.value
        require(predValue == null || dynamicFilter == null)
        val pred: (E) -> Boolean = when {
            predValue != null -> {
                {
                    predValue(it)
                }
            }

            dynamicFilter != null -> {
                {
                    dynamicPredicates!!.getOrPut(it) { newDynamicPredicate(it) }.value
                }
            }

            else -> {
                { true }
            }
        }
        /*when nodes move in node children lists in javafx, it must be atomic*/
        target.atomicChange {
            setAllOneByOneNeverAllowingDuplicates(
                this@DynamicList.source
                    .filter(pred)
                    .let {
                        val c = this@DynamicList.comparator.value
                        if (c != null) sortedWith(c)
                        else it
                    }
            )
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

    override fun <O : MObservable> addDependency(
        mainDep: O,
        moreDeps: List<MObservable>?,
        debugLogger: DebugLogger?,
        vararg deepDependencies: (O) -> MObservable?
    ) =
        dependencyHelper.addDependency(
            mainDep = mainDep,
            moreDeps = moreDeps,
            debugLogger = debugLogger,
            *deepDependencies
        )

    override fun <O : MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O) -> List<MObservable>) =
        dependencyHelper.addDependencyWithDeepList(o, deepDependencies)

    override fun <O : ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
        o: O,
        vararg deepDependencies: (O) -> MObservable?
    ) {
        return dependencyHelper.addDependencyIgnoringFutureNullOuterChanges(o, *deepDependencies)
    }

    override fun removeDependency(o: MObservable) = dependencyHelper.removeDependency(o)

    override fun <O : MObservable> addWeakDependency(
        weakRef: MyWeakRef<*>,
        mainDep: O,
        moreDeps: List<MObservable>?,
        debugLogger: DebugLogger?,
        vararg deepDependencies: (O) -> MObservable?
    ) = dependencyHelper.addWeakDependency(
        weakRef = weakRef,
        mainDep = mainDep,
        moreDeps = moreDeps,
        debugLogger = debugLogger,
        *deepDependencies
    )


}
package matt.obs.hold

import matt.collect.itr.filterNotNull
import matt.lang.anno.Open
import matt.lang.common.go
import matt.lang.delegation.provider
import matt.lang.setall.setAll
import matt.lang.tostring.SimpleStringableClass
import matt.lang.weak.common.WeakRefInter
import matt.model.code.delegate.SimpleGetter
import matt.model.op.debug.DebugLogger
import matt.model.op.prints.Prints
import matt.obs.col.change.ListChange
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.col.olist.basicROObservableListOf
import matt.obs.col.oset.basicImmutableObservableSetOf
import matt.obs.col.oset.basicObservableSetOf
import matt.obs.common.MObservable
import matt.obs.listen.MyListenerInter
import matt.obs.listen.ObsHolderListener
import matt.obs.prop.typed.TypedBindableProperty
import matt.obs.prop.typed.TypedImmutableObsList
import matt.obs.prop.typed.TypedImmutableObsSet
import matt.obs.prop.typed.TypedMutableObsList
import matt.obs.prop.typed.TypedMutableObsSet
import matt.obs.prop.typed.TypedSerializableElement
import matt.obs.prop.writable.BindableProperty

interface ObservableStructIdea


interface MObsHolder<O : MObservable> : MObservable, ObservableStructIdea {
    val observables: List<O>

    @Open
    override fun observe(op: () -> Unit) =
        ObsHolderListener().apply {
            subListeners.setAll(observables.map { it.observe(op) })
        }

    @Open
    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*> =
        ObsHolderListener().apply {
            subListeners.setAll(observables.map { it.observeWeakly(w, op) })
        }

    @Open
    override fun removeListener(listener: MyListenerInter<*>) {
        (listener as? ObsHolderListener)?.subListeners?.forEach { it.tryRemovingListener() }
    }
}


interface NamedObsHolder<O : MObservable> : MObsHolder<O> {
    abstract fun namedObservables(): Map<String, O>

    @Open
    override val observables get() = namedObservables().values.toList()
}


sealed class ObservableHolderImplBase<O : MObservable> : SimpleStringableClass(), NamedObsHolder<O> {
    final override var nam: String? = null

    @PublishedApi
    internal val _observables = mutableMapOf<String, O>()
    final override fun namedObservables(): Map<String, O> = _observables
    final override var debugger: Prints?
        get() =
            observables.map { it.debugger }.filterNotNull().let {
                if (it.isEmpty()) null
                else DebugLogger("for ${it.size} observables")
            }
        set(value) {
            observables.forEach {
                it.debugger = value
            }
        }
}

open class ObservableObjectHolder<T> : ObservableHolderImplBase<BindableProperty<T>>() {

    fun registeredProp(defaultValue: T) =
        provider {
            val fx = BindableProperty(defaultValue)
            _observables[it] = fx
            SimpleGetter(fx)
        }
}


open class ObservableHolderImpl : ObservableHolderImplBase<MObservable>() {

    fun <T> registeredProp(
        defaultValue: T,
        onChange: (() -> Unit)? = null
    ) = provider {
        val fx = BindableProperty(defaultValue)
        onChange?.go { listener ->
            fx.onChange { listener() }
        }
        _observables[it] = fx
        SimpleGetter(fx)
    }


    fun <E> registeredMutableList(
        vararg default: E,
        listener: ((ListChange<E>) -> Unit)? = null
    ) = provider {
        val fx =
            basicMutableObservableListOf(*default).also {
                if (listener != null) it.onChange {
                    listener.invoke(it)
                }
            }
        _observables[it] = fx
        SimpleGetter(fx)
    }

    fun <E> registeredList(
        vararg default: E,
        listener: ((ListChange<E>) -> Unit)? = null
    ) = provider {
        val fx =
            basicROObservableListOf(*default).also {
                if (listener != null) it.onChange {
                    listener.invoke(it)
                }
            }
        _observables[it] = fx
        SimpleGetter(fx)
    }
}


open class TypedObservableHolder : ObservableHolderImplBase<TypedSerializableElement<*>>() {

    var wasResetBecauseSerializedDataWasWrongClassVersion = false

    private val sectionsM = mutableListOf<TypedObservableHolder>()
    val sections: List<TypedObservableHolder> = sectionsM

    inline fun <reified T> registeredProp(
        defaultValue: T,
        noinline onChange: (() -> Unit)? = null
    ) = provider {
        val fx = TypedBindableProperty(T::class, nullable = null is T, value = defaultValue)
        onChange?.go { listener ->
            fx.onChange { listener() }
        }
        _observables[it] = fx
        postRegister(fx)
        SimpleGetter(fx)
    }


    fun <T : TypedObservableHolder> registeredSection(obsHolder: T) =
        provider { sectionName ->
            sectionsM += obsHolder
            val sectionProps = obsHolder.namedObservables().mapKeys { sectionName + "." + it.key }
            sectionProps.forEach {
                _observables[it.key] = it.value
                postRegister(it.value)
            }
            SimpleGetter(obsHolder)
        }


    inline fun <reified E> registeredMutableList(vararg default: E) =
        provider {
            val list = basicMutableObservableListOf(*default)
            val fx = TypedMutableObsList(elementCls = E::class, nullableElements = null is E, list = list)
            _observables[it] = fx
            postRegister(fx)
            SimpleGetter(fx)
        }

    inline fun <reified E> registeredList(vararg default: E) =
        provider {
            val list = basicROObservableListOf(*default)
            val fx = TypedImmutableObsList(elementCls = E::class, nullableElements = null is E, list = list)
            _observables[it] = fx
            postRegister(fx)
            SimpleGetter(fx)
        }

    inline fun <reified E> registeredSet(vararg default: E) =
        provider {
            val set = basicImmutableObservableSetOf(*default)
            val fx = TypedImmutableObsSet(elementCls = E::class, nullableElements = null is E, set = set)
            _observables[it] = fx
            postRegister(fx)
            SimpleGetter(fx)
        }

    inline fun <reified E> registeredMutableSet(vararg default: E) =
        provider {
            val set = basicObservableSetOf(*default)
            val fx = TypedMutableObsSet(elementCls = E::class, nullableElements = null is E, set = set)
            _observables[it] = fx
            postRegister(fx)
            SimpleGetter(fx)
        }


    open fun postRegister(prop: MObservable) = Unit
}





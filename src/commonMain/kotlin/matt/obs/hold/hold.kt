package matt.obs.hold

import matt.collect.itr.filterNotNull
import matt.lang.delegation.provider
import matt.lang.go
import matt.lang.setAll
import matt.model.debug.DebugLogger
import matt.model.delegate.SimpleGetter
import matt.obs.MObservable
import matt.obs.col.change.CollectionChange
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.col.olist.basicROObservableListOf
import matt.obs.listen.Listener
import matt.obs.listen.ObsHolderListener
import matt.obs.prop.BindableProperty
import matt.obs.prop.typed.TypedBindableProperty

interface MObsHolder<O: MObservable>: MObservable {
  val observables: List<O>

  override fun observe(op: ()->Unit) = ObsHolderListener().apply {
	subListeners.setAll(observables.map { it.observe(op) })
  }

  override fun removeListener(listener: Listener) {
	(listener as? ObsHolderListener)?.subListeners?.forEach { it.tryRemovingListener() }
  }
}


interface NamedObsHolder<O: MObservable>: MObsHolder<O> {
  fun namedObservables(): Map<String, O>
  override val observables get() = namedObservables().values.toList()
}


sealed class ObservableHolderImplBase<O: MObservable>: NamedObsHolder<O> {
  override var nam: String? = null

  @PublishedApi
  internal val _observables = mutableMapOf<String, O>()
  final override fun namedObservables(): Map<String, O> = _observables
  final override var debugger: DebugLogger?
	get() = observables.map { it.debugger }.filterNotNull().let {
	  if (it.isEmpty()) null
	  else DebugLogger("for ${it.size} observables")
	}
	set(value) {
	  observables.forEach {
		it.debugger = value
	  }
	}
}

open class ObservableObjectHolder<T>: ObservableHolderImplBase<BindableProperty<T>>() {

  fun registeredProp(defaultValue: T) = provider {
	val fx = BindableProperty(defaultValue)
	_observables[it] = fx
	SimpleGetter(fx)
  }
}


open class ObservableHolderImpl: ObservableHolderImplBase<MObservable>() {

  fun <T> registeredProp(defaultValue: T, onChange: (()->Unit)? = null) = provider {
	val fx = BindableProperty(defaultValue)
	onChange?.go { listener ->
	  fx.onChange { listener() }
	}
	_observables[it] = fx
	SimpleGetter(fx)
  }


  fun <E> registeredMutableList(vararg default: E, listener: ((CollectionChange<E>)->Unit)? = null) = provider {
	val fx = basicMutableObservableListOf(*default).also {
	  if (listener != null) it.onChange {
		listener.invoke(it)
	  }
	}
	_observables[it] = fx
	SimpleGetter(fx)
  }

  fun <E> registeredList(vararg default: E, listener: ((CollectionChange<E>)->Unit)? = null) = provider {
	val fx = basicROObservableListOf(*default).also {
	  if (listener != null) it.onChange {
		listener.invoke(it)
	  }
	}
	_observables[it] = fx
	SimpleGetter(fx)
  }

}



open class TypedObservableHolder: ObservableHolderImplBase<TypedBindableProperty<*>>() {
  inline fun <reified T> registeredProp(defaultValue: T, noinline onChange: (()->Unit)? = null) = provider {
	val fx = TypedBindableProperty(T::class, nullable = null is T, value = defaultValue)
	onChange?.go { listener ->
	  fx.onChange { listener() }
	}
	_observables[it] = fx
	SimpleGetter(fx)
  }
}

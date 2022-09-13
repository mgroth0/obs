package matt.obs.hold

import matt.lang.go
import matt.lang.setAll
import matt.model.delegate.SimpleGetter
import matt.obs.MObservable
import matt.obs.col.change.CollectionChange
import matt.obs.col.olist.ObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.col.olist.basicROObservableListOf
import matt.obs.listen.MyListener
import matt.obs.listen.ObsHolderListener
import matt.obs.prop.BindableProperty
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KProperty

interface MObsHolder<O: MObservable>: MObservable {
  val observables: List<O>

  override fun observe(op: ()->Unit) = ObsHolderListener().apply {
	subListeners.setAll(observables.map { it.observe(op) })
  }

  override fun removeListener(listener: MyListener<*>): Boolean {
	return (listener as? ObsHolderListener)?.subListeners?.map { it.tryRemovingListener() }?.any { it } ?: false
  }
}


interface NamedObsHolder<O: MObservable>: MObsHolder<O> {
  fun namedObservables(): Map<String, O>
  override val observables get() = namedObservables().values.toList()
}


sealed class ObservableHolderImplBase<O: MObservable>: NamedObsHolder<O> {
  protected val _observables = mutableMapOf<String, O>()
  override fun namedObservables(): Map<String, O> = _observables
}

open class ObservableObjectHolder<T>: ObservableHolderImplBase<BindableProperty<T>>() {
  inner class RegisteredProp(private val defaultValue: T) {
	operator fun provideDelegate(
	  thisRef: ObservableObjectHolder<T>, prop: KProperty<*>
	): SimpleGetter<Any, BindableProperty<T>> {
	  val fx = BindableProperty(defaultValue)
	  _observables[prop.name] = fx
	  return SimpleGetter(fx)
	}
  }
}


open class ObservableHolderImpl: ObservableHolderImplBase<MObservable>() {
  @OptIn(ExperimentalContracts::class)
  inner class RegisteredProp<T>(private val defaultValue: T, private val onChange: (()->Unit)? = null) {
	operator fun provideDelegate(
	  thisRef: ObservableHolderImpl,
	  prop: KProperty<*>,
	): SimpleGetter<Any, BindableProperty<T>> {
	  val fx = BindableProperty(defaultValue) // .provideDelegate(thisRef, prop)
	  onChange?.go { listener ->
		fx.onChange { listener() }
	  }
	  _observables[prop.name] = fx    //	  _properties.add(fx.observable)
	  return SimpleGetter(fx)
	}
  }

  inner class RegisteredMutableList<E: Any>(private vararg val default: E, private val listener: ((CollectionChange<E>)->Unit)? = null) {
	operator fun provideDelegate(
	  thisRef: ObservableHolderImpl, prop: KProperty<*>
	): SimpleGetter<Any, MutableObsList<E>> {
	  val fx = basicMutableObservableListOf(*default).also {
		if (listener != null) it.onChange {
		  listener.invoke(it)
		}
	  }
	  _observables[prop.name] = fx
	  return SimpleGetter(fx)
	}
  }


  inner class RegisteredList<E: Any>(private vararg val default: E, private val listener: (()->Unit)? = null) {
	operator fun provideDelegate(
	  thisRef: ObservableHolderImpl, prop: KProperty<*>
	): SimpleGetter<Any, ObsList<E>> {
	  val fx = basicROObservableListOf(*default).also {
		if (listener != null) it.onChange {
		  listener.invoke()
		}
	  }
	  _observables[prop.name] = fx
	  return SimpleGetter(fx)
	}
  }
}
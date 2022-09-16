package matt.obs.invalid

import matt.obs.MObservable
import matt.obs.listen.Listener
import matt.obs.prop.ObsVal
import kotlin.jvm.Synchronized

interface CustomInvalidations: MObservable {
  fun markInvalid()
}

interface CustomDependencies: CustomInvalidations {

  fun addDependencies(vararg obs: MObservable) {
	obs.forEach {
	  addDependency(it)
	}
  }

  fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable?)
  fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>)
  fun <O: ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(o: O, vararg deepDependencies: (O)->MObservable?)
  fun removeDependency(o: MObservable)


}


class DependencyHelper(main: CustomInvalidations): CustomDependencies,
												   CustomInvalidations by main {
  private val deps = mutableListOf<DepListenerSet>()

  @Synchronized override fun <O: MObservable> addDependency(
	o: O,
	vararg deepDependencies: (O)->MObservable?
  ) {
	deps += DepListenerSet(
	  obs = this,
	  dep = o,
	  getDeepDeps = { deepDependencies.mapNotNull { it.invoke(o) } },
	  subListeners = deepDependencies.mapNotNull {
		it(o)?.observe {
		  markInvalid()
		}.apply {
		  this?.name = "subListener1"
		}
	  }
	)
  }

  @Synchronized override fun <O: MObservable> addDependencyWithDeepList(
	o: O,
	deepDependencies: (O)->List<MObservable>
  ) {

	deps += DepListenerSet(
	  obs = this,
	  dep = o,
	  getDeepDeps = { deepDependencies.invoke(o) },
	  subListeners = deepDependencies(o).map {
		it.observe {
		  markInvalid()
		}.apply {
		  this.name = "subListener2"
		}
	  }
	)
  }


  @Synchronized override fun <O: ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
	o: O,
	vararg deepDependencies: (O)->MObservable?
  ) {
	deps += DepListenerSet(
	  obs = this,
	  dep = o,
	  getDeepDeps = { deepDependencies.mapNotNull { it.invoke(o) } },
	  subListeners = deepDependencies.mapNotNull {
		it(o)?.observe {
		  markInvalid()
		}
	  }
	)
  }

  @Synchronized override fun removeDependency(o: MObservable) {
	deps.filter { it.obs == o }.toList().forEach {
	  removeListener(it.mainListener)
	  it.subListeners.forEach {
		removeListener(it)
	  }
	  deps.remove(it)
	}
  }
}

private open class DepListenerSet(
  val obs: CustomInvalidations,
  open val dep: MObservable,
  getDeepDeps: (dep: MObservable)->List<MObservable>,
  var subListeners: List<Listener>
) {
  open val mainListener = dep.observe {
	obs.markInvalid()
	subListeners.forEach { it.tryRemovingListener() }
	subListeners = getDeepDeps(dep).map {
	  it.observe {
		obs.markInvalid()
	  }
	}
  }.apply {
	name = "${DepListenerSet::class.simpleName} mainListener"
  }
}

private class DepListenerSetIgnoringNullOuterValues(
  obs: CustomInvalidations,
  override val dep: ObsVal<*>,
  getDeepDeps: (dep: MObservable)->List<MObservable>,
  subListeners: List<Listener>
): DepListenerSet(obs, dep, getDeepDeps, subListeners) {
  override val mainListener = dep.onChange {
	if (it != null) {
	  obs.markInvalid()
	  this.subListeners.forEach { it.tryRemovingListener() }
	  this.subListeners = getDeepDeps(dep).map {
		it.observe {
		  obs.markInvalid()
		}
	  }
	}
  }
}
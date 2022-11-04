package matt.obs.invalid

import matt.model.debug.DebugLogger
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


  fun <O: MObservable> addDependency(
	mainDep: O,
	moreDeps: List<MObservable> = EMPTY_MORE_DEPS_LIST,
	debugLogger: DebugLogger? = null,
	vararg deepDependencies: (O)->MObservable?
  )

  fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>)
  fun <O: ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(o: O, vararg deepDependencies: (O)->MObservable?)
  fun removeDependency(o: MObservable)

}

private val EMPTY_MORE_DEPS_LIST by lazy { listOf<MObservable>() }


class DependencyHelper(main: CustomInvalidations): CustomDependencies,
												   CustomInvalidations by main {
  private val deps = mutableListOf<DepListenerSet>()



  @Synchronized override fun <O: MObservable> addDependency(
	mainDep: O,
	moreDeps: List<MObservable>,
	debugLogger: DebugLogger?,
	vararg deepDependencies: (O)->MObservable?
  ) {
	deps += DepListenerSet(
	  obs = this,
	  mainDep = mainDep,
	  moreDeps = moreDeps,
	  getDeepDeps = { deepDependencies.mapNotNull { it.invoke(mainDep) } },
	  subListeners = deepDependencies.mapNotNull {
		it(mainDep)?.observe {
		  markInvalid()
		}.apply {
		  this?.name = "subListener1"
		}
	  },
	  debugLogger = debugLogger
	)
  }

  @Synchronized override fun <O: MObservable> addDependencyWithDeepList(
	o: O,
	deepDependencies: (O)->List<MObservable>
  ) {

	deps += DepListenerSet(
	  obs = this,
	  mainDep = o,
	  moreDeps = EMPTY_MORE_DEPS_LIST,
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
	  mainDep = o,
	  moreDeps = EMPTY_MORE_DEPS_LIST,
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
	  it.mainListeners.forEach {
		removeListener(it)
	  }
	  it.subListeners.forEach {
		removeListener(it)
	  }
	  deps.remove(it)
	}
  }
}

private open class DepListenerSet(
  val obs: CustomInvalidations,
  open val mainDep: MObservable,
  val moreDeps: List<MObservable>,
  getDeepDeps: (dep: MObservable)->List<MObservable>,
  var subListeners: List<Listener>,
  val debugLogger: DebugLogger? = null
) {
  open val mainListeners = (listOf(mainDep) + moreDeps).mapIndexed { index, it ->
	it.observe {
	  obs.markInvalid()
	  subListeners.forEach { it.tryRemovingListener() }
	  subListeners = getDeepDeps(mainDep).map {
		it.observe {
		  obs.markInvalid()
		}
	  }
	}.apply {
	  name = "${DepListenerSet::class.simpleName} mainListener $index"
	}
  }
}

private class DepListenerSetIgnoringNullOuterValues(
  obs: CustomInvalidations,
  override val mainDep: ObsVal<*>,
  getDeepDeps: (dep: MObservable)->List<MObservable>,
  subListeners: List<Listener>
): DepListenerSet(obs, mainDep = mainDep, moreDeps = EMPTY_MORE_DEPS_LIST, getDeepDeps, subListeners) {
  override val mainListeners = listOf(mainDep.onChange {
	if (it != null) {
	  obs.markInvalid()
	  this.subListeners.forEach { it.tryRemovingListener() }
	  this.subListeners = getDeepDeps(mainDep).map {
		it.observe {
		  obs.markInvalid()
		}
	  }
	}
  })
}
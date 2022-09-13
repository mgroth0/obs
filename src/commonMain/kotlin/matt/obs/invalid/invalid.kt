package matt.obs.invalid

import matt.lang.go
import matt.obs.MObservable
import matt.obs.listen.Listener
import kotlin.contracts.ExperimentalContracts
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

  fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable)
  fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>)
  fun removeDependency(o: MObservable)


}

@OptIn(ExperimentalContracts::class) class DependencyHelper(main: CustomInvalidations): CustomDependencies,
																						CustomInvalidations by main {
  private val deps = mutableMapOf<MObservable, Listener>()
  private val subDeps = mutableMapOf<MObservable, List<Listener>>()

  @Synchronized override fun <O: MObservable> addDependency(o: O, vararg deepDependencies: (O)->MObservable) {
	require(o !in deps)
	subDeps[o] = deepDependencies.map {
	  it(o).observe {
		markInvalid()
	  }
	}
	deps[o] = o.observe {
	  subEvent(o, deepDependencies.map { it(o) })
	}
  }

  @Synchronized override fun <O: MObservable> addDependencyWithDeepList(o: O, deepDependencies: (O)->List<MObservable>) {
	require(o !in deps)
	subDeps[o] = deepDependencies(o).map {
	  it.observe {
		markInvalid()
	  }
	}
	deps[o] = o.observe {
	  subEvent(o, deepDependencies(o))
	}
  }

  @Synchronized private fun <O: MObservable> subEvent(o: O, deepDependencies: List<MObservable>) {
	markInvalid()
	subDeps[o]!!.forEach { it.tryRemovingListener() }
	subDeps[o] = deepDependencies.map {
	  it.observe {
		markInvalid()
	  }
	}
  }

  @Synchronized override fun removeDependency(o: MObservable) {
	deps.remove(o)?.go { l ->
	  removeListener(l)
	  subDeps[o]!!.forEach {
		it.tryRemovingListener()
	  }
	}
  }
}
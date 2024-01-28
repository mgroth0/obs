package matt.obs.invalid

import matt.collect.list.single.SingleElementListImpl
import matt.lang.anno.Open
import matt.lang.function.Produce
import matt.lang.sync.ReferenceMonitor
import matt.lang.sync.inSync
import matt.lang.weak.WeakRefInter
import matt.model.op.debug.DebugLogger
import matt.obs.MObservable
import matt.obs.listen.Listener
import matt.obs.listen.MyListenerInter
import matt.obs.prop.ObsVal

interface CustomInvalidations : MObservable {
    fun markInvalid()
}

interface CustomDependencies : CustomInvalidations {

    @Open
    fun addDependencies(vararg obs: MObservable) {
        obs.forEach {
            addDependency(it)
        }
    }

    @Open
    fun addWeakDependencies(
        w: WeakRefInter<*>,
        vararg obs: MObservable
    ) {
        obs.forEach {
            addWeakDependency(w, it)
        }
    }


    fun <O : MObservable> addDependency(
        mainDep: O,
        moreDeps: List<MObservable>? = null,
        debugLogger: DebugLogger? = null,
        vararg deepDependencies: (O) -> MObservable?
    )

    fun <O : MObservable> addWeakDependency(
        weakRef: WeakRefInter<*>,
        mainDep: O,
        moreDeps: List<MObservable>? = null,
        debugLogger: DebugLogger? = null,
        vararg deepDependencies: (O) -> MObservable?
    )

    fun <O : MObservable> addDependencyWithDeepList(
        o: O,
        deepDependencies: (O) -> List<MObservable>
    )

    fun <O : ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
        o: O,
        vararg deepDependencies: (O) -> MObservable?
    )

    fun removeDependency(o: MObservable)

}


class DependencyHelper(main: CustomInvalidations) : CustomDependencies,
    CustomInvalidations by main, ReferenceMonitor {


    private val deps = mutableListOf<DepListenerSet>()

    override fun <O : MObservable> addWeakDependency(
        weakRef: WeakRefInter<*>,
        mainDep: O,
        moreDeps: List<MObservable>?,
        debugLogger: DebugLogger?,
        vararg deepDependencies: (O) -> MObservable?
    ) = inSync {
        deps += WeakDepListenerSet(
            obs = this,
            weakRef = weakRef,
            mainDep = mainDep,
            moreDeps = moreDeps,
            getDeepDeps = deepDependencies.takeIf { it.isNotEmpty() }
                ?.let { { deepDependencies.mapNotNull { it.invoke(mainDep) } } },
            subListeners = deepDependencies.mapNotNull {
                it(mainDep)?.observeWeakly(weakRef) {
                    markInvalid()
                }.apply {
                    this?.name = "subListener1"
                }
            },
            debugLogger = debugLogger
        )
    }

    override fun <O : MObservable> addDependency(
        mainDep: O,
        moreDeps: List<MObservable>?,
        debugLogger: DebugLogger?,
        vararg deepDependencies: (O) -> MObservable?
    ) = inSync {
        deps += DefaultDepListenerSet(
            obs = this,
            mainDep = mainDep,
            moreDeps = moreDeps,
            getDeepDeps = deepDependencies.takeIf { it.isNotEmpty() }
                ?.let { { deepDependencies.mapNotNull { it.invoke(mainDep) } } },
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

    override fun <O : MObservable> addDependencyWithDeepList(
        o: O,
        deepDependencies: (O) -> List<MObservable>
    ) = inSync {

        deps += DefaultDepListenerSet(
            obs = this,
            mainDep = o,
            moreDeps = null,
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


    override fun <O : ObsVal<*>> addDependencyIgnoringFutureNullOuterChanges(
        o: O,
        vararg deepDependencies: (O) -> MObservable?
    ) = inSync {
        deps += DefaultDepListenerSet(
            obs = this,
            mainDep = o,
            moreDeps = null,
            getDeepDeps = if (deepDependencies.isEmpty()) null else {
                { deepDependencies.mapNotNull { it.invoke(o) } }
            },
            subListeners = deepDependencies.mapNotNull {
                it(o)?.observe {
                    markInvalid()
                }
            }
        )
    }

    override fun removeDependency(o: MObservable) = inSync {
        deps.filter { it.obs == o }.toList().forEach {
            removeDep(it)
        }
    }


    fun removeAllDependencies() = inSync {
        val depsCopy = deps.toList()
        depsCopy.forEach {
            removeDep(it)
        }
    }

    private fun removeDep(dep: DepListenerSet) {
        dep.removeAllListeners()
        deps.remove(dep)
    }

}

private sealed interface DepListenerSet {
    val obs: MObservable
    fun removeAllListeners()
}

private sealed class DepListenerSetBase : DepListenerSet

private open class DefaultDepListenerSet(
    final override val obs: CustomInvalidations,
    mainDep: MObservable,
    moreDeps: List<MObservable>?,
    getDeepDeps: ((dep: MObservable) -> List<MObservable>)?,
    var subListeners: List<MyListenerInter<*>>,
    @Suppress("UNUSED_PARAMETER") debugLogger: DebugLogger? = null
) : DepListenerSetBase() {

    private val getTheDeepDeps: Produce<List<MObservable>> = getDeepDeps?.let {
        { it.invoke(mainDep) }
        //	it(mainDep)
    } ?: { listOf() /*dont save mainDep in memory*/ }

    open val mainListeners = run {

        fun setup(
            dep: MObservable,
            index: Int
        ) = dep.observe {
            obs.markInvalid()
            subListeners.forEach { it.tryRemovingListener() }
            subListeners = getTheDeepDeps().map {
                it.observe {
                    obs.markInvalid()
                }
            }
        }.apply {
            name = "${WeakDepListenerSet::class.simpleName} listener $index for ${obs.nam}"
        }

        if (moreDeps == null) {
            SingleElementListImpl(setup(mainDep, 0))
        } else {
            (listOf(mainDep) + moreDeps).mapIndexed { index, it ->
                setup(it, index)
            }
        }


    }

    final override fun removeAllListeners() {
        mainListeners.forEach {
            it.tryRemovingListener()
        }
        subListeners.forEach {
            it.tryRemovingListener()
        }
    }

}


private open class WeakDepListenerSet(
    final override val obs: CustomInvalidations,
    weakRef: WeakRefInter<*>,
    mainDep: MObservable,
    moreDeps: List<MObservable>?,
    getDeepDeps: ((dep: MObservable) -> List<MObservable>)?,
    var subListeners: List<MyListenerInter<*>>,
    @Suppress("UNUSED_PARAMETER") debugLogger: DebugLogger? = null
) : DepListenerSetBase() {

    private val getTheDeepDeps: Produce<List<MObservable>> = getDeepDeps?.let {
        { it.invoke(mainDep) }
        //	it(mainDep)
    } ?: { listOf() /*dont save mainDep in memory*/ }


    open val mainListeners = run {

        fun setup(
            dep: MObservable,
            index: Int
        ) = dep.observeWeakly(weakRef) {
            obs.markInvalid()
            subListeners.forEach { it.tryRemovingListener() }
            subListeners = getTheDeepDeps().map {
                it.observeWeakly(weakRef) {
                    obs.markInvalid()
                }
            }
        }.apply {
            name = "${WeakDepListenerSet::class.simpleName} listener $index for ${obs.nam}"
        }

        if (moreDeps == null) {
            SingleElementListImpl(setup(mainDep, 0))
        } else {
            (listOf(mainDep) + moreDeps).mapIndexed { index, it ->
                setup(it, index)
            }
        }


    }

    final override fun removeAllListeners() {
        mainListeners.forEach {
            it.tryRemovingListener()
        }
        subListeners.forEach {
            it.tryRemovingListener()
        }
    }

}


private class DepListenerSetIgnoringNullOuterValues(
    obs: CustomInvalidations,
    mainDep: ObsVal<*>,
    getDeepDeps: ((dep: MObservable) -> List<MObservable>)?,
    subListeners: List<Listener>
) : DefaultDepListenerSet(obs, mainDep = mainDep, moreDeps = null, getDeepDeps, subListeners) {


    private val getTheDeepDeps: Produce<List<MObservable>> = getDeepDeps?.let {
        { it.invoke(mainDep) }
        //	it(mainDep)
    } ?: { listOf() /*dont save mainDep in memory*/ }


    override val mainListeners = listOf(mainDep.onChange { new ->
        if (new != null) {
            obs.markInvalid()
            this.subListeners.forEach { it.tryRemovingListener() }
            this.subListeners = getTheDeepDeps().map {
                it.observe {
                    obs.markInvalid()
                }
            }
        }
    })
}
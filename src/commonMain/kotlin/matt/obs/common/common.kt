package matt.obs.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import matt.lang.anno.Open
import matt.lang.assertions.require.requireNonNegative
import matt.lang.assertions.require.requireNot
import matt.lang.assertions.require.requireNull
import matt.lang.common.err
import matt.lang.common.unsafeErr
import matt.lang.exec.Exec
import matt.lang.function.Op
import matt.lang.tostring.SimpleStringableClass
import matt.lang.weak.common.WeakRefInter
import matt.lang.weak.weak
import matt.model.flowlogic.syncop.AntiDeadlockSynchronizer
import matt.model.op.prints.Prints
import matt.obs.common.custom.ElementDecoder
import matt.obs.hold.TypedObservableHolder
import matt.obs.listen.MyListener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.MyWeakListener
import matt.obs.listen.update.Update
import matt.obs.maybeRemoveByRefQueue
import matt.obs.prop.typed.AbstractTypedObsList
import matt.obs.prop.typed.AbstractTypedObsSet
import matt.obs.prop.typed.CastingTypedBindableProperty
import matt.obs.prop.typed.NonNull
import matt.obs.prop.typed.Null
import matt.obs.prop.typed.TypedBindableProperty
import matt.obs.prop.typed.TypedSerializableElement

@DslMarker
annotation class ObservableDSL

@ObservableDSL
interface MListenable<L : MyListenerInter<*>> : MObservable {
    fun addListener(listener: L): L
    fun releaseUpdatesAfter(op: Op)
}

abstract class MObservableImpl<U : Update, L : MyListenerInter<in U>> : SimpleStringableClass(), MListenable<L> {

    final override var nam: String? = null


    @Open
    override fun toStringProps() =
        mapOf(
            "name" to nam,
            "#listeners" to listeners.size
        )


    private val listeners = mutableListOf<L>()

    private val synchronizer by lazy { AntiDeadlockSynchronizer() }


    final override fun addListener(listener: L): L {
        synchronizer.operateOnInternalDataNowOrLater {
            listeners += listener
            listener as MyListener<*>
            requireNull(listener.currentObservable)
            listener.currentObservable = weak(this)
            if (listener is MyWeakListener<*>) {


                if (!maybeRemoveByRefQueue(listener)) {
                    listeners.remove(listener)
                    (listener as? MyListener<*>)?.currentObservable = null
                }
            }
        }
        return listener
    }

    final override var debugger: Prints? = null

    private var currentUpdateCount = 0

    private var notifyAfterUpdates: MutableList<U>? = null
    private var notifyAfterDepth = 0

    protected fun notifyListeners(update: U) =
        synchronizer.useInternalData {
            if (notifyAfterDepth > 0) {
                if (notifyAfterUpdates == null) {
                    notifyAfterUpdates = mutableListOf()
                }
                notifyAfterUpdates!!.add(update)
            } else {
                listeners.forEach { listener ->

                    if (listener.preInvocation(update) != null) {
                        listener.notify(update, debugger = debugger)
                        listener.postInvocation()
                    }
                }
            }
        }


    final override fun removeListener(listener: MyListenerInter<*>) {
        synchronizer.operateOnInternalDataNowOrLater {
            listeners.remove(listener)
            (listener as? MyListener<*>)?.currentObservable = null
        }
    }



    final override fun releaseUpdatesAfter(op: Op) =
        synchronizer.useInternalData {
            notifyAfterDepth += 1
            op()
            notifyAfterDepth -= 1
            requireNonNegative(notifyAfterDepth)
            if (notifyAfterDepth == 0) {
                notifyAfterUpdates?.forEach {
                    notifyListeners(it)
                }
                notifyAfterUpdates = null
            }
        }
}

@ObservableDSL
interface MObservable {
    var nam: String?
    fun observe(op: () -> Unit): MyListenerInter<*>
    fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*>

    fun removeListener(listener: MyListenerInter<*>)

    /*critical if an observer is receiving a batch of redundant notifications and only needs to act once*/
    @Open
    fun patientlyObserve(
        scheduleOp: Exec,
        op: () -> Unit
    ): MyListenerInter<*> {
        var shouldScheduleAnother = true
        return observe {
            if (shouldScheduleAnother) {
                shouldScheduleAnother = false
                scheduleOp {
                    shouldScheduleAnother = true
                    op()
                }
            }
        }
    }

    var debugger: Prints?
}



interface CustomDecoderAndEncoder<T, E: ElementDecoder<*>> {
    fun finishDecoding(): T
    val elements: List<E>
}


abstract class ElementDecoderImpl<V>(
    @Open override val key: String,
    serializer: KSerializer<V>,
    @Open override val isOptional: Boolean,
    cast: (Any?) -> V
) : ElementDecoder<V>(serializer, cast)



class MetaProp<T : TypedObservableHolder, V>(
    override val key: String,
    override val serializer: KSerializer<V>,
    private val onLoad: (V) -> MetaPropAction<T>,
    private val onNotFound: () -> MetaPropAction<T>,
    private val getValueForSerializing: () -> V,
    private val contenxt: MyCustomDecoderAndEncoder<T>,
    cast: (Any?) -> V
) : ElementDecoderImpl<V>(key = key, serializer = serializer, isOptional = true, cast = cast) {

    override fun shouldEncode(v: V): Boolean {
        return true /*we are encoding defaults here, yes*/
    }

    override fun handleLoadedValue(
        v: V
    ) {
        when (val result = onLoad(v)) {
            is DoNothing -> Unit
            is Replace   -> {
                requireNot(contenxt.gotReplacement)
                contenxt.gotReplacement = true
                unsafeErr("need to thorougly test replacing after these major destructive changes")
                contenxt.obj = result.obj
            }
        }
    }

    override fun getCurrentValue() = getValueForSerializing()

    override fun handleNotFound() {
        val notFoundResult = onNotFound()
        when (notFoundResult) {
            is DoNothing -> Unit
            is Replace   -> {
                requireNot(contenxt.gotReplacement)
                contenxt.gotReplacement = true
                unsafeErr("need to thorougly test replacing after these major destructive changes")
                contenxt.obj = notFoundResult.obj
            }
        }
    }
}


sealed interface MetaPropAction<T>
class Replace<T>(val obj: T) : MetaPropAction<T>
class DoNothing<T> : MetaPropAction<T>

sealed interface MetaPropResult
data object NotPresent : MetaPropResult



class MyCustomElement<V>(
    override val key: String,
    override val serializer: KSerializer<V>,
    override val isOptional: Boolean,
    /*private val cls: KClass<V & Any>,*/
    private val convertCastCurrentValue: (Any?) -> V,
    private val observable: TypedSerializableElement<*>
) : ElementDecoderImpl<V>(key = key, serializer = serializer, isOptional = isOptional, cast = convertCastCurrentValue) {

    override fun handleLoadedValue(
        v: V
    ) {
        observable.setFromEncoded(v)
        /*val observable = customDecoder.observables[key]
        (observable)!!.setFromEncoded(v)*/
    }

    override fun getCurrentValue(): V {
        /*val someValue = obj.namedObservables()[key]!!.provideEncodable()*/
        val someValue = observable.provideEncodable()

        val unboxed =
            when (val v = someValue) {
                is NonNull -> v.value
                Null       -> null
            }
        return convertCastCurrentValue(unboxed)
        /*
            return if (someValue == null) {
                return null
            }
            else {
                cls.cast(obj)
            }*/
    }

    override fun shouldEncode(v: V): Boolean {
        return true /*we are encoding defaults here, yes*/
    }

    override fun handleNotFound() {
        error("I am pretty sure this should never happen with this current implementation")
    }
}

class MyCustomDecoderAndEncoder<T : TypedObservableHolder>(
    internal var obj: T,
    getMetaProps: (MyCustomDecoderAndEncoder<T>) -> List<MetaProp<T, *>>
) : CustomDecoderAndEncoder<T, ElementDecoderImpl<*>> {
    val observables = obj.namedObservables()
    var gotReplacement = false
    override fun finishDecoding(): T = obj

    val metaProps: List<MetaProp<T, *>> by lazy {
        getMetaProps(this)
    }




    final override val elements by lazy {
        buildList {
            addAll(metaProps)
            val metaKeys = metaProps.map { it.key }
            obj.namedObservables().forEach {

                if (it.key in metaKeys) {
                    err("property can not have the name ${it.key}, which is used as a meta property")
                }

                when (val theValue = it.value) {
                    is TypedBindableProperty<*> -> {
                        add(
                            theValue.customElement(it.key)
                        )
                    }

                    is AbstractTypedObsList<*>  -> {
                        val cls = theValue.elementCls
                        val elementSerializer = theValue.elementSer
                      /*      serializer(
                                cls,
                                run {
                                    check(cls.typeParameters.isEmpty())
                                    listOf()
                                },
                                isNullable = theValue.nullableElements
                            )*/
                        add(
                            theValue.customElement(it.key)

                        )
                    }

                    is AbstractTypedObsSet<*>   -> {
                        val cls = theValue.elementCls
                        val elementSer = theValue.elementSer
                       /*     serializer(
                                cls,
                                run {
                                    check(cls.typeParameters.isEmpty())
                                    listOf()
                                },
                                isNullable = theValue.nullableElements
                            )*/
                        add(
                            theValue.customElement(it.key)
                        )
                    }

                    is CastingTypedBindableProperty -> error("I should not need this branch! Bug!")
                }
            }
        }
    }
}

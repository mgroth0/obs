package matt.obs.col.olist.mappedlist


import matt.lang.model.value.LazyValue
import matt.lang.setall.setAll
import matt.lang.weak.MyWeakRef
import matt.obs.col.change.mirror
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.basicMutableObservableListOf
import matt.obs.col.olist.dynamic.CalculatedList
import matt.obs.col.olist.view

fun <S, T> ImmutableObsList<S>.toMappedList(mapFun: (S) -> T): MappedList<T> {
    return BasicMappedList(
        source = this, target = BasicObservableListImpl(), converter = mapFun
    )
}

interface MappedList<T> : CalculatedList<T> {

}

class BasicMappedList<S, T>(
    private val source: ImmutableObsList<S>, private val target: MutableObsList<T>, private val converter: (S) -> T
) : ImmutableObsList<T> by target, MappedList<T> {

    override fun refresh() {
        target.setAll(source.map(converter))
    }

    init {
        refresh()
        source.onChange {
            target.mirror(it, converter)
        }
    }

}


fun <W : Any, S, T> ImmutableObsList<S>.toWeakMappedList(w: W, mapFun: (W, S) -> T): CalculatedList<T> {
    return WeakMappedList(
        weakObj = w,
        source = this,
        target = basicMutableObservableListOf(),
        converter = mapFun
    )
}


class WeakMappedList<W : Any, S, T>(
    weakObj: W,
    private val source: ImmutableObsList<S>,
    private val target: MutableObsList<T>,
    private val converter: (W, S) -> T
) : ImmutableObsList<T> by target, MappedList<T> {
    private val weakRef = MyWeakRef(weakObj)

    override fun refresh() {
        target.atomicChange {
            setAll(
                this@WeakMappedList.source.map {
                    this@WeakMappedList.converter(this@WeakMappedList.weakRef.deref()!!, it)
                }
            )
        }
    }

    init {
        refresh()
        source.onChangeWithAlreadyWeak(weakRef) { w, it ->
            target.mirror(it) {
                converter(w, it)
            }
        }
    }
}


fun <S, T> ImmutableObsList<S>.toLazyMappedList(mapFun: (S) -> T): MappedList<T> {
    return LazyMappedList(
        source = this, target = BasicObservableListImpl(), converter = mapFun
    )
}

class LazyMappedList<S, T>(
    private val source: ImmutableObsList<S>,
    private val target: MutableObsList<LazyValue<T>>,
    private val converter: (S) -> T
) : ImmutableObsList<T> by target.view({ it.value }), MappedList<T> {

    override fun refresh() {
        target.setAll(source.map {
            LazyValue { converter(it) }
        })
    }


    init {
        refresh()
        source.onChange {
            target.mirror(it.convert(
                target
            ) {
                LazyValue {
                    converter(it)
                }
            })
        }
    }
}


fun <S, T, W : Any> ImmutableObsList<S>.toLazyMappedListWithWeak(w: W, mapFun: (W, S) -> T): MappedList<T> {
    return LazyWeakMappedList(
        weakObject = w,
        source = this,
        target = basicMutableObservableListOf(),
        converter = mapFun
    )
}

class LazyWeakMappedList<W : Any, S, T>(
    weakObject: W,
    private val source: ImmutableObsList<S>,
    private val target: MutableObsList<LazyValue<T>> = basicMutableObservableListOf<LazyValue<T>>(),
    private val converter: (W, S) -> T,
) : ImmutableObsList<T> by target.view({ it.value }), MappedList<T> {

    private val weakRef = MyWeakRef(weakObject)

    override fun refresh() {
        target.setAll(source.map {
            LazyValue {
                converter(weakRef.deref()!!, it)
            }
        })
    }

    init {
        refresh()
        source.onChangeWithAlreadyWeak(weakRef) { deRefed, it ->
            target.mirror(it.convert(
                target
            ) {
                LazyValue {
                    converter(deRefed, it)
                }
            })
        }
    }
}


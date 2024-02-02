package matt.obs.col.olist.sync

import matt.lang.convert.BiConverter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.col.change.mirror
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.MutableObsList

fun <S, T> MutableObsList<S>.toSyncedList(converter: BiConverter<S, T>): MutableObsList<T> {
    val r = BasicObservableListImpl(map { converter.convertToB(it) })
    val rb = RecursionBlocker()
    onChange("toSyncedList1") {
        rb.with {
            r.mirror(it) { converter.convertToB(it) }
        }
    }
    r.onChange("toSyncedList2") {
        rb.with {
            mirror(it) { converter.convertToA(it) }
        }
    }
    return r
}

package matt.obs.col.olist.cat

import matt.lang.setall.setAll
import matt.lang.sync.SimpleReferenceMonitor
import matt.lang.sync.inSync
import matt.obs.col.change.mirror
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.basicMutableObservableListOf

fun <E> ImmutableObsList<E>.concatenatedTo(other: ImmutableObsList<E>) = ConcatList(this, other)

class ConcatList<E>(
    firstList: ImmutableObsList<E>,
    secondList: ImmutableObsList<E>,
    private val target: MutableObsList<E> = basicMutableObservableListOf()
) : MutableObsList<E> by target {
    init {
        var annoPartStart = 0
        var annoPartEndExclusive = firstList.size
        var realPartStart = firstList.size
        var realPartEndExclusive = firstList.size + secondList.size

        val monitor = SimpleReferenceMonitor()

        target.setAll(firstList + secondList)

        firstList.onChange {
            inSync(monitor) {
                target.subList(annoPartStart, annoPartEndExclusive).mirror(it)
                annoPartStart = 0
                annoPartEndExclusive = firstList.size
                realPartStart = firstList.size
                realPartEndExclusive = firstList.size + secondList.size
            }
        }
        secondList.onChange {
            inSync(monitor) {
                target.subList(realPartStart, realPartEndExclusive).mirror(it)
                annoPartStart = 0
                annoPartEndExclusive = firstList.size
                realPartStart = firstList.size
                realPartEndExclusive = firstList.size + secondList.size
            }
        }

    }
}

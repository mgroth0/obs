package matt.obs.col.olist.cat

import matt.lang.setAll
import matt.obs.col.change.mirror
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.ObsList
import matt.obs.col.olist.basicMutableObservableListOf

fun <E> ObsList<E>.concatenatedTo(other: ObsList<E>) = ConcatList(this, other)

class ConcatList<E>(
  firstList: ObsList<E>,
  secondList: ObsList<E>,
  private val target: MutableObsList<E> = basicMutableObservableListOf()
): ObsList<E> by target {
  init {
	var annoPartStart = 0
	var annoPartEndExclusive = firstList.size
	var realPartStart = firstList.size
	var realPartEndExclusive = firstList.size + secondList.size

	val sem = java.util.concurrent.Semaphore(1)

	target.setAll(firstList + secondList)

	firstList.onChange {
	  sem.acquire()
	  target.subList(annoPartStart, annoPartEndExclusive).mirror(it)
	  annoPartStart = 0
	  annoPartEndExclusive = firstList.size
	  realPartStart = firstList.size
	  realPartEndExclusive = firstList.size + secondList.size
	  sem.release()
	}
	secondList.onChange {
	  sem.acquire()
	  target.subList(realPartStart, realPartEndExclusive).mirror(it)
	  annoPartStart = 0
	  annoPartEndExclusive = firstList.size
	  realPartStart = firstList.size
	  realPartEndExclusive = firstList.size + secondList.size
	  sem.release()
	}

  }
}
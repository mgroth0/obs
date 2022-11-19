package matt.obs.col.olist.mappedlist


import matt.lang.model.value.LazyValue
import matt.model.op.convert.Converter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.col.change.mirror
import matt.obs.col.olist.BasicObservableListImpl
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.olist.ObsList

fun <S, T> ObsList<S>.toMappedList(mapFun: (S)->T): ObsList<T> {
  val r = BasicObservableListImpl(map(mapFun))
  onChange {
	r.mirror(it, mapFun)
  }
  return r
}

/*fun <S, T> ObsList<S>.toSomewhatLazyMappedList(mapFun: (S)->T): ObsList<T> {
  val r = BasicObservableListImpl(MutableLazyList(map {
	LazyValue { mapFun(it) }
  }))
  onChange {
	r.mirror(it, mapFun)
  }
  return r
}*/



fun <S, T> ObsList<S>.toLazyMappedList(mapFun: (S)->T): ImmutableObsList<T> {
  val r = BasicObservableListImpl(map { LazyValue { mapFun(it) } })
  onChange {
	r.mirror(
	  it.convert(
		r
	  ) {
		LazyValue {
		  mapFun(it)
		}
	  }
	)
  }
  return r.view { it.value }
}

fun <S, T> MutableObsList<S>.toSyncedList(converter: Converter<S, T>): MutableObsList<T> {
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
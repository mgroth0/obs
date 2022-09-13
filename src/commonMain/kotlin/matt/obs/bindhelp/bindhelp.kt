package matt.obs.bindhelp

import matt.lang.setAll
import matt.lang.weak.WeakRef
import matt.lang.weak.getValue
import matt.model.recursionblocker.RecursionBlocker
import matt.obs.MObservable
import matt.obs.col.change.mirror
import matt.obs.col.olist.BasicROObservableList
import matt.obs.listen.MyListener
import matt.obs.prop.FXBackedPropBase
import matt.obs.prop.MObservableVal
import matt.obs.prop.ValProp
import matt.obs.prop.WritableMObservableVal
import kotlin.jvm.Synchronized

sealed interface Bindable {
  val bindManager: Bindable
  var theBind: ABind?
  fun unbind()
}

sealed class BindableImpl: Bindable {
  override val bindManager get() = this
  override var theBind: ABind? = null
  @Synchronized override fun unbind() {
	require(this !is FXBackedPropBase || !isFXBound)
	theBind?.cut()
	theBind = null
  }
}

interface BindableList<E>: Bindable {
  fun <S> bind(source: BasicROObservableList<S>, converter: (S)->E)
  fun <S> bind(source: ValProp<S>, converter: (S)->List<E>)
}

class BindableListImpl<E>(private val list: MutableList<E>): BindableImpl(), BindableList<E> {

  @Synchronized override fun <S> bind(source: BasicROObservableList<S>, converter: (S)->E) {
	unbind()
	list.setAll(source.map(converter))
	val listener = source.onChange {
	  list.mirror(it, converter)
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun <S> bind(source: ValProp<S>, converter: (S)->List<E>) {
	unbind()
	list.setAll(converter(source.value))
	val listener = source.onChange {
	  list.setAll(converter(it))
	}
	theBind = TheBind(source = source, listener = listener)
  }
}

interface BindableValue<T>: Bindable {
  fun bind(source: MObservableVal<T, *, *>)
  fun bindBidirectional(source: WritableMObservableVal<T>)
}

class BindableValueHelper<T>(private val wProp: WritableMObservableVal<T>): BindableImpl(), BindableValue<T> {
  @Synchronized override fun bind(source: MObservableVal<T, *, *>) {
	require(this !is FXBackedPropBase || !isFXBound)
	unbind()
	wProp.value = source.value
	val listener = source.onChange {
	  wProp.value = source.value
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun bindBidirectional(source: WritableMObservableVal<T>) {
	require(this !is FXBackedPropBase || !isFXBound)
	unbind()
	source.unbind()


	bind(source)
	source.bind(wProp)

	wProp.value = source.value

	val rBlocker = RecursionBlocker()
	val sourceListener = source.onChange {
	  rBlocker.with {
		wProp.value = source.value
	  }
	}
	val targetListener = wProp.onChange {
	  rBlocker.with {
		source.value = wProp.value
	  }
	}

	theBind =
	  BiTheBind(source = source, target = wProp, sourceListener = sourceListener, targetListener = targetListener)
	source.theBind = theBind
  }

}

interface ABind {
  fun cut()
}

class TheBind(
  source: MObservable<*>,
  private val listener: MyListener<*>
): ABind {
  val source by WeakRef(source)
  override fun cut() {
	source?.removeListener(listener)
  }
}

class BiTheBind(
  val source: WritableMObservableVal<*>,
  val target: WritableMObservableVal<*>,
  private val sourceListener: MyListener<*>,
  private val targetListener: MyListener<*>
): ABind {

  override fun cut() {
	source.removeListener(sourceListener)
	target.removeListener(targetListener)
	source.theBind = null
	target.theBind = null
  }
}
package matt.obs.bindhelp

import matt.lang.setAll
import matt.lang.weak.WeakRef
import matt.lang.weak.getValue
import matt.model.convert.Converter
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.obs.MListenable
import matt.obs.bind.LazyBindableProp
import matt.obs.bind.binding
import matt.obs.col.InternallyBackedOCollection
import matt.obs.col.change.mirror
import matt.obs.col.olist.ObsList
import matt.obs.listen.Listener
import matt.obs.prop.BindableProperty
import matt.obs.prop.FXBackedPropBase
import matt.obs.prop.ObsVal
import matt.obs.prop.Var
import kotlin.jvm.Synchronized

sealed interface Bindable {
  val bindManager: Bindable
  var theBind: ABind?
  fun unbind()
  val isBound: Boolean get() = theBind != null
  val isBoundUnidirectionally: Boolean get() = theBind is TheBind
  val isBoundBidirectionally: Boolean get() = theBind is BiTheBind
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
  fun <S> bind(source: ObsList<S>, converter: (S)->E)
  fun <S> bind(source: ObsVal<S>, converter: (S)->List<E>)
}

/*todo: lazily evaluated bound lists!*/
class BindableListImpl<E>(private val target: MutableList<E>): BindableImpl(), BindableList<E> {

  @Synchronized override fun <S> bind(source: ObsList<S>, converter: (S)->E) {
	unbind()
	(target as? InternallyBackedOCollection<*>)?.bindWritePass?.hold()
	target.setAll(source.map(converter))
	(target as? InternallyBackedOCollection<*>)?.bindWritePass?.release()
	val listener = source.onChange {
	  (target as? InternallyBackedOCollection<*>)?.bindWritePass?.hold()
	  target.mirror(it, converter)
	  (target as? InternallyBackedOCollection<*>)?.bindWritePass?.release()
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun <S> bind(source: ObsVal<S>, converter: (S)->List<E>) {
	unbind()
	(target as? InternallyBackedOCollection<*>)?.bindWritePass?.hold()
	target.setAll(converter(source.value))
	(target as? InternallyBackedOCollection<*>)?.bindWritePass?.release()
	val listener = source.onChange {
	  (target as? InternallyBackedOCollection<*>)?.bindWritePass?.hold()
	  target.setAll(converter(it))
	  (target as? InternallyBackedOCollection<*>)?.bindWritePass?.release()
	}
	theBind = TheBind(source = source, listener = listener)
  }
}

interface BindableValue<T>: Bindable {
  fun bind(source: ObsVal<out T>)
  fun bindBidirectional(source: Var<T>)
  fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>)
}

/*this is the way to have a final interface fun!!!*/
fun <S, T> BindableValue<T>.bind(source: ObsVal<out S>, converter: Converter<S, T>) =
  bind(source.binding { converter.convertToB(it) })

fun <S, T> BindableValue<T>.bindInv(source: ObsVal<out S>, converter: Converter<T, S>) =
  bind(source.binding { converter.convertToA(it) })

fun <S, T> BindableValue<T>.bindBidirectionalInv(source: Var<S>, converter: Converter<S, T>) =
  bindBidirectional(source, converter.invert())

class BindableValueHelper<T>(private val wProp: Var<T>): BindableImpl(), BindableValue<T> {

  infix fun <TT> Var<TT>.setCorrectlyTo(new: ()->TT) {
	when (this) {
	  is BindableProperty<TT> -> setFromBinding(new())
	  is LazyBindableProp<TT> -> setFromBinding(new)
	  else                    -> {
		value = new()
	  }
	}
  }

  @Synchronized override fun bind(source: ObsVal<out T>) {
	require(this !is FXBackedPropBase || !isFXBound)
	unbind()
	wProp setCorrectlyTo { source.value }
	val listener = source.observe {
	  wProp setCorrectlyTo { source.value }
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun bindBidirectional(source: Var<T>) {
	unbind()
	source.unbind()
	wProp setCorrectlyTo { source.value }

	val rBlocker = RecursionBlocker()
	val sourceListener = source.observe {
	  rBlocker.with {
		wProp setCorrectlyTo { source.value }
	  }
	}
	val targetListener = wProp.observe {
	  rBlocker.with {
		source setCorrectlyTo { wProp.value }
	  }
	}

	theBind =
	  BiTheBind(source = source, target = wProp, sourceListener = sourceListener, targetListener = targetListener)
	source.theBind = theBind
  }

  @Synchronized override fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>) {
	unbind()
	source.unbind()
	wProp setCorrectlyTo { converter.convertToA(source.value) }

	val rBlocker = RecursionBlocker()
	val sourceListener = source.observe {
	  rBlocker.with {
		wProp setCorrectlyTo { converter.convertToA(source.value) }
	  }
	}
	val targetListener = wProp.observe {
	  rBlocker.with {
		source setCorrectlyTo { converter.convertToB(wProp.value) }
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
  source: MListenable<*>, private val listener: Listener
): ABind {
  val source by WeakRef(source)
  override fun cut() {
	source?.removeListener(listener)
  }
}

class BiTheBind(
  val source: Var<*>, val target: Var<*>, private val sourceListener: Listener, private val targetListener: Listener
): ABind {

  override fun cut() {
	source.removeListener(sourceListener)
	target.removeListener(targetListener)
	source.theBind = null
	target.theBind = null
  }
}
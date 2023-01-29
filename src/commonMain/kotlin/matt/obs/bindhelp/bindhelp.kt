package matt.obs.bindhelp

import matt.lang.go
import matt.lang.setall.setAll
import matt.lang.weak.MyWeakRef
import matt.lang.weak.getValue
import matt.model.flowlogic.recursionblocker.RecursionBlocker
import matt.model.op.convert.Converter
import matt.obs.MListenable
import matt.obs.bind.LazyBindableProp
import matt.obs.bind.binding
import matt.obs.col.IBObsCol
import matt.obs.col.change.mirror
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.MutableObsList
import matt.obs.col.oset.MutableObsSet
import matt.obs.col.oset.ObsSet
import matt.obs.listen.MyListenerInter
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
  fun bind(source: ImmutableObsList<E>) = bind(source) { it }
  fun <S> bind(source: ImmutableObsList<S>, converter: (S)->E)
  fun <S> bind(source: ObsVal<S>, converter: (S)->List<E>)
}

interface BindableSet<E>: Bindable {
  fun bind(source: ObsSet<E>) = bind(source) { it }
  fun <S> bind(source: ObsSet<S>, converter: (S)->E)
  fun <S> bind(source: ObsVal<S>, converter: (S)->Set<E>)
}

/*matt.log.todo.todo: lazily evaluated bound lists!*/
class BindableListImpl<E>(private val target: MutableObsList<E>): BindableImpl(), BindableList<E> {

  @Synchronized override fun <S> bind(source: ImmutableObsList<S>, converter: (S)->E) {
	unbind()
	(target as? IBObsCol)?.bindWritePass?.hold()
	target.setAll(source.map(converter))
	(target as? IBObsCol)?.bindWritePass?.release()
	val listener = source.onChange {
	  (target as? IBObsCol)?.bindWritePass?.hold()
	  target.mirror(it, converter)
	  (target as? IBObsCol)?.bindWritePass?.release()
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun <S> bind(source: ObsVal<S>, converter: (S)->List<E>) {
	unbind()
	(target as? IBObsCol)?.bindWritePass?.hold()
	target.setAll(converter(source.value))
	(target as? IBObsCol)?.bindWritePass?.release()
	val listener = source.onChange {
	  (target as? IBObsCol)?.bindWritePass?.hold()
	  target.setAll(converter(it))
	  (target as? IBObsCol)?.bindWritePass?.release()
	}
	theBind = TheBind(source = source, listener = listener)
  }
}

/*matt.log.todo.todo: lazily evaluated bound lists!*/
class BindableSetImpl<E>(private val target: MutableObsSet<E>): BindableImpl(), BindableSet<E> {

  @Synchronized override fun <S> bind(source: ObsSet<S>, converter: (S)->E) {
	unbind()
	(target as? IBObsCol)?.bindWritePass?.hold()
	target.setAll(source.map(converter))
	(target as? IBObsCol)?.bindWritePass?.release()
	val listener = source.onChange {
	  (target as? IBObsCol)?.bindWritePass?.hold()
	  target.mirror(it, converter)
	  (target as? IBObsCol)?.bindWritePass?.release()
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun <S> bind(source: ObsVal<S>, converter: (S)->Set<E>) {
	unbind()
	(target as? IBObsCol)?.bindWritePass?.hold()
	target.setAll(converter(source.value))
	(target as? IBObsCol)?.bindWritePass?.release()
	val listener = source.onChange {
	  (target as? IBObsCol)?.bindWritePass?.hold()
	  target.setAll(converter(it))
	  (target as? IBObsCol)?.bindWritePass?.release()
	}
	theBind = TheBind(source = source, listener = listener)
  }
}

interface BindableValue<T>: Bindable {
  fun bind(source: ObsVal<out T>)
  fun bindWeakly(source: ObsVal<out T>)
  fun bindBidirectional(source: Var<T>, checkEquality: Boolean = false, clean: Boolean = true, debug: Boolean = false)
  fun <S> bindBidirectional(source: Var<S>, converter: Converter<T, S>)
}

/*this is the way to have a final interface fun!!!*/
fun <S, T> BindableValue<T>.bind(source: ObsVal<out S>, converter: Converter<S, T>) =
  bind(source.binding { converter.convertToB(it) })

fun <S, T> BindableValue<T>.bindInv(source: ObsVal<out S>, converter: Converter<T, S>) =
  bind(source.binding { converter.convertToA(it) })

fun <S, T> BindableValue<T>.bindBidirectionalInv(source: Var<S>, converter: Converter<S, T>) =
  bindBidirectional(source, converter.invert())


/*reduce number of listeners, which contributes a huge amount to my object count*/
fun <T> bindMultipleTargetsTogether(
  targets: Collection<BindableValue<T>>,
  source: ObsVal<out T>
) {


  targets.forEach {
	require(it !is FXBackedPropBase || !it.isFXBound)
	it.unbind()
	@Suppress("UNCHECKED_CAST")
	(it.bindManager as BindableValueHelper<T>).apply {
	  wProp setCorrectlyTo { source.value }
	}
  }

  @Suppress("UNCHECKED_CAST")
  val listener = source.observe {
	targets.forEach {
	  (it.bindManager as BindableValueHelper<T>).apply {
		wProp setCorrectlyTo { source.value }
	  }
	}
  }

  val b = TheBind(source = source, listener = listener)
  targets.forEach {
	it.bindManager.theBind = b
  }
}

infix fun <TT> Var<TT>.setCorrectlyTo(new: ()->TT) {
  when (this) {
	is BindableProperty<TT> -> setFromBinding(new())
	is LazyBindableProp<TT> -> setFromBinding(new)
	else                    -> {
	  value = new()
	}
  }
}

class BindableValueHelper<T>(internal val wProp: Var<T>): BindableImpl(), BindableValue<T> {



  @Synchronized override fun bind(source: ObsVal<out T>) {
	require(this !is FXBackedPropBase || !isFXBound)
	unbind()
	wProp setCorrectlyTo { source.value }
	val listener = source.observe {
	  wProp setCorrectlyTo { source.value }
	}
	theBind = TheBind(source = source, listener = listener)
  }


  @Synchronized override fun bindWeakly(source: ObsVal<out T>) {
	require(this !is FXBackedPropBase || !isFXBound)
	unbind()
	wProp setCorrectlyTo { source.value }
	val listener = source.onChangeWithWeak(wProp) { w, it ->
	  w setCorrectlyTo { it }
	}
	theBind = TheBind(source = source, listener = listener)
  }

  @Synchronized override fun bindBidirectional(source: Var<T>, checkEquality: Boolean, clean: Boolean, debug: Boolean) {
	if (clean) {
	  unbind()
	  source.unbind()
	}
	if (!checkEquality || source.value != wProp.value) {
	  wProp setCorrectlyTo { source.value }
	}

	val rBlocker = RecursionBlocker()
	val sourceListener = source.observe {
	  if (!checkEquality || source.value != wProp.value) {
		rBlocker.with {
		  wProp setCorrectlyTo { source.value }
		}
	  }

	}


	val targetListener = wProp.observe {
	  if (!checkEquality || source.value != wProp.value) {
		rBlocker.with {
		  source setCorrectlyTo { wProp.value }
		}
	  }
	}

	theBind =
	  BiTheBind(
		source = source, target = wProp, sourceListener = sourceListener, targetListener = targetListener, debug = debug
	  )
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
  source: MListenable<*>,
  listener: MyListenerInter<*>
): ABind {
  private val source by MyWeakRef(source)
  private val listener by MyWeakRef(listener)
  override fun cut() {
	listener?.go { l ->
	  source?.removeListener(l)
	}
  }
}

class BiTheBind(
  source: Var<*>,
  target: Var<*>,
  sourceListener: MyListenerInter<*>,
  targetListener: MyListenerInter<*>,
  private val debug: Boolean = false
): ABind {
  private val source by MyWeakRef(source)
  private val target by MyWeakRef(target)
  private val sourceListener by MyWeakRef(sourceListener)
  private val targetListener by MyWeakRef(targetListener)
  override fun cut() {
	if (debug) println("cutting $this")
	sourceListener?.go {
	  source?.removeListener(it)
	}
	targetListener?.go {
	  target?.removeListener(it)
	}
	source?.theBind = null
	target?.theBind = null
  }
}
package matt.obs.prop.listen

sealed interface ListenerType<T> {
  fun invokeWith(old: T, new: T) = when (this) {
	is NewListener<T>       -> invoke(new)
	is OldAndNewListener<T> -> invoke(old, new)
  }
}

fun interface NewListener<T>: ListenerType<T> {
  fun invoke(new: T)
  override fun invokeWith(old: T, new: T) = super.invokeWith(old = old, new = new) /*keep this because of internal kotlin runtime bug? ... yup*/
}

fun interface OldAndNewListener<T>: ListenerType<T> {
  fun invoke(old: T, new: T)
  override fun invokeWith(old: T, new: T) = super.invokeWith(old = old, new = new) /*keep this because of internal kotlin runtime bug? ... yup*/
}
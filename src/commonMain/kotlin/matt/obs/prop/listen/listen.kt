package matt.obs.prop.listen

sealed interface ListenerType<T> {
  fun invokeWith(old: T, new: T) = when (this) {
	is NewListener<T>       -> invoke(new)
	is OldAndNewListener<T> -> invoke(old, new)
  }
}

fun interface NewListener<T>: ListenerType<T> {
  fun invoke(new: T)
}

fun interface OldAndNewListener<T>: ListenerType<T> {
  fun invoke(old: T, new: T)
}
package matt.obs.invalid

import matt.obs.MObservable

interface UpdatesFromOutside: MObservable {
  fun markInvalid()
  fun setupDependencies(vararg obs: MObservable) {
	obs.forEach {
	  it.invalidate(this)
	}
  }
  fun bind(o: MObservable) = o.invalidate(this)
}

fun <O: MObservable, UFO: UpdatesFromOutside> UFO.invalidateDeeplyFrom(o: O, deepGetter: O.()->MObservable?): UFO {
  var deepListener = o.deepGetter()?.invalidate(this)
  o.observe {
	deepListener?.tryRemovingListener()
	deepListener = o.deepGetter()?.invalidate(this)
  }
  return this
}
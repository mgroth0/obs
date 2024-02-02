package matt.obs.fx


actual fun <E> Iterable<E>.requireNotFxObservable(): Iterable<E> {
    /*FX is not present on JS*/
    return this
}

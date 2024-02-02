@file:JvmName("FxCommonJvmAndroidKt")

package matt.obs.fx

import matt.classload.systemClassGetter
import matt.lang.assertions.require.requireNot
import matt.lang.classname.JvmQualifiedClassName
import matt.lang.go


internal val JAVAFX_OBSERVABLE_CLASS by lazy {
    with(systemClassGetter()) {
        /*warn("observableClass is null")*/
        /*It's gonna be null when using a program that doesn't depend on javafx... which I finally am capable of*/
        JvmQualifiedClassName("javafx.beans.Observable").get()
    }
}

actual fun <E> Iterable<E>.requireNotFxObservable(): Iterable<E> {
    JAVAFX_OBSERVABLE_CLASS?.go {
        requireNot(
            it.isInstance(it)
            /*!this::class.isSubTypeOf(it)*/
        ) {
            "this is the wrong way to make a BasicObservableList from an ObservableList if you want them to be synced"
        }
    }
    return this
}

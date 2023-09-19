package matt.obs.fx

import matt.classload.systemClassGetter
import matt.lang.classname.JvmQualifiedClassName
import matt.lang.go
import matt.lang.platform.KotlinPlatform.JS
import matt.lang.platform.KotlinPlatform.JVM
import matt.lang.platform.KotlinPlatform.Native
import matt.lang.platform.currentPlatform
import matt.lang.require.requireNot

internal val JAVAFX_OBSERVABLE_CLASS by lazy {
    with(systemClassGetter()) {
        /*warn("observableClass is null")*/
        /*It's gonna be null when using a program that doesn't depend on javafx... which I finally am capable of*/
        JvmQualifiedClassName("javafx.beans.Observable").get()
    }
}


internal fun <E> Iterable<E>.requireNotFxObservable() = apply {
    when (currentPlatform) {
        JVM        -> {
            JAVAFX_OBSERVABLE_CLASS?.go {
                requireNot(
                    it.isInstance(it)
                    /*!this::class.isSubTypeOf(it)*/
                ) {
                    "this is the wrong way to make a BasicObservableList from an ObservableList if you want them to be synced"
                }
            }
        }

        JS, Native -> Unit
    }

}
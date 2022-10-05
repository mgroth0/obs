package matt.obs.fx

import matt.lang.go
import matt.reflect.classForName
import matt.reflect.isSubTypeOf

internal val JAVAFX_OBSERVABLE_CLASS by lazy {
  classForName("javafx.beans.Observable") ?: run {
	/*warn("observableClass is null")*/
	/*It's gonna be null when using a program that doesn't depend on javafx... which I finally am capable of*/
	null
  }
}


internal fun <E> Collection<E>.requireNotObservable() = apply {
  JAVAFX_OBSERVABLE_CLASS?.go {
	require(!this::class.isSubTypeOf(it)) {
	  "this is the wrong way to make a BasicObservableList from an ObservableList if you want them to be synced"
	}
  }
}
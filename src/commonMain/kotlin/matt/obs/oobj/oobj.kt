package matt.obs.oobj

import matt.obs.MObservableObjectImpl

open class ObservableObject<T: ObservableObject<T>>: MObservableObjectImpl<T>()
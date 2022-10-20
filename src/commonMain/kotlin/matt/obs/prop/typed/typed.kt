package matt.obs.prop.typed

import matt.obs.prop.BindableProperty
import kotlin.reflect.KClass

class TypedBindableProperty<T: Any>(val cls: KClass<T>, value: T): BindableProperty<T>(value)
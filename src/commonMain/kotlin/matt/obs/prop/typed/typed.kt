package matt.obs.prop.typed

import matt.obs.prop.BindableProperty
import kotlin.reflect.KClass

class TypedBindableProperty<T>(val cls: KClass<*>, val nullable: Boolean, value: T): BindableProperty<T>(value)
package matt.obs.json.prop

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import matt.lang.common.NOT_IMPLEMENTED
import matt.obs.prop.writable.BindableProperty


fun <T> BindableProperty<T>.setFromJson(j: JsonElement) {
    if (j is JsonNull) {
        @Suppress("UNCHECKED_CAST")
        (this as BindableProperty<Any?>).value = null
    } else if (j is JsonPrimitive) {
        if (j.isString) {
            @Suppress("UNCHECKED_CAST")
            (this as BindableProperty<String?>).value = j.content
        } else {
            val int = j.intOrNull
            if (int != null) {
                @Suppress("UNCHECKED_CAST")
                (this as BindableProperty<Int?>).value = int
            } else {
                val double = j.doubleOrNull
                if (double != null) {
                    @Suppress("UNCHECKED_CAST")
                    (this as BindableProperty<Double?>).value = double
                } else {
                    val bool = j.booleanOrNull
                    if (bool != null) {
                        @Suppress("UNCHECKED_CAST")
                        (this as BindableProperty<Boolean?>).value = bool
                    } else {
                        NOT_IMPLEMENTED("don't know how to get value from $j")
                    }
                }
            }
        }
    } else NOT_IMPLEMENTED("don't know how to get value from $j")
}

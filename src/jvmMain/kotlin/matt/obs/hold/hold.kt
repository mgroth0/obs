@file:OptIn(InternalSerializationApi::class)

package matt.obs.hold

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import matt.obs.json.sers.JsonObjectFXSerializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@OptIn(InternalSerializationApi::class)
open class TypedObsHolderSerializer<T: TypedObservableHolder>(
  private val cls: KClass<T>
): JsonObjectFXSerializer<T>(cls) {
  override fun deserialize(jsonObject: JsonObject): T {
	return cls.createInstance().apply {
	  namedObservables().forEach { (k, v) ->
		jsonObject[k]?.let { /*null should be JsonNull here, so nullables should be ok*/
		  v::value.set(Json.decodeFromJsonElement(v.cls.serializer(), it))
		}
	  }
	}
  }
}
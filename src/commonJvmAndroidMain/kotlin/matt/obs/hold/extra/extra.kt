package matt.obs.hold.extra

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import matt.lang.assertions.require.requireNot
import matt.log.warn.common.warn
import matt.obs.hold.TypedObservableHolder
import matt.obs.hold.j.ElementDecoderImpl
import matt.obs.hold.j.MyCustomDecoder
import matt.obs.hold.j.TypedObsHolderSerializer
import kotlin.reflect.KClass


class MetaProp<T : TypedObservableHolder, V>(
    override val key: String,
    override val serializer: KSerializer<V>,
    private val onLoad: (V) -> MetaPropAction<T>,
    private val onNotFound: () -> MetaPropAction<T>,
    private val getValueForSerializing: () -> V
) : ElementDecoderImpl<T, V>(key = key, serializer = serializer, isOptional = true) {

    override fun shouldEncode(v: V): Boolean {
        return true /*we are encoding defaults here, yes*/
    }

    override fun handleLoadedValue(
        v: V,
        customDecoder: MyCustomDecoder<T>
    ) {
        when (val result = onLoad(v)) {
            is DoNothing -> Unit
            is Replace   -> {
                requireNot(customDecoder.gotReplacement)
                customDecoder.gotReplacement = true
                customDecoder.obj = result.obj
            }
        }
    }

    override fun getCurrentValue(obj: T) = getValueForSerializing()

    override fun handleNotFound(customDecoder: MyCustomDecoder<T>) {
        val notFoundResult = onNotFound()
        when (notFoundResult) {
            is DoNothing -> Unit
            is Replace   -> {
                requireNot(customDecoder.gotReplacement)
                customDecoder.gotReplacement = true
                customDecoder.obj = notFoundResult.obj
            }
        }
    }
}

sealed interface MetaPropAction<T>
class Replace<T>(val obj: T) : MetaPropAction<T>
class DoNothing<T> : MetaPropAction<T>

sealed interface MetaPropResult
data object NotPresent : MetaPropResult

open class VersionedTypedObsHolderSerializer<T : TypedObservableHolder>(
    cls: KClass<out T>,
    classVersion: Int
) : TypedObsHolderSerializer<T>(cls) {
    final override val metaProps =
        listOf(
            MetaProp(key = "classVersion", serializer = serializer<Int>(), onLoad = { loadedClassVersion ->
                if (loadedClassVersion != classVersion) {
                    Replace(
                        newInstance().apply {
                            warn("class version not compatible! ($loadedClassVersion!=$classVersion)")
                            wasResetBecauseSerializedDataWasWrongClassVersion = true
                        }
                    )
                } else DoNothing()
            }, onNotFound = {
                warn("did not check class version!")
                Replace(
                    newInstance().apply {
                        wasResetBecauseSerializedDataWasWrongClassVersion = true
                    }
                )
            }, getValueForSerializing = { classVersion })
        )
}

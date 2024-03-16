package matt.obs.hold.extra

import kotlinx.serialization.serializer
import matt.log.warn.common.warn
import matt.obs.common.DoNothing
import matt.obs.common.MetaProp
import matt.obs.common.MyCustomDecoderAndEncoder
import matt.obs.common.Replace
import matt.obs.hold.TypedObservableHolder
import matt.obs.hold.j.TypedObsHolderSerializer
import kotlin.reflect.KClass


open class VersionedTypedObsHolderSerializer<T : TypedObservableHolder>(
    private val cls: KClass<out T>,
    private val classVersion: Int
) : TypedObsHolderSerializer<T>(cls) {
    final override fun metaProps(t: MyCustomDecoderAndEncoder<T>): List<MetaProp<T, *>> =
        listOf(
            MetaProp(
                key = "classVersion",
                serializer = serializer<Int>(),
                onLoad = { loadedClassVersion ->
                    if (loadedClassVersion != classVersion) {
                        Replace(
                            newInstance().apply {
                                warn("class version not compatible! ($loadedClassVersion!=$classVersion)")
                                wasResetBecauseSerializedDataWasWrongClassVersion = true
                            }
                        )
                    } else DoNothing()
                },
                onNotFound = {
                    warn("did not check class version!")
                    Replace(
                        newInstance().apply {
                            wasResetBecauseSerializedDataWasWrongClassVersion = true
                        }
                    )
                },
                getValueForSerializing = { classVersion },
                contenxt = t,
                cast = {
                    it as Int
                }
            )

        )
}

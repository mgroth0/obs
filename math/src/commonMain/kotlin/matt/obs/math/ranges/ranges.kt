package matt.obs.math.ranges

import matt.obs.math.int.ObsI
import matt.obs.math.long.ObsL
import matt.obs.prop.BindableProperty

operator fun ObsI.rangeTo(other: ObsI): Sequence<ObsI> = value.rangeTo(other.value).asSequence().map(::BindableProperty)

operator fun ObsI.rangeTo(other: Int): Sequence<ObsI> = value.rangeTo(other).asSequence().map(::BindableProperty)

operator fun ObsI.rangeTo(other: ObsL): Sequence<ObsL> = value.rangeTo(other.value).asSequence().map(::BindableProperty)

operator fun ObsI.rangeTo(other: Long): Sequence<ObsL> = value.rangeTo(other).asSequence().map(::BindableProperty)


operator fun ObsL.rangeTo(other: ObsL): Sequence<ObsL> = value.rangeTo(other.value).asSequence().map(::BindableProperty)

operator fun ObsL.rangeTo(other: Long): Sequence<ObsL> = value.rangeTo(other).asSequence().map(::BindableProperty)

operator fun ObsL.rangeTo(other: ObsI): Sequence<ObsL> = value.rangeTo(other.value).asSequence().map(::BindableProperty)

operator fun ObsL.rangeTo(other: Int): Sequence<ObsL> = value.rangeTo(other).asSequence().map(::BindableProperty)
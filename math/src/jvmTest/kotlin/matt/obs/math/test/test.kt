package matt.obs.math.test


import matt.obs.math.int.op.div
import matt.obs.math.int.op.minus
import matt.obs.math.int.op.plus
import matt.obs.math.int.op.rem
import matt.obs.math.int.op.times
import matt.obs.prop.BindableProperty
import matt.test.Tests
import kotlin.test.Test
import kotlin.test.assertEquals

class MathTests : Tests() {
    @Test
    fun mathBindings() {
        val intProp = BindableProperty(1)
        (intProp * 10).apply {
            intProp.value = 10
            assertEquals(100, value)
        }
        (intProp / 10).apply {
            intProp.value = 10
            assertEquals(1, value)
        }
        (intProp + 5).apply {
            intProp.value = 7
            assertEquals(12, value)
        }
        (intProp - 5).apply {
            intProp.value = 7
            assertEquals(2, value)
        }
        (intProp % 5).apply {
            intProp.value = 13
            assertEquals(3, value)
        }
    }
}

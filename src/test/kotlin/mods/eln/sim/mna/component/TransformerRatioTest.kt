package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals

class TransformerRatioTest {
    @Test
    fun setAndGetRatioUseAccessors() {
        val transformer = Transformer()
        transformer.setRatio(2.5)
        assertEquals(2.5, transformer.getRatio())
    }
}

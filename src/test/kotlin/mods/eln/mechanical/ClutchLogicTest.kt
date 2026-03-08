package mods.eln.mechanical

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClutchLogicTest {
    @Test
    fun clutchCannotStopSlippingWhenLeftSideIsStatic() {
        assertFalse(canClutchStopSlipping(StaticShaftNetwork(), ShaftNetwork()))
    }

    @Test
    fun clutchCannotStopSlippingWhenRightSideIsStatic() {
        assertFalse(canClutchStopSlipping(ShaftNetwork(), StaticShaftNetwork()))
    }

    @Test
    fun clutchCanStopSlippingWhenBothSidesRotateFreely() {
        assertTrue(canClutchStopSlipping(ShaftNetwork(), ShaftNetwork()))
    }
}

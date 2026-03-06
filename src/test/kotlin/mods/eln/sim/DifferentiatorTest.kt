package mods.eln.sim

import kotlin.test.Test
import kotlin.test.assertEquals

class DifferentiatorTest {
    @Test
    fun startupUsesFirstOrderDifference() {
        val differentiator = Differentiator()
        val dt = 0.5

        val first = differentiator.nextStep(1.0, dt)
        val second = differentiator.nextStep(2.0, dt)
        val third = differentiator.nextStep(3.0, dt)
        val fourth = differentiator.nextStep(4.0, dt)

        assertEquals(2.0, first, 1e-12)
        assertEquals(2.0, second, 1e-12)
        assertEquals(2.0, third, 1e-12)
        assertEquals(2.0, fourth, 1e-12)
    }

    @Test
    fun higherOrderStencilTracksLinearSlopeAfterWarmup() {
        val differentiator = Differentiator()
        val dt = 0.1
        val slope = 3.5

        var last = 0.0
        for (step in 0..8) {
            val t = step * dt
            last = differentiator.nextStep(slope * t, dt)
        }

        assertEquals(slope, last, 1e-12)
    }

    @Test
    fun resetRestartsFromColdState() {
        val differentiator = Differentiator()
        val dt = 0.25

        repeat(8) { i -> differentiator.nextStep(i * dt, dt) }
        differentiator.reset()

        val afterReset = differentiator.nextStep(2.0, dt)
        assertEquals(22.0 / (6.0 * dt), afterReset, 1e-12)
    }
}

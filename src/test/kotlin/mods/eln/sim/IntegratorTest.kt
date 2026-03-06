package mods.eln.sim

import kotlin.test.Test
import kotlin.test.assertEquals

class IntegratorTest {
    @Test
    fun constantInputMatchesExpectedSequence() {
        val integrator = Integrator()
        val dt = 1.0
        val actual = List(6) { integrator.nextStep(1.0, dt) }
        val expected = listOf(
            14.0 / 45.0,
            78.0 / 45.0,
            102.0 / 45.0,
            166.0 / 45.0,
            194.0 / 45.0,
            258.0 / 45.0
        )

        expected.zip(actual).forEach { (exp, got) ->
            assertEquals(exp, got, 1e-12)
        }
    }

    @Test
    fun impulseInputShowsRoundRobinAccumulatorPhases() {
        val integrator = Integrator()
        val dt = 1.0
        val input = listOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val actual = input.map { integrator.nextStep(it, dt) }
        val expected = listOf(
            14.0 / 45.0,
            64.0 / 45.0,
            24.0 / 45.0,
            64.0 / 45.0,
            28.0 / 45.0,
            64.0 / 45.0,
            24.0 / 45.0,
            64.0 / 45.0
        )

        expected.zip(actual).forEach { (exp, got) ->
            assertEquals(exp, got, 1e-12)
        }
    }

    @Test
    fun resetClearsSamplesAndAccumulatedIntegrations() {
        val integrator = Integrator()
        val dt = 0.25

        repeat(10) { integrator.nextStep(2.0, dt) }
        integrator.reset()

        val firstAfterReset = integrator.nextStep(2.0, dt)
        assertEquals(14.0 / 45.0 * 2.0 * dt, firstAfterReset, 1e-12)
    }
}

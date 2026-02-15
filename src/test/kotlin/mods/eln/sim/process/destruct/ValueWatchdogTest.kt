package mods.eln.sim.process.destruct

import kotlin.test.Test
import kotlin.test.assertTrue
import mods.eln.disableLog4jJmx

private class StubDestructible : IDestructible {
    var destroyed = false
    override fun destructImpl() {
        destroyed = true
    }

    override fun describe(): String = "stub"
}

private class TestWatchdog(private var watchedValue: Double) : ValueWatchdog() {
    var destroyed = false
    var destroyCalled = false

    override fun getValue(): Double = watchedValue

    override fun onDestroy(value: Double, overflow: Double) {
        destroyCalled = true
    }
}

class ValueWatchdogTest {
    @Test
    fun processTriggersDestroyAfterOverflow() {
        disableLog4jJmx()
        val watchdog = TestWatchdog(10.0)
        val target = StubDestructible()
        watchdog.setDestroys(target)
        watchdog.max = 1.0
        watchdog.min = -1.0
        watchdog.timeoutReset = 0.0

        watchdog.process(1.0)
        watchdog.process(1.0)

        assertTrue(watchdog.destroyCalled)
        assertTrue(target.destroyed)
    }

    @Test
    fun resetRestoresBoot() {
        val watchdog = TestWatchdog(0.0)
        watchdog.boot = false
        watchdog.reset()
        assertTrue(watchdog.boot)
    }
}

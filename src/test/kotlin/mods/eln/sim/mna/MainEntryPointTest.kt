package mods.eln.sim.mna

import kotlin.test.Test

class MainEntryPointTest {
    @Test
    fun subsystemMainRuns() {
        disableLog4jJmx()
        SubSystem.main(emptyArray())
    }

    @Test
    fun rootSystemMainRuns() {
        disableLog4jJmx()
        RootSystem.main(emptyArray())
    }
}

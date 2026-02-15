package mods.eln.sim.mna

internal fun disableLog4jJmx() {
    System.setProperty("log4j2.disable.jmx", "true")
}

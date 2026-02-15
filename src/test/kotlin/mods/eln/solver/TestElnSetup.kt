package mods.eln.solver

import mods.eln.Eln
import mods.eln.misc.FunctionTable

internal fun ensureBatteryVoltageTable(table: FunctionTable) {
    if (Eln.instance == null) {
        Eln.instance = Eln()
    }
    Eln.instance.batteryVoltageFunctionTable = table
}

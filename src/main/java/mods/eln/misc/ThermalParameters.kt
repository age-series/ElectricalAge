package mods.eln.misc

data class ThermalParameters(var thermalHeatTime: Double, var thermalMaxTemp: Double?, var thermalMinTemp: Double?) {
    companion object {
        fun battery(): ThermalParameters {
            return ThermalParameters(60.0, 100.0, -40.0)
        }
    }
}

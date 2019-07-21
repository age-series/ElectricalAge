package mods.eln.misc

import java.util.HashMap

/**
 * Generic Material (acts like Copper by default)
 */
class MaterialProperties {
    companion object {

        private val materials = HashMap<MaterialType, MaterialData>()
        private var prepared = false

        @JvmStatic
        fun prepare() {
            materials[MaterialType.COPPER] = MaterialData(
                1.68,
                385.0,
                2530.0)
            materials[MaterialType.RUBBER] = MaterialData(
                1e5,
                0.15,
                Double.MAX_VALUE)
            prepared = true
        }

        /**
         * get Electrical Resistivity
         *
         * @return resistivity (ohms/meter)
         */
        @JvmStatic
        fun getElectricalResistivity(type: MaterialType): Double {
            if (!prepared) {
                prepare()
            }
            return (materials[type]?.ELECTRICAL_RESISTIVITY ?: 1.68) * 10e-8
        }

        /*

Electrical Resistivity

Material    Resistivity ρ (ohm m)

Silver:     1.59    x10^-8
Copper:     1.68    x10^-8
Aluminum:   2.65    x10^-8
Tungsten:   5.6     x10^-8
Iron:       9.71    x10^-8
Platinum    10.6    x10^-8
Lead:       22      x10^-8
Mercury     98      x10^-8
Glass       1-10000 x10^-9
Rubber      1-100   x10^-13

Source: http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/rstiv.html

*/

        /**
         * get Thermal Conductivity
         *
         * @return thermal conductivity (W/m K)
         */
        @JvmStatic
        fun getThermalConductivity(type: MaterialType): Double {
            if (!prepared) {
                prepare()
            }
            return materials[type]?.THERMAL_CONDUCTIVITY ?: 385.0
        }
    }

    /*

Thermal Conductivity

Material    Thermal conductivity (W/m K)*
Diamond:    1000
Silver:     406.0
Copper:     385.0
Gold:       314
Brass:      109.0
Aluminum:   205.0
Iron:       79.5
Steel:      50.2
Lead:       34.7
Mercury:    8.3   // I question using this because of convection
Glass:      0.8
Water:      0.6   // I question using this because of convection
Air:        0.024 // I question using this because of convection

Rubber, Vulcanized: 0.15

Source: http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/thrcn.html
https://www.electronics-cooling.com/2001/11/the-thermal-conductivity-of-rubbers-elastomers/

*/

    // TODO: Get recommended amperage per material type per mm^2. (remember, skin effect on AC signals)

    // TODO: Replace Fusing Current with Dielectric Strength? Fusing current is honestly kinda useless...
    private data class MaterialData(val ELECTRICAL_RESISTIVITY: Double, val THERMAL_CONDUCTIVITY: Double, val FUSING_CURRENT: Double)
}

enum class MaterialType {
    COPPER,
    RUBBER
}

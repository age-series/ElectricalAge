package mods.eln.config

import mods.eln.Eln
import mods.eln.Other
import mods.eln.entity.ReplicatorPopProcess
import mods.eln.misc.Utils
import java.util.*
import kotlin.math.max
import kotlin.math.min

object ConfigHandler {
    fun loadConfig(eln: Eln) {
        Eln.config.load()

        //Hacks for correct long date typing failures in config file
        //WARNING/BUG: "renameProperty" changes the type to String! However, read functions don't seem to care
        // attention to it, so it's OK... for the moment.
        if (Eln.config.hasKey("lamp", "incondescentLifeInHours")) Eln.config.renameProperty(
            "lamp",
            "incondescentLifeInHours",
            "incandescentLifeInHours"
        )
        if (Eln.config.hasKey("mapgenerate", "plumb")) Eln.config.renameProperty("mapgenerate", "plumb", "lead")
        if (Eln.config.hasKey("mapgenerate", "cooper")) Eln.config.renameProperty("mapgenerate", "cooper", "copper")
        if (Eln.config.hasKey("simulation", "electricalFrequancy")) Eln.config.renameProperty(
            "simulation",
            "electricalFrequancy",
            "electricalFrequency"
        )
        if (Eln.config.hasKey("simulation", "thermalFrequancy")) Eln.config.renameProperty(
            "simulation",
            "thermalFrequancy",
            "thermalFrequency"
        )

        Eln.modbusEnable = Eln.config["modbus", "enable", false, "Enable Modbus RTU"].getBoolean(false)
        Eln.modbusPort = Eln.config["modbus", "port", 1502, "TCP Port for Modbus RTU"].getInt(1502)
        Eln.debugEnabled = Eln.config["debug", "enable", false, "Enables debug printing spam"].getBoolean(false)
        Eln.debugExplosions = Eln.config["debug", "watchdog", false, "Watchdog Impl. check"].getBoolean(false)
        Eln.explosionEnable =
            Eln.config["gameplay", "explosion", false, "Make explosions a bit bigger"].getBoolean(true)

        Eln.versionCheckEnabled =
            Eln.config["general", "versionCheckEnable", true, "Enable version checker"].getBoolean(true)
        Eln.analyticsEnabled =
            Eln.config["general", "analyticsEnable", true, "Enable Analytics for Electrical Age"].getBoolean(true)
        Eln.analyticsURL =
            Eln.config["general", "analyticsURL", "http://eln.ja13.org/stat", "Set update checker URL"].string
        Eln.analyticsPlayerUUIDOptIn = Eln.config["general", "analyticsPlayerOptIn", false, "Opt into sending player " +
                "UUID when sending analytics"].getBoolean(false)
        Eln.enableFestivities = Eln.config["general", "enableFestiveItems", true, "Set this to false to enable grinch" +
                " mode"].boolean
        Eln.verticalIronCableCrafting = Eln.config["general", "verticalIronCableCrafting", false, "Set this to true " +
                "to craft with vertical ingots instead of horizontal ones"].boolean

        if (Eln.analyticsEnabled) {
            val p = Eln.config["general", "playerUUID", ""]
            if (p.string.length == 0) {
                Eln.playerUUID = UUID.randomUUID().toString()
                p.set(Eln.playerUUID)
            } else Eln.playerUUID = p.string
        }

        Eln.directPoles = Eln.config["general", "directPoles", true, "Enables direct air to ground poles"].boolean

        eln.heatTurbinePowerFactor = Eln.config["balancing", "heatTurbinePowerFactor", 1].getDouble(1.0)
        eln.solarPanelPowerFactor = Eln.config["balancing", "solarPanelPowerFactor", 1].getDouble(1.0)
        eln.windTurbinePowerFactor = Eln.config["balancing", "windTurbinePowerFactor", 1].getDouble(1.0)
        eln.waterTurbinePowerFactor = Eln.config["balancing", "waterTurbinePowerFactor", 1].getDouble(1.0)
        eln.fuelGeneratorPowerFactor = Eln.config["balancing", "fuelGeneratorPowerFactor", 1].getDouble(1.0)
        eln.fuelHeatFurnacePowerFactor = Eln.config["balancing", "fuelHeatFurnacePowerFactor", 1.0].double
        eln.autominerRange = Eln.config["balancing", "autominerRange", 10, "Maximum horizontal distance from autominer " +
                "that will be mined"].getInt(10)

        Other.wattsToEu =
            Eln.config["balancing", "ElnToIndustrialCraftConversionRatio", 1.0 / 3.0, "Watts to EU"].getDouble(1.0 / 3.0)
        Other.wattsToOC = Eln.config["balancing", "ElnToOpenComputerConversionRatio", 1.0 / 3.0 / 2.5, "Watts to OC " +
                "Power"].getDouble(1.0 / 3.0 / 2.5)
        Other.wattsToRf = Eln.config["balancing", "ElnToThermalExpansionConversionRatio", 1.0 / 3.0 * 4, "Watts to " +
                "RF"].getDouble(1.0 / 3.0 * 4)
        eln.plateConversionRatio = Eln.config["balancing", "platesPerIngot", 1, "Plates made per ingot"].getInt(1)
        Eln.shaftEnergyFactor = Eln.config["balancing", "shaftEnergyFactor", 0.05].getDouble(0.05)

        eln.stdBatteryHalfLife = Eln.config["battery", "batteryHalfLife", 2, "How many days it takes for a battery to " +
                "decay half way"].getDouble(2.0) * Utils.minecraftDay
        eln.batteryCapacityFactor = Eln.config["balancing", "batteryCapacityFactor", 1].getDouble(1.0)

        eln.ComputerProbeEnable = Eln.config["compatibility", "ComputerProbeEnable", true, "Enable the OC/CC <-> Eln " +
                "Computer Probe"].getBoolean(true)
        eln.ElnToOtherEnergyConverterEnable =
            Eln.config["compatibility", "ElnToOtherEnergyConverterEnable", true, "Enable the Eln Energy Exporter"].getBoolean(
                true
            )

        eln.replicatorPop = Eln.config["entity", "replicatorPop", false, "Enable the replicator mob"].getBoolean(false)
        ReplicatorPopProcess.popPerSecondPerPlayer =
            Eln.config["entity", "replicatorPopWhenThunderPerSecond", 1.0 / 120].getDouble(1.0 / 120)
        eln.replicatorRegistrationId = Eln.config["entity", "replicatorId", -1].getInt(-1)
        eln.killMonstersAroundLamps = Eln.config["entity", "killMonstersAroundLamps", true].getBoolean(true)
        eln.killMonstersAroundLampsRange = Eln.config["entity", "killMonstersAroundLampsRange", 9].getInt(9)
        eln.maxReplicators = Eln.config["entity", "maxReplicators", 100].getInt(100)

        eln.forceOreRegen = Eln.config["mapGenerate", "forceOreRegen", false].getBoolean(false)
        Eln.genCopper = Eln.config["mapGenerate", "copper", true].getBoolean(true)
        Eln.genLead = Eln.config["mapGenerate", "lead", true].getBoolean(true)
        Eln.genTungsten = Eln.config["mapGenerate", "tungsten", true].getBoolean(true)
        Eln.genCinnabar = Eln.config["mapGenerate", "cinnabar", true].getBoolean(true)
        Eln.genCinnabar = false

        Eln.oredictTungsten = Eln.config["dictionary", "tungsten", false].getBoolean(false)
        if (Eln.oredictTungsten) {
            Eln.dictTungstenOre = "oreTungsten"
            Eln.dictTungstenDust = "dustTungsten"
            Eln.dictTungstenIngot = "ingotTungsten"
        } else {
            Eln.dictTungstenOre = "oreElnTungsten"
            Eln.dictTungstenDust = "dustElnTungsten"
            Eln.dictTungstenIngot = "ingotElnTungsten"
        }
        Eln.oredictChips = Eln.config["dictionary", "chips", true].getBoolean(true)
        if (Eln.oredictChips) {
            Eln.dictCheapChip = "circuitBasic"
            Eln.dictAdvancedChip = "circuitAdvanced"
        } else {
            Eln.dictCheapChip = "circuitElnBasic"
            Eln.dictAdvancedChip = "circuitElnAdvanced"
        }

        eln.incandescentLampLife = Eln.config["lamp", "incandescentLifeInHours", 16.0].getDouble(16.0)
        eln.economicLampLife = Eln.config["lamp", "economicLifeInHours", 64.0].getDouble(64.0)
        eln.carbonLampLife = Eln.config["lamp", "carbonLifeInHours", 6.0].getDouble(6.0)
        eln.ledLampLife = Eln.config["lamp", "ledLifeInHours", 512.0].getDouble(512.0)
        Eln.ledLampInfiniteLife = Eln.config["lamp", "infiniteLedLife", false].boolean
        Eln.allowSwingingLamps = Eln.config["lamp", "swingingLamps", true].boolean

        eln.fuelGeneratorTankCapacity =
            Eln.config["fuelGenerator", "tankCapacityInSecondsAtNominalPower", 20 * 60].getDouble((20 * 60).toDouble())

        eln.addOtherModOreToXRay = Eln.config["xrayscannerconfig", "addOtherModOreToXRay", true].getBoolean(true)
        eln.xRayScannerRange = Eln.config["xrayscannerconfig", "rangeInBloc", 5.0, "X-Ray Scanner range; set " +
                "between 4 and 10 blocks"].getDouble(5.0).toFloat()
        eln.xRayScannerRange = max(min(eln.xRayScannerRange.toDouble(), 10.0), 4.0).toFloat()
        eln.xRayScannerCanBeCrafted = Eln.config["xrayscannerconfig", "canBeCrafted", true].getBoolean(true)

        eln.electricalFrequency =
            Eln.config["simulation", "electricalFrequency", 20, "Set to a clean divisor of 20"].getDouble(20.0)
        eln.electricalInterSystemOverSampling = Eln.config["simulation", "electricalInterSystemOverSampling", 50, "You " +
                "don't want to set this lower than 50."].getInt(50)
        eln.thermalFrequency =
            Eln.config["simulation", "thermalFrequency", 400, "I wouldn't touch this one either"].getDouble(400.0)

        Eln.wirelessTxRange = Eln.config["wireless", "txRange", 32, "Maximum range for wireless transmitters to be " +
                "recieved, as well as lamp supplies"].int

        Eln.wailaEasyMode =
            Eln.config["balancing", "wailaEasyMode", false, "Display more detailed WAILA info on some " +
                    "machines (good for creative mode)"].getBoolean(false)
        Eln.cablePowerFactor =
            Eln.config["balancing", "cablePowerFactor", 1.0, "Multiplication factor for cable power " +
                    "capacity. We recommend 2.0 to 4.0 for larger modpacks, but 1.0 for Eln standalone, or if you like a " +
                    "challenge.", 0.5, 4.0].getDouble(1.0)

        Eln.fuelHeatValueFactor = Eln.config["balancing", "fuelHeatValueFactor", 0.0000675, "Factor to apply when " +
                "converting real word heat values to Minecraft heat values (1mB = 1l)."].double

        Eln.noSymbols = Eln.config["general", "noSymbols", false, "Show the item instead of the electrical symbol as " +
                "an icon"].boolean
        Eln.noVoltageBackground = Eln.config["general", "noVoltageBackground", false, "Disable colored background to " +
                "items"].boolean

        Eln.maxSoundDistance = Eln.config["debug", "maxSoundDistance", 16.0, "Set this lower if you have clipping " +
                "sounds in spaces with many sound sources (generators)"].double
        Eln.soundChannels = Eln.config["debug", "soundChannels", 200, "Change the number of sound channels. Set to -1" +
                " to use default"].getInt(200)

        Eln.flywheelMass = min(
            max(
                Eln.config["balancing", "flywheelMass", 50.0, "How heavy is *your* " +
                        "flywheel?"].double, 1.0
            ), 1000.0
        )

        Eln.config.save()
    }
}
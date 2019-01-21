package mods.eln;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.misc.Utils;
import mods.eln.server.SaveConfig;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.UUID;

public class ConfigHandler {

    public static SaveConfig config;

    public ConfigHandler() {

    }

    public void readConfig(FMLPreInitializationEvent event) {

        Configuration config = new Configuration(
            event.getSuggestedConfigurationFile());
        config.load();

        //Hacks for correct long date typing failures in config file
        //WARNING/BUG: "renameProperty" changes the type to String! However read functions don't seem to care attention to it, so it's OK... for the moment.
        if (config.hasKey("lamp", "incondescentLifeInHours"))
            config.renameProperty("lamp", "incondescentLifeInHours", "incandescentLifeInHours");
        if (config.hasKey("mapgenerate", "plumb"))
            config.renameProperty("mapgenerate", "plumb", "lead");
        if (config.hasKey("mapgenerate", "cooper"))
            config.renameProperty("mapgenerate", "cooper", "copper");
        if (config.hasKey("simulation", "electricalFrequancy"))
            config.renameProperty("simulation", "electricalFrequancy", "electricalFrequency");
        if (config.hasKey("simulation", "thermalFrequancy"))
            config.renameProperty("simulation", "thermalFrequancy", "thermalFrequency");


        Vars.modbusEnable = config.get("modbus", "enable", false).getBoolean(false);
        Vars.modbusPort = config.get("modbus", "port", 1502).getInt(1502);
        Vars.debugEnabled = config.get("debug", "enable", false).getBoolean(false);

        Vars.explosionEnable = config.get("gameplay", "explosion", true).getBoolean(true);

        //explosionEnable = false;
        Vars.versionCheckEnabled = config.get("general", "versionCheckEnable", true).getBoolean(true);
        Vars.analyticsEnabled = config.get("general", "analyticsEnable", true).getBoolean(true);

        if (Vars.analyticsEnabled) {
            final Property p = config.get("general", "playerUUID", "");
            if (p.getString().length() == 0) {
                Vars.playerUUID = UUID.randomUUID().toString();
                p.set(Vars.playerUUID);
            } else
                Vars.playerUUID = p.getString();
        }

        Vars.heatTurbinePowerFactor = config.get("balancing", "heatTurbinePowerFactor", 1).getDouble(1);
        Vars.solarPanelPowerFactor = config.get("balancing", "solarPanelPowerFactor", 1).getDouble(1);
        Vars.windTurbinePowerFactor = config.get("balancing", "windTurbinePowerFactor", 1).getDouble(1);
        Vars.waterTurbinePowerFactor = config.get("balancing", "waterTurbinePowerFactor", 1).getDouble(1);
        Vars.fuelGeneratorPowerFactor = config.get("balancing", "fuelGeneratorPowerFactor", 1).getDouble(1);
        Vars.fuelHeatFurnacePowerFactor = config.get("balancing", "fuelHeatFurnacePowerFactor", 1.0).getDouble();
        Vars.autominerRange = config.get("balancing", "autominerRange", 10, "Maximum horizontal distance from autominer that will be mined").getInt(10);

        Vars.ElnToIc2ConversionRatio = config.get("balancing", "ElnToIndustrialCraftConversionRatio", 1.0 / 3.0).getDouble(1.0 / 3.0);
        Vars.ElnToOcConversionRatio = config.get("balancing", "ElnToOpenComputerConversionRatio", 1.0 / 3.0 / 2.5).getDouble(1.0 / 3.0 / 2.5);
        Vars.ElnToTeConversionRatio = config.get("balancing", "ElnToThermalExpansionConversionRatio", 1.0 / 3.0 * 4).getDouble(1.0 / 3.0 * 4);
        //	Other.ElnToBuildcraftConversionRatio = config.get("balancing", "ElnToBuildcraftConversionRatio", 1.0 / 3.0 / 5 * 2).getDouble(1.0 / 3.0 / 5 * 2);
        Vars.plateConversionRatio = config.get("balancing", "platesPerIngot", 1).getInt(1);

        Vars.stdBatteryHalfLife = config.get("battery", "batteryHalfLife", 2, "How many days it takes for a battery to decay half way").getDouble(2) * Utils.minecraftDay;
        Vars.batteryCapacityFactor = config.get("balancing", "batteryCapacityFactor", 1.).getDouble(1.);

        Vars.ComputerProbeEnable = config.get("compatibility", "ComputerProbeEnable", true).getBoolean(true);
        Vars.ElnToOtherEnergyConverterEnable = config.get("compatibility", "ElnToOtherEnergyConverterEnable", true).getBoolean(true);

        Vars.replicatorPop = config.get("entity", "replicatorPop", true).getBoolean(true);
        ReplicatorPopProcess.popPerSecondPerPlayer = config.get("entity", "replicatorPopWhenThunderPerSecond", 1.0 / 120).getDouble(1.0 / 120);
        Vars.replicatorRegistrationId = config.get("entity", "replicatorId", -1).getInt(-1);
        Vars.killMonstersAroundLamps = config.get("entity", "killMonstersAroundLamps", true).getBoolean(true);
        Vars.killMonstersAroundLampsRange = config.get("entity", "killMonstersAroundLampsRange", 9).getInt(9);
        Vars.maxReplicators = config.get("entity", "maxReplicators", 100).getInt(100);

        Vars.forceOreRegen = config.get("mapGenerate", "forceOreRegen", false).getBoolean(false);
        Vars.genCopper = config.get("mapGenerate", "copper", true).getBoolean(true);
        Vars.genLead = config.get("mapGenerate", "lead", true).getBoolean(true);
        Vars.genTungsten = config.get("mapGenerate", "tungsten", true).getBoolean(true);
        Vars.genCinnabar = config.get("mapGenerate", "cinnabar", true).getBoolean(true);
        Vars.genCinnabar = false;

        Vars.oredictTungsten = config.get("dictionary", "tungsten", false).getBoolean(false);
        if (Vars.oredictTungsten) {
            Vars.dictTungstenOre = "oreTungsten";
            Vars.dictTungstenDust = "dustTungsten";
            Vars.dictTungstenIngot = "ingotTungsten";
        } else {
            Vars.dictTungstenOre = "oreElnTungsten";
            Vars.dictTungstenDust = "dustElnTungsten";
            Vars.dictTungstenIngot = "ingotElnTungsten";
        }
        Vars.oredictChips = config.get("dictionary", "chips", true).getBoolean(true);
        if (Vars.oredictChips) {
            Vars.dictCheapChip = "circuitBasic";
            Vars.dictAdvancedChip = "circuitAdvanced";
        } else {
            Vars.dictCheapChip = "circuitElnBasic";
            Vars.dictAdvancedChip = "circuitElnAdvanced";
        }

        Vars.incandescentLampLife = config.get("lamp", "incandescentLifeInHours", 16.0).getDouble(16.0) * 3600;
        Vars.economicLampLife = config.get("lamp", "economicLifeInHours", 64.0).getDouble(64.0) * 3600;
        Vars.carbonLampLife = config.get("lamp", "carbonLifeInHours", 6.0).getDouble(6.0) * 3600;
        Vars.ledLampLife = config.get("lamp", "ledLifeInHours", 512.0).getDouble(512.0) * 3600;
        Vars.ledLampInfiniteLife = config.get("lamp", "infiniteLedLife", false).getBoolean();

        Vars.fuelGeneratorTankCapacity = config.get("fuelGenerator",
            "tankCapacityInSecondsAtNominalPower", 20 * 60).getDouble(20 * 60);

        Vars.addOtherModOreToXRay = config.get("xrayscannerconfig", "addOtherModOreToXRay", true).getBoolean(true);
        Vars.xRayScannerRange = (float) config.get("xrayscannerconfig", "rangeInBloc", 5.0).getDouble(5.0);
        Vars.xRayScannerRange = Math.max(Math.min(Vars.xRayScannerRange, 10), 4);
        Vars.xRayScannerCanBeCrafted = config.get("xrayscannerconfig", "canBeCrafted", true).getBoolean(true);

        Vars.electricalFrequency = config.get("simulation", "electricalFrequency", 20).getDouble(20);
        Vars.electricalInterSystemOverSampling = config.get("simulation", "electricalInterSystemOverSampling", 50).getInt(50);
        Vars.thermalFrequency = config.get("simulation", "thermalFrequency", 400).getDouble(400);
        Vars.cableRsFactor = config.get("simulation", "cableRsFactor", 1.0).getDouble(1.0);
        Vars.cablePace = config.get("simulation", "cablePace", 1.0).getDouble(1.0);

        Vars.wirelessTxRange = config.get("wireless", "txRange", 32).getInt();

        Vars.wailaEasyMode = config.get("balancing", "wailaEasyMode", false, "Display more detailed WAILA info on some machines").getBoolean(false);

        Vars.fuelHeatValueFactor = config.get("balancing", "fuelHeatValueFactor", 0.0000675,
            "Factor to apply when converting real word heat values to Minecraft heat values (1mB = 1l).").getDouble();

        Vars.noSymbols = config.get("general", "noSymbols", false).getBoolean();
        Vars.noVoltageBackground = config.get("general", "noVoltageBackground", false).getBoolean();

        Vars.maxSoundDistance = config.get("debug", "maxSoundDistance", 16.0).getDouble();

        config.save();

    }
}

package mods.eln.misc;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import mods.eln.Eln;
import mods.eln.Other;
import mods.eln.debug.DebugType;
import mods.eln.entity.ReplicatorPopProcess;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.UUID;

public class ConfigHandler {
    public static void loadConfig(FMLPreInitializationEvent event) {
        // The absolute first thing to do is load the configuration file so that if anything goes wrong, the DebugPrint class has been prepared.
        // All prints before that point are handled with logger.info()

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
        if (config.hasKey("balancing", "cablePowerFactor"))
            config.renameProperty("balancing", "cablePowerFactor", "cableFactor");


        Eln.modbusEnable = config.get("modbus", "enable", false).getBoolean(false);
        Eln.modbusPort = config.get("modbus", "port", 1502).getInt(1502);
        Eln.debugEnabled = config.get("debug", "enable", false).getBoolean(false);

        Eln.explosionEnable = config.get("gameplay", "explosion", true).getBoolean(true);

        Eln.versionCheckEnabled = config.get("general", "versionCheckEnable", true, "Enable version checker").getBoolean(true);
        Eln.analyticsEnabled = config.get("general", "analyticsEnable", true, "Enable Analytics for Electrical Age").getBoolean(true);
        Eln.analyticsURL = config.get("general", "analyticsURL", "http://eln.ja13.org/stat", "Set update checker URL").getString();
        Eln.analyticsPlayerUUIDOptIn = config.get("general", "analyticsPlayerOptIn", false, "Opt into sending player UUID when sending analytics (default DISABLED)").getBoolean(false);

        if (Eln.analyticsEnabled) {
            final Property p = config.get("general", "playerUUID", "");
            if (p.getString().length() == 0) {
                Eln.playerUUID = UUID.randomUUID().toString();
                p.set(Eln.playerUUID);
            } else
                Eln.playerUUID = p.getString();
        }

        Eln.heatTurbinePowerFactor = config.get("balancing", "heatTurbinePowerFactor", 1).getDouble(1);
        Eln.solarPanelPowerFactor = config.get("balancing", "solarPanelPowerFactor", 1).getDouble(1);
        Eln.windTurbinePowerFactor = config.get("balancing", "windTurbinePowerFactor", 1).getDouble(1);
        Eln.waterTurbinePowerFactor = config.get("balancing", "waterTurbinePowerFactor", 1).getDouble(1);
        Eln.fuelGeneratorPowerFactor = config.get("balancing", "fuelGeneratorPowerFactor", 1).getDouble(1);
        Eln.fuelHeatFurnacePowerFactor = config.get("balancing", "fuelHeatFurnacePowerFactor", 1.0).getDouble();
        Eln.autominerRange = config.get("balancing", "autominerRange", 10, "Maximum horizontal distance from autominer that will be mined").getInt(10);

        Other.ElnToIc2ConversionRatio = config.get("balancing", "ElnToIndustrialCraftConversionRatio", 1.0 / 3.0).getDouble(1.0 / 3.0);
        Other.ElnToOcConversionRatio = config.get("balancing", "ElnToOpenComputerConversionRatio", 1.0 / 3.0 / 2.5).getDouble(1.0 / 3.0 / 2.5);
        Other.ElnToTeConversionRatio = config.get("balancing", "ElnToThermalExpansionConversionRatio", 1.0 / 3.0 * 4).getDouble(1.0 / 3.0 * 4);
        //	Other.ElnToBuildcraftConversionRatio = config.get("balancing", "ElnToBuildcraftConversionRatio", 1.0 / 3.0 / 5 * 2).getDouble(1.0 / 3.0 / 5 * 2);
        Eln.plateConversionRatio = config.get("balancing", "platesPerIngot", 1).getInt(1);
        Eln.shaftEnergyFactor = config.get("balancing", "shaftEnergyFactor", 0.05).getDouble(0.05);

        Eln.stdBatteryHalfLife = config.get("battery", "batteryHalfLife", 2, "How many days it takes for a battery to decay half way").getDouble(2) * Utils.minecraftDay;
        Eln.batteryCapacityFactor = config.get("balancing", "batteryCapacityFactor", 1.).getDouble(1.);

        Eln.ComputerProbeEnable = config.get("compatibility", "ComputerProbeEnable", true).getBoolean(true);
        Eln.ElnToOtherEnergyConverterEnable = config.get("compatibility", "ElnToOtherEnergyConverterEnable", true).getBoolean(true);

        Eln.replicatorPop = config.get("entity", "replicatorPop", true).getBoolean(true);
        ReplicatorPopProcess.popPerSecondPerPlayer = config.get("entity", "replicatorPopWhenThunderPerSecond", 1.0 / 120).getDouble(1.0 / 120);
        Eln.replicatorRegistrationId = config.get("entity", "replicatorId", -1).getInt(-1);
        Eln.killMonstersAroundLamps = config.get("entity", "killMonstersAroundLamps", true).getBoolean(true);
        Eln.killMonstersAroundLampsRange = config.get("entity", "killMonstersAroundLampsRange", 9).getInt(9);
        Eln.maxReplicators = config.get("entity", "maxReplicators", 100).getInt(100);

        Eln.forceOreRegen = config.get("mapGenerate", "forceOreRegen", false).getBoolean(false);
        Eln.genCopper = config.get("mapGenerate", "copper", true).getBoolean(true);
        Eln.genLead = config.get("mapGenerate", "lead", true).getBoolean(true);
        Eln.genTungsten = config.get("mapGenerate", "tungsten", true).getBoolean(true);
        Eln.genCinnabar = config.get("mapGenerate", "cinnabar", true).getBoolean(true);
        Eln.genCinnabar = false;

        Eln.oredictTungsten = config.get("dictionary", "tungsten", false).getBoolean(false);
        if (Eln.oredictTungsten) {
            Eln.dictTungstenOre = "oreTungsten";
            Eln.dictTungstenDust = "dustTungsten";
            Eln.dictTungstenIngot = "ingotTungsten";
        } else {
            Eln.dictTungstenOre = "oreElnTungsten";
            Eln.dictTungstenDust = "dustElnTungsten";
            Eln.dictTungstenIngot = "ingotElnTungsten";
        }
        Eln.oredictChips = config.get("dictionary", "chips", true).getBoolean(true);
        if (Eln.oredictChips) {
            Eln.dictCheapChip = "circuitBasic";
            Eln.dictAdvancedChip = "circuitAdvanced";
        } else {
            Eln.dictCheapChip = "circuitElnBasic";
            Eln.dictAdvancedChip = "circuitElnAdvanced";
        }

        Eln.incandescentLampLife = config.get("lamp", "incandescentLifeInHours", 16.0).getDouble(16.0) * 3600;
        Eln.economicLampLife = config.get("lamp", "economicLifeInHours", 64.0).getDouble(64.0) * 3600;
        Eln.carbonLampLife = config.get("lamp", "carbonLifeInHours", 6.0).getDouble(6.0) * 3600;
        Eln.ledLampLife = config.get("lamp", "ledLifeInHours", 512.0).getDouble(512.0) * 3600;
        Eln.ledLampInfiniteLife = config.get("lamp", "infiniteLedLife", false).getBoolean();

        Eln.fuelGeneratorTankCapacity = config.get("fuelGenerator",
            "tankCapacityInSecondsAtNominalPower", 20 * 60).getDouble(20 * 60);

        Eln.addOtherModOreToXRay = config.get("xrayscannerconfig", "addOtherModOreToXRay", true).getBoolean(true);
        Eln.xRayScannerRange = (float) config.get("xrayscannerconfig", "rangeInBloc", 5.0).getDouble(5.0);
        Eln.xRayScannerRange = Math.max(Math.min(Eln.xRayScannerRange, 10), 4);
        Eln.xRayScannerCanBeCrafted = config.get("xrayscannerconfig", "canBeCrafted", true).getBoolean(true);

        Eln.electricalFrequency = config.get("simulation", "electricalFrequency", 20).getDouble(20);
        Eln.electricalInterSystemOverSampling = config.get("simulation", "electricalInterSystemOverSampling", 50).getInt(50);
        Eln.thermalFrequency = config.get("simulation", "thermalFrequency", 400).getDouble(400);

        Eln.wirelessTxRange = config.get("wireless", "txRange", 32).getInt();

        Eln.wailaEasyMode = config.get("balancing", "wailaEasyMode", false, "Display more detailed WAILA info on some machines").getBoolean(false);
        Eln.cableFactor = config.get("balancing", "cableFactor", 1.0, "Multiplication factor for cable power capacity. We recommend 2.0 to 4.0 for larger modpacks, but 1.0 for Eln standalone, or if you like a challenge.", 0.5, 4.0).getDouble(1.0);

        Eln.fuelHeatValueFactor = config.get("balancing", "fuelHeatValueFactor", 0.0000675,
            "Factor to apply when converting real word heat values to Minecraft heat values (1mB = 1l).").getDouble();

        Eln.noSymbols = config.get("general", "noSymbols", false).getBoolean();
        Eln.noVoltageBackground = config.get("general", "noVoltageBackground", false).getBoolean();

        Eln.maxSoundDistance = config.get("debug", "maxSoundDistance", 16.0).getDouble();

        Eln.cableResistanceMultiplier = config.get("debug", "cableResistanceMultiplier", 1000000000).getDouble();

        Eln.energyMeterWebhookFrequency = config.get("network", "meterWebhookFrequency", 0, "Frequency (in seconds) to send webhooks with energy usage. Default of 0 disables webhooks. You are encouraged to set this to either 60 or 500.").getInt();

        {
            // typstr gets the most current list of values that you can use
            String typstr = "";
            for (DebugType dt: DebugType.values()) {
                typstr += dt.name() + ", ";
            }
            typstr = typstr.substring(0, typstr.length() - 2);

            // if not created, it will create the value with everything enabled, and also dynamically edit the comment to list all possible types you can use
            String dst = config.get("debug", "enabledTypes", typstr, "One/multiple of: " + typstr).getString();

            // this parses all of the ones the user has selected and adds them to the debugTypes list.
            for (String str: dst.split(",")) {
                str = str.trim();
                //Eln.logger.info("Enabling debug prints for " + str);
                try {
                    Eln.debugTypes.add(DebugType.valueOf(str));
                } catch (Exception e) {
                    Eln.logger.error("Error loading config with DebugType: " + e);
                }
            }
        }

        config.save();
    }
}

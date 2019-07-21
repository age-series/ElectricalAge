package mods.eln.registry;

import mods.eln.Eln;
import mods.eln.ghost.GhostGroup;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.gridnode.transformer.GridTransformerDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.mechanical.*;
import mods.eln.misc.Coordonate;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.Utils;
import mods.eln.misc.series.SerieEE;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.sound.SoundCommand;
import mods.eln.transparentnode.FuelGeneratorDescriptor;
import mods.eln.transparentnode.FuelHeatFurnaceDescriptor;
import mods.eln.transparentnode.ResistorSinkDescriptor;
import mods.eln.transparentnode.autominer.AutoMinerDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor;
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.*;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor;
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor;
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor;
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterDescriptor;
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.transparentnode.transformer.TransformerDescriptor;
import mods.eln.transparentnode.turbine.TurbineDescriptor;
import mods.eln.transparentnode.turret.TurretDescriptor;
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor;
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.HashSet;

import static mods.eln.i18n.I18N.TR_NAME;

public class TransparentNodeRegistry {

    private static void addRecipe(ItemStack output, Object... params) {
        RegistryUtils.addRecipe(output, params);
    }
    private static void addShapelessRecipe(ItemStack output, Object... params) {
        RegistryUtils.addShapelessRecipe(output, params);
    }
    private static ItemStack findItemStack(String name, int stackSize) {
        return RegistryUtils.findItemStack(name, stackSize);
    }
    private static String firstExistingOre(String... oreNames) {
        return RegistryUtils.firstExistingOre(oreNames);
    }
    private static ItemStack findItemStack(String name) {
        return RegistryUtils.findItemStack(name);
    }

    public static void thingRegistration() {
        //TRANSPARENT NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerPowerComponent(1);
        registerTransformer(2);
        registerHeatFurnace(3);
        registerTurbine(4);
        registerElectricalAntenna(7);
        registerBattery(16);
        registerElectricalFurnace(32);
        registerMacerator(33);
        registerArcFurnace(34);
        registerCompressor(35);
        registerMagnetizer(36);
        registerPlateMachine(37);
        registerEggIncubator(41);
        registerAutoMiner(42);
        registerSolarPanel(48);
        registerWindTurbine(49);
        registerThermalDissipatorPassiveAndActive(64);
        registerTransparentNodeMisc(65);
        registerTurret(66);
        registerFuelGenerator(67);
        registerGridDevices(123);
    }

    public static void recipeRegistration() {

        HashSet<String> oreNames = new HashSet<String>();
        {
            final String[] names = OreDictionary.getOreNames();
            Collections.addAll(oreNames, names);
        }

        recipeTransformer();
        recipeHeatFurnace();
        recipeTurbine();
        recipeElectricalFurnace();
        recipeMachine();
        recipeEggIncubator();
        recipeSolarPanel();
        recipeBattery();
        recipeGridDevices(oreNames);
        recipeWindTurbine();
        recipeFuelGenerator();
        recipeThermalDissipatorPassiveAndActive();
        recipeTransporter();
        recipeTurret();
        recipeElectricalAntenna();
        recipeAutoMiner();
        recipeDisplays();
    }

    private static void registerTransformer(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "DC-DC Converter");
            TransformerDescriptor desc = new TransformerDescriptor(name, Eln.obj.getObj("transformator"),
                Eln.obj.getObj("feromagneticcorea"), Eln.obj.getObj("transformatorCase"), 0.5f);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeTransformer() {
        addRecipe(findItemStack("DC-DC Converter"),
            "C C",
            "III",
            'C', findItemStack("Copper Cable"),
            'I', new ItemStack(Items.iron_ingot));
    }

    private static void registerHeatFurnace(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Stone Heat Furnace");
            HeatFurnaceDescriptor desc = new HeatFurnaceDescriptor(name,
                "stonefurnace",
                4000,
                Utils.getCoalEnergyReference() * 2 / 3,
                8,
                500,
                new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2) // thermal
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Fuel Heat Furnace");
            FuelHeatFurnaceDescriptor desc = new FuelHeatFurnaceDescriptor(name,
                Eln.obj.getObj("FuelHeater"), new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2));
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }
    private static void recipeHeatFurnace() {
        addRecipe(findItemStack("Stone Heat Furnace"),
            "BBB",
            "BIB",
            "BiB",
            'B', new ItemStack(Blocks.stone),
            'i', findItemStack("Copper Thermal Cable"),
            'I', findItemStack("Combustion Chamber"));
        addRecipe(findItemStack("Fuel Heat Furnace"),
            "IcI",
            "mCI",
            "IiI",
            'c', findItemStack("Cheap Chip"),
            'm', findItemStack("Electrical Motor"),
            'C', new ItemStack(Items.cauldron),
            'I', new ItemStack(Items.iron_ingot),
            'i', findItemStack("Copper Thermal Cable"));
    }

    private static void registerTurbine(int id) {
        int subId;
        String name;
        FunctionTable TtoU = new FunctionTable(new double[]{0, 0.1, 0.85,
            1.0, 1.1, 1.15, 1.18, 1.19, 1.25}, 8.0 / 5.0);
        FunctionTable PoutToPin = new FunctionTable(new double[]{0.0, 0.2,
            0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7}, 8.0 / 5.0);
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "50V Turbine");
            double RsFactor = 0.1;
            double nominalU = Eln.LVU;
            double nominalP = 1000 * Eln.heatTurbinePowerFactor;
            double nominalDeltaT = 250;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbineb", Eln.lowVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, Eln.lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v");
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "200V Turbine");
            double RsFactor = 0.10;
            double nominalU = Eln.MVU;
            double nominalP = 2000 * Eln.heatTurbinePowerFactor;
            double nominalDeltaT = 350;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbinebblue", Eln.meduimVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, Eln.meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v");
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 9;
            SteamTurbineDescriptor desc = new SteamTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Steam Turbine"),
                Eln.obj.getObj("Turbine")
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 11;
            GasTurbineDescriptor desc = new GasTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Gas Turbine"),
                Eln.obj.getObj("GasTurbine")
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);

        }
        {
            subId = 12;

            StraightJointDescriptor desc = new StraightJointDescriptor(
                TR_NAME(I18N.Type.NONE, "Joint"),
                Eln.obj.getObj("StraightJoint"));
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 13;

            VerticalHubDescriptor desc = new VerticalHubDescriptor(
                TR_NAME(I18N.Type.NONE, "Joint hub"),
                Eln.obj.getObj("VerticalHub"));
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 14;

            FlywheelDescriptor desc = new FlywheelDescriptor(
                TR_NAME(I18N.Type.NONE, "Flywheel"),
                Eln.obj.getObj("Flywheel"));
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 15;

            TachometerDescriptor desc = new TachometerDescriptor(
                TR_NAME(I18N.Type.NONE, "Tachometer"),
                Eln.obj.getObj("Tachometer"));
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        // New Shaft motor and motor code
        {
            subId = 10;

            GenMotorDescriptor desc = new GenMotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Generator"),
                GMType.GENERATOR,
                Eln.obj.getObj("Generator"),
                Eln.veryHighCurrentCableDescriptor,
                250.0,
                3200.0,
                16000.0,
                0.95,
                Eln.sixNodeThermalLoadInitializer.copy()
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;

            GenMotorDescriptor desc = new GenMotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Shaft Motor"),
                GMType.MOTOR,
                Eln.obj.getObj("Motor"),
                Eln.veryHighCurrentCableDescriptor,
                250.0,
                3200.0,
                16000.0,
                0.99,
                Eln.sixNodeThermalLoadInitializer.copy()
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }


        /* old shaft and motor code
        {
            subId = 10;
            float nominalRads = 800, nominalU = 3200;
            float nominalP = 4000;
            GeneratorDescriptor desc = new GeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "Generator"),
                Eln.obj.getObj("Generator"),
                Eln.veryHighCurrentCableDescriptor,
                nominalRads, nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                Eln.sixNodeThermalLoadInitializer.copy()
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;

            float nominalRads = 800, nominalU = 3200;
            float nominalP = 1200;

            MotorDescriptor desc = new MotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Shaft Motor"),
                Eln.obj.getObj("Motor"),
                Eln.veryHighCurrentCableDescriptor,
                nominalRads,
                nominalU,
                nominalP,
                25.0f * nominalP / nominalU,
                25.0f * nominalP / nominalU,
                Eln.sixNodeThermalLoadInitializer.copy()
            );

            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
         */
        {
            subId = 17;
            ClutchDescriptor desc = new ClutchDescriptor(
                TR_NAME(I18N.Type.NONE, "Clutch"),
                Eln.obj.getObj("Clutch")
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 18;
            FixedShaftDescriptor desc = new FixedShaftDescriptor(
                TR_NAME(I18N.Type.NONE, "Fixed Shaft"),
                Eln.obj.getObj("FixedShaft")
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeTurbine() {
        addRecipe(findItemStack("50V Turbine"),
            " m ",
            "HMH",
            " E ",
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Low Voltage Cable"),
            'H', findItemStack("Copper Thermal Cable"),
            'm', findItemStack("Electrical Motor")
        );
        addRecipe(findItemStack("200V Turbine"),
            "ImI",
            "HMH",
            "IEI",
            'I', "itemRubber",
            'M', findItemStack("Advanced Machine Block"),
            'E', findItemStack("Medium Voltage Cable"),
            'H', findItemStack("Copper Thermal Cable"),
            'm', findItemStack("Advanced Electrical Motor"));
        addRecipe(findItemStack("Generator"),
            "mmm",
            "ama",
            " ME",
            'm', findItemStack("Advanced Electrical Motor"),
            'M', findItemStack("Advanced Machine Block"),
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'E', findItemStack("High Voltage Cable")
        );
        addRecipe(findItemStack("Shaft Motor"),
            "imi",
            " ME",
            'i', "ingotIron",
            'M', findItemStack("Advanced Machine Block"),
            'm', findItemStack("Advanced Electrical Motor"),
            'E', findItemStack("Very High Voltage Cable")
        );
        addRecipe(findItemStack("Steam Turbine"),
            " a ",
            "aAa",
            " M ",
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'A', firstExistingOre("blockAluminum", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Gas Turbine"),
            "msH",
            "sSs",
            " M ",
            'm', findItemStack("Advanced Electrical Motor"),
            'H', findItemStack("Copper Thermal Cable"),
            's', firstExistingOre("ingotSteel", "ingotIron"),
            'S', firstExistingOre("blockSteel", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Joint"),
            "   ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );
        addRecipe(findItemStack("Joint hub"),
            " i ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );
        addRecipe(findItemStack("Flywheel"),
            "PPP",
            "PmP",
            "PPP",
            'P', "ingotLead",
            'm', findItemStack("Machine Block")
        );
        addRecipe(findItemStack("Tachometer"),
            "p  ",
            "iii",
            "cm ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block"),
            'p', findItemStack("Electrical Probe Chip"),
            'c', findItemStack("Signal Cable")
        );
        addRecipe(findItemStack("Clutch"),
            "iIi",
            " c ",
            'i', "ingotIron",
            'I', "plateIron",
            'c', findItemStack("Machine Block")
        );
        addRecipe(findItemStack("Fixed Shaft"),
            "iBi",
            " c ",
            'i', "ingotIron",
            'B', "blockIron",
            'c', findItemStack("Machine Block")
        );
    }

    private static void registerElectricalFurnace(int id) {
        int subId;
        String name;
        Eln.furnaceList.add(new ItemStack(Blocks.furnace));
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Furnace");
            double[] PfTTable = new double[]{0, 20, 40, 80, 160, 240, 360,
                540, 756, 1058.4, 1481.76};
            double[] thermalPlostfTTable = new double[PfTTable.length];
            for (int idx = 0; idx < thermalPlostfTTable.length; idx++) {
                thermalPlostfTTable[idx] = PfTTable[idx]
                    * Math.pow((idx + 1.0) / thermalPlostfTTable.length, 2)
                    * 2;
            }
            FunctionTableYProtect PfT = new FunctionTableYProtect(PfTTable,
                800.0, 0, 100000.0);
            FunctionTableYProtect thermalPlostfT = new FunctionTableYProtect(
                thermalPlostfTTable, 800.0, 0.001, 10000000.0);
            ElectricalFurnaceDescriptor desc = new ElectricalFurnaceDescriptor(
                name, PfT, thermalPlostfT,
                40
            );
            Eln.electricalFurnace = desc;
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            Eln.furnaceList.add(desc.newItemStack());
        }
    }
    private static void recipeElectricalFurnace() {
        addRecipe(findItemStack("Electrical Furnace"),
            "III",
            "IFI",
            "ICI",
            'C', findItemStack("Low Voltage Cable"),
            'F', new ItemStack(Blocks.furnace),
            'I', new ItemStack(Items.iron_ingot));
        addShapelessRecipe(findItemStack("Canister of Water", 1),
            findItemStack("Inert Canister"),
            new ItemStack(Items.water_bucket));
    }

    private static void registerMacerator(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Macerator");
            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratora", Eln.LVU, 200,
                Eln.LVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.lowVoltageCableDescriptor,
                Eln.maceratorRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Macerator");
            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratorb", Eln.MVU, 400,
                Eln.MVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.meduimVoltageCableDescriptor,
                Eln.maceratorRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }
    }
    private static void registerCompressor(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Compressor");
            CompressorDescriptor desc = new CompressorDescriptor(
                name,
                Eln.obj.getObj("compressora"),
                Eln.LVU, 200,
                Eln.LVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.lowVoltageCableDescriptor,
                Eln.compressorRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Compressor");
            CompressorDescriptor desc = new CompressorDescriptor(
                name,
                Eln.obj.getObj("compressorb"),
                Eln.MVU, 400,
                Eln.MVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.meduimVoltageCableDescriptor,
                Eln.compressorRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }
    }
    private static void registerPlateMachine(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Plate Machine");
            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,
                Eln.obj.getObj("platemachinea"),
                Eln.LVU, 200,
                Eln.LVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.lowVoltageCableDescriptor,
                Eln.plateMachineRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Plate Machine");
            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,
                Eln.obj.getObj("platemachineb"),
                Eln.MVU, 400,
                Eln.MVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.meduimVoltageCableDescriptor,
                Eln.plateMachineRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");
        }
    }
    private static void registerMagnetizer(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Magnetizer");
            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,
                Eln.obj.getObj("magnetizera"),
                Eln.LVU, 200,
                Eln.LVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.lowVoltageCableDescriptor,
                Eln.magnetiserRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:Motor");
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Magnetizer");
            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,
                Eln.obj.getObj("magnetizerb"),
                Eln.MVU, 400,
                Eln.MVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.meduimVoltageCableDescriptor,
                Eln.magnetiserRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:Motor");
        }
    }
    private static void registerArcFurnace(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "800V Arc Furnace");
            ArcFurnaceDescriptor desc = new ArcFurnaceDescriptor(
                name,
                Eln.obj.getObj("arcfurnace"),
                Eln.HVU, 10000,
                Eln.HVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),
                Eln.highVoltageCableDescriptor,
                Eln.arcFurnaceRecipes);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:arc_furnace");

        }
    }
    private static void recipeMachine() {
        addRecipe(findItemStack("50V Macerator", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', new ItemStack(Items.flint),
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("200V Macerator", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', Eln.dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', new ItemStack(Items.diamond),
            'I', "ingotAlloy");
        addRecipe(findItemStack("50V Compressor", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', "plateIron",
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("200V Compressor", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', Eln.dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");
        addRecipe(findItemStack("50V Plate Machine", 1),
            "IRI",
            "IMI",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("200V Plate Machine", 1),
            "DCD",
            "DMD",
            "DcD",
            'M', findItemStack("Advanced Machine Block"),
            'C', Eln.dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");
        addRecipe(findItemStack("50V Magnetizer", 1),
            "IRI",
            "cMc",
            "III",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("200V Magnetizer", 1),
            "ICI",
            "cMc",
            "III",
            'M', findItemStack("Advanced Machine Block"),
            'C', Eln.dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'I', "ingotAlloy");
        addRecipe(findItemStack("800V Arc Furnace", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', findItemStack("3x Graphite Rods"),
            'c', findItemStack("Synthetic Diamond"),
            'D', "plateGold",
            'I', "ingotAlloy");
    }

    private static void registerEggIncubator(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Egg Incubator");
            EggIncubatorDescriptor desc = new EggIncubatorDescriptor(
                name, Eln.obj.getObj("eggincubator"),
                Eln.lowVoltageCableDescriptor,
                Eln.LVU, 50);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeEggIncubator() {
        addRecipe(findItemStack("50V Egg Incubator", 1),
            "IGG",
            "E G",
            "CII",
            'C', Eln.dictCheapChip,
            'E', findItemStack("Small 50V Tungsten Heating Corp"),
            'I', new ItemStack(Items.iron_ingot),
            'G', new ItemStack(Blocks.glass_pane));
    }

    private static void registerSolarPanel(int id) {
        int subId;
        GhostGroup ghostGroup;
        String name;
        double LVSolarU = 59;
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Small Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                Eln.obj.getObj("smallsolarpannel"), null,
                ghostGroup, 0, 1, 0,
                null, LVSolarU / 4, 65.0 * Eln.solarPanelPowerFactor,
                0.01,
                Math.PI / 2, Math.PI / 2
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Small Rotating Solar Panel");
            ghostGroup = new GhostGroup();
            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                Eln.obj.getObj("smallsolarpannelrot"), Eln.lowVoltageCableDescriptor.render,
                ghostGroup, 0, 1, 0,
                null, LVSolarU / 4, Eln.solarPanelBasePower * Eln.solarPanelPowerFactor,
                0.01,
                Math.PI / 4, Math.PI / 4 * 3
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "2x3 Solar Panel");
            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);
            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);
            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                Eln.obj.getObj("bigSolarPanel"), Eln.meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 0,
                groundCoordinate,
                LVSolarU * 2, Eln.solarPanelBasePower * Eln.solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 2, Math.PI / 2
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "2x3 Rotating Solar Panel");
            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);
            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                Eln.obj.getObj("bigSolarPanelrot"), Eln.meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 1,
                groundCoordinate,
                LVSolarU * 2, Eln.solarPanelBasePower * Eln.solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 8 * 3, Math.PI / 8 * 5
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeSolarPanel() {
        addRecipe(findItemStack("Small Solar Panel"),
            "LLL",
            "CSC",
            "III",
            'S', "plateSilicon",
            'L', findItemStack("Lapis Dust"),
            'I', new ItemStack(Items.iron_ingot),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("Small Rotating Solar Panel"),
            "ISI",
            "I I",
            'S', findItemStack("Small Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.iron_ingot));
        for (String metal : new String[]{"blockSteel", "blockAluminum", "blockAluminium", "casingMachineAdvanced"}) {
            for (String panel : new String[]{"Small Solar Panel", "Small Rotating Solar Panel"}) {
                addRecipe(findItemStack("2x3 Solar Panel"),
                    "PPP",
                    "PPP",
                    "I I",
                    'P', findItemStack(panel),
                    'I', metal);
            }
        }
        addRecipe(findItemStack("2x3 Rotating Solar Panel"),
            "ISI",
            "IMI",
            "I I",
            'S', findItemStack("2x3 Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.iron_ingot));
    }

    private static void registerBattery(int id) {
        int subId;
        String name;
        double[] voltageFunctionTable = {0.000, 0.9, 1.0, 1.025, 1.04, 1.05,
            2.0};
        FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,
            6.0 / 5);

        Utils.printFunction(voltageFunction, -0.2, 1.2, 0.1);

        double stdDischargeTime = 60 * 16;
        double stdU = Eln.LVU;
        double stdP = Eln.LVP() / 4;
        double stdEfficiency = 1.0 - 2.0 / 50.0;

        Eln.batteryVoltageFunctionTable = voltageFunction;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Cost Oriented Battery");
            BatteryDescriptor desc = new BatteryDescriptor(
                name,
                "BatteryBig",
                Eln.batteryCableDescriptor,
                true,
                voltageFunction,
                stdU,
                stdP * 1.2,
                0.00,
                stdP,
                stdDischargeTime * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife,
                "Cheap battery"
            );
            desc.setRenderSpec("lowcost");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Capacity Oriented Battery");
            BatteryDescriptor desc = new BatteryDescriptor(
                name,
                "BatteryBig",
                Eln.batteryCableDescriptor,
                true,
                voltageFunction,
                stdU / 4,
                stdP / 2 * 1.2,
                0.000,
                stdP / 2,
                stdDischargeTime * 8 * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife,
                "the battery"
            );
            desc.setRenderSpec("capacity");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Voltage Oriented Battery");
            BatteryDescriptor desc = new BatteryDescriptor(
                name,
                "BatteryBig",
                Eln.meduimVoltageCableDescriptor,
                true,
                voltageFunction,
                stdU * 4,
                stdP * 1.2,
                0.000,
                stdP,
                stdDischargeTime * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife,
                "the battery"
            );
            desc.setRenderSpec("highvoltage");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Current Oriented Battery");
            BatteryDescriptor desc = new BatteryDescriptor(
                name,
                "BatteryBig",
                Eln.batteryCableDescriptor,
                true,
                voltageFunction,
                stdU,
                stdP * 1.2 * 4,
                0.000,
                stdP * 4,
                stdDischargeTime / 6 * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife,
                "the battery"
            );
            desc.setRenderSpec("current");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Life Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(
                name,
                "BatteryBig",
                Eln.batteryCableDescriptor,
                false,
                voltageFunction,
                stdU,
                stdP * 1.2,
                0.000,
                stdP,
                stdDischargeTime * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife * 8,
                "the battery"
            );
            desc.setRenderSpec("life");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Single-use Battery");
            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig",
                Eln.batteryCableDescriptor,
                false,
                voltageFunction,
                stdU,
                stdP * 1.2 * 2,
                0.000,
                stdP * 2,
                stdDischargeTime / 4 * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife * 8,
                "the battery"
            );
            desc.setStartCharge(1.0);
            desc.setRechargable(false);
            desc.setRenderSpec("coal");
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Experimental Battery");

            BatteryDescriptor desc = new BatteryDescriptor(
                name,
                "BatteryBig",
                Eln.batteryCableDescriptor,
                false,
                voltageFunction,
                stdU * 2,
                stdP * 1.2 * 8,
                0.025,
                stdP * 8,
                stdDischargeTime / 4 * Eln.batteryCapacityFactor,
                stdEfficiency,
                Eln.stdBatteryHalfLife * 8,
                "You were unable to fix the power leaking problem, though." // name, description)
            );
            desc.setRenderSpec("highvoltage");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeBattery() {
        addRecipe(findItemStack("Cost Oriented Battery"),
            "C C",
            "PPP",
            "PPP",
            'C', findItemStack("Low Voltage Cable"),
            'P', "ingotLead",
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Capacity Oriented Battery"),
            "PBP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotLead");
        addRecipe(findItemStack("Voltage Oriented Battery"),
            "PBP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', findItemStack("Iron Cable"));

        addRecipe(findItemStack("Current Oriented Battery"),
            "PBP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotCopper");
        addRecipe(findItemStack("Life Oriented Battery"),
            "PBP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', new ItemStack(Items.gold_ingot));
        addRecipe(findItemStack("Experimental Battery"),
            " S ",
            "LDV",
            " C ",
            'S', findItemStack("Capacity Oriented Battery"),
            'L', findItemStack("Life Oriented Battery"),
            'V', findItemStack("Voltage Oriented Battery"),
            'C', findItemStack("Current Oriented Battery"),
            'D', new ItemStack(Items.diamond));
        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Low Voltage Cable"),
            'p', new ItemStack(Items.coal, 1, 0),
            'I', "ingotCopper");
        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Low Voltage Cable"),
            'p', new ItemStack(Items.coal, 1, 1),
            'I', "ingotCopper");
    }

    private static void registerPowerComponent(int id) {
        int subId;
        String name;
        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Power inductor");
            PowerInductorDescriptor desc = new PowerInductorDescriptor(
                name, null, SerieEE.newE12(-1)
            );
            Eln.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
        {
            subId = 20;
            name = TR_NAME(I18N.Type.NONE, "Power capacitor");
            PowerCapacitorDescriptor desc = new PowerCapacitorDescriptor(
                name, null, SerieEE.newE6(-2), 300
            );
            Eln.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }
    // there are no crafting recipies as this is a removed item.

    private static void registerGridDevices(int id) {
        int subId;
        {
            subId = 3;
            GridTransformerDescriptor descriptor =
                new GridTransformerDescriptor("Grid DC-DC Converter", Eln.obj.getObj("GridConverter"), "textures/wire.png", Eln.highVoltageCableDescriptor);
            GhostGroup g = new GhostGroup();
            g.addElement(1, 0, 0);
            g.addElement(0, 0, -1);
            g.addElement(1, 0, -1);
            g.addElement(1, 1, 0);
            g.addElement(0, 1, 0);
            g.addElement(1, 1, -1);
            g.addElement(0, 1, -1);
            descriptor.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 4;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor(
                    "Utility Pole",
                    Eln.obj.getObj("UtilityPole"),
                    "textures/wire.png",
                    Eln.highVoltageCableDescriptor,
                    false,
                    24,
                    12800);
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, 0);
            g.addElement(0, 3, 0);
            //g.addRectangle(-1, 1, 3, 4, -1, 1);
            descriptor.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 5;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor(
                    "Utility Pole w/DC-DC Converter",
                    Eln.obj.getObj("UtilityPole"),
                    "textures/wire.png",
                    Eln.highVoltageCableDescriptor,
                    true,
                    24,
                    12800);
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, 0);
            g.addElement(0, 3, 0);
            //g.addRectangle(-1, 1, 3, 4, -1, 1);
            descriptor.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 6;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor("Transmission Tower",
                    Eln.obj.getObj("TransmissionTower"),
                    "textures/wire.png",
                    Eln.highVoltageCableDescriptor,
                    false,
                    96,
                    51200);
            GhostGroup g = new GhostGroup();
            g.addRectangle(-1, 1, 0, 0, -1, 1);
            g.addRectangle(0, 0, 1, 8, 0, 0);
            g.removeElement(0, 0, 0);
            descriptor.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            // subId = 7;
            // Reserved for T2.5 poles.
        }
    }
    private static void recipeGridDevices(HashSet<String> oreNames) {
        int poleRecipes = 0;
        for (String oreName : new String[]{
            "ingotAluminum",
            "ingotAluminium",
            "ingotSteel",
        }) {
            if (oreNames.contains(oreName)) {
                addRecipe(findItemStack("Utility Pole"),
                    "WWW",
                    "IWI",
                    " W ",
                    'W', "logWood",
                    'I', oreName
                );
                poleRecipes++;
            }
        }
        if (poleRecipes == 0) {
            // Really?
            addRecipe(findItemStack("Utility Pole"),
                "WWW",
                "IWI",
                " W ",
                'I', "ingotIron",
                'W', "logWood"
            );
        }
        addRecipe(findItemStack("Utility Pole w/DC-DC Converter"),
            "HHH",
            " TC",
            " PH",
            'P', findItemStack("Utility Pole"),
            'H', findItemStack("High Voltage Cable"),
            'C', findItemStack("Optimal Ferromagnetic Core"),
            'T', findItemStack("DC-DC Converter")
        );

        // I don't care what you think, if your modpack lacks steel then you don't *need* this much power.
        // Or just use the new Arc furnace. Other mod's steel methods are slow and tedious and require huge multiblocks.
        // Feel free to add alternate non-iron recipes, though. Here, or by minetweaker.
        for (String type : new String[]{
            "Aluminum",
            "Aluminium",
            "Steel"
        }) {
            String blockType = "block" + type;
            String ingotType = "ingot" + type;
            if (oreNames.contains(blockType)) {
                addRecipe(findItemStack("Transmission Tower"),
                    "ii ",
                    "mi ",
                    " B ",
                    'i', ingotType,
                    'B', blockType,
                    'm', findItemStack("Machine Block"));
                addRecipe(findItemStack("Grid DC-DC Converter"),
                    "i i",
                    "mtm",
                    "imi",
                    'i', ingotType,
                    't', findItemStack("DC-DC Converter"),
                    'm', findItemStack("Advanced Machine Block"));
            }
        }
    }

    private static void registerWindTurbine(int id) {
        int subId;
        String name;
        FunctionTable PfW = new FunctionTable(
            new double[]{0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2},
            8.0 / 5.0);
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Wind Turbine");
            WindTurbineDescriptor desc = new WindTurbineDescriptor(
                name, Eln.obj.getObj("WindTurbineMini"),
                Eln.lowVoltageCableDescriptor,

                PfW,
                160 * Eln.windTurbinePowerFactor, 10,
                Eln.LVU * 1.18, 22,
                3,
                7, 2, 2,
                2, 0.07,
                "eln:WINDTURBINE_BIG_SF", 1f // Use the wind turbine sound and play at normal volume (1 => 100%)
            );
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, -1);
            g.addElement(0, 2, 1);
            g.addElement(0, 3, -1);
            g.addElement(0, 3, 1);
            g.addRectangle(0, 0, 1, 3, 0, 0);
            desc.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        /*{ //TODO Work on the large wind turbine
            subId = 1;
            name = TR_NAME(DPType.NONE, "Large Wind Turbine");

            WindTurbineDescriptor desc = new WindTurbineDescriptor(
                name, obj.getObj("WindTurbineMini"), // name,Obj3D obj,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                PfW,// PfW
                160 * windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
                LVU * 1.18, 22,// double maxVoltage, double maxWind,
                3,// int offY,
                7, 2, 2,// int rayX,int rayY,int rayZ,
                2, 0.07,// int blockMalusMinCount,double blockMalus
                "eln:WINDTURBINE_BIG_SF", 1f // Use the wind turbine sound and play at normal volume (1 => 100%)
            );

            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, -1);
            g.addElement(0, 2, 1);
            g.addElement(0, 3, -1);
            g.addElement(0, 3, 1);
            g.addRectangle(0, 0, 1, 3, 0, 0);
            desc.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        } */

        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Water Turbine");
            Coordonate waterCoord = new Coordonate(1, -1, 0, 0);
            WaterTurbineDescriptor desc = new WaterTurbineDescriptor(
                name, Eln.obj.getObj("SmallWaterWheel"),
                Eln.lowVoltageCableDescriptor,
                30 * Eln.waterTurbinePowerFactor,
                Eln.LVU * 1.18,
                waterCoord,
                "eln:water_turbine", 1f
            );
            GhostGroup g = new GhostGroup();
            g.addRectangle(1, 1, 0, 1, -1, 1);
            desc.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }
    private static void recipeWindTurbine() {
        addRecipe(findItemStack("Wind Turbine"),
            " I ",
            "IMI",
            " B ",
            'B', findItemStack("Machine Block"),
            'I', "plateIron",
            'M', findItemStack("Electrical Motor"));
        /*addRecipe(findItemStack("Large Wind Turbine"), //todo add recipe to large wind turbine
            "TTT",
            "TCT",
            "TTT",
            'T', findItemStack("Wind Turbine"),
            'C', findItemStack("Advanced Machine Block")); */
        addRecipe(findItemStack("Water Turbine"),
            "  I",
            "BMI",
            "  I",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"));
    }

    private static void registerFuelGenerator(int id) {
        int subId;
        {
            subId = 1;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(I18N.Type.NONE, "50V Fuel Generator"), Eln.obj.getObj("FuelGenerator50V"),
                    Eln.lowVoltageCableDescriptor, Eln.fuelGeneratorPowerFactor * 1200, Eln.LVU * 1.25, Eln.fuelGeneratorTankCapacity);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 2;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(I18N.Type.NONE, "200V Fuel Generator"), Eln.obj.getObj("FuelGenerator200V"),
                    Eln.meduimVoltageCableDescriptor, Eln.fuelGeneratorPowerFactor * 6000, Eln.MVU * 1.25,
                    Eln.fuelGeneratorTankCapacity);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }
    private static void recipeFuelGenerator() {
        addRecipe(findItemStack("50V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Low Voltage Cable"),
            'M', findItemStack("Electrical Motor"));
        addRecipe(findItemStack("200V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Advanced Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Medium Voltage Cable"),
            'M', findItemStack("Advanced Electrical Motor"));
    }

    private static void registerThermalDissipatorPassiveAndActive(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Small Passive Thermal Dissipator");
            ThermalDissipatorPassiveDescriptor desc = new ThermalDissipatorPassiveDescriptor(
                name,
                Eln.obj.getObj("passivethermaldissipatora"),
                200, -100,
                250, 30,
                10, 1
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "Small Active Thermal Dissipator");
            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                Eln.obj.getObj("activethermaldissipatora"),
                Eln.LVU, 50,
                800,
                Eln.lowVoltageCableDescriptor,
                // cableDescriptor,
                130, -100,
                200, 30,
                10, 1
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 34;
            name = TR_NAME(I18N.Type.NONE, "200V Active Thermal Dissipator");
            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                Eln.obj.getObj("200vactivethermaldissipatora"),
                Eln.MVU, 60,
                1200,
                Eln.meduimVoltageCableDescriptor,
                130, -100,
                200, 30,
                10, 1
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 35;
            name = TR_NAME(I18N.Type.NONE, "Creative Dissipator");
            ResistorSinkDescriptor desc = new ResistorSinkDescriptor(
                name,
                Eln.veryHighCurrentCableDescriptor,
                Eln.obj.getObj("passivethermaldissipatora")
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeThermalDissipatorPassiveAndActive() {
        addRecipe(
            findItemStack("Small Passive Thermal Dissipator"),
            "I I",
            "III",
            "CIC",
            'I', "ingotCopper",
            'C', findItemStack("Copper Thermal Cable"));
        addRecipe(
            findItemStack("Small Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Electrical Motor"),
            'R', "itemRubber");
        addRecipe(
            findItemStack("200V Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber");
    }

    private static void registerTransparentNodeMisc(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Experimental Transporter");
            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-1, 0, 1, 0);
            powerLoad[1] = new Coordonate(-1, 0, -1, 0);
            GhostGroup doorOpen = new GhostGroup();
            doorOpen.addRectangle(-4, -3, 2, 2, 0, 0);
            GhostGroup doorClose = new GhostGroup();
            doorClose.addRectangle(-2, -2, 0, 1, 0, 0);
            TeleporterDescriptor desc = new TeleporterDescriptor(
                name, Eln.obj.getObj("Transporter"),
                Eln.highVoltageCableDescriptor,
                new Coordonate(-1, 0, 0, 0), new Coordonate(-1, 1, 0, 0),
                2,// int areaH
                powerLoad,
                doorOpen, doorClose
            );
            desc.setChargeSound("eln:transporter", 0.5f);
            GhostGroup g = new GhostGroup();
            g.addRectangle(-2, 0, 0, 1, -1, -1);
            g.addRectangle(-2, 0, 0, 1, 1, 1);
            g.addRectangle(-4, -1, 2, 2, 0, 0);
            g.addElement(0, 1, 0);
            g.addElement(-1, 0, 0, Eln.ghostBlock, Eln.ghostBlock.tFloor);
            g.addRectangle(-3, -3, 0, 1, -1, -1);
            g.addRectangle(-3, -3, 0, 1, 1, 1);
            desc.setGhostGroup(g);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeTransporter() {
        addRecipe(findItemStack("Experimental Transporter", 1),
            "RMR",
            "RMR",
            " R ",
            'M', findItemStack("Advanced Machine Block"),
            'C', findItemStack("High Voltage Cable"),
            'R', Eln.dictAdvancedChip);
    }

    private static void registerTurret(int id) {
        {
            int subId = 0;
            String name = TR_NAME(I18N.Type.NONE, "800V Defence Turret");
            TurretDescriptor desc = new TurretDescriptor(name, "Turret");
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeTurret() {
        addRecipe(findItemStack("800V Defence Turret", 1),
            " R ",
            "CMC",
            " c ",
            'M', findItemStack("Advanced Machine Block"),
            'C', Eln.dictAdvancedChip,
            'c', Eln.highVoltageCableDescriptor.newItemStack(),
            'R', new ItemStack(Blocks.redstone_block));
    }

    private static void registerElectricalAntenna(int id) {
        int subId;
        String name;
        {
            subId = 0;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Low Power Transmitter Antenna");
            double P = 250;
            desc = new ElectricalAntennaTxDescriptor(name,
                Eln.obj.getObj("lowpowertransmitterantenna"), 200,
                0.9, 0.7,
                Eln.LVU, P,
                Eln.LVU * 1.3, P * 1.3,
                Eln.lowVoltageCableDescriptor);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Low Power Receiver Antenna");
            double P = 250;
            desc = new ElectricalAntennaRxDescriptor(name,
                Eln.obj.getObj("lowpowerreceiverantenna"), Eln.LVU, P,
                Eln.LVU * 1.3, P * 1.3,
                Eln.lowVoltageCableDescriptor);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Medium Power Transmitter Antenna");
            double P = 1000;
            desc = new ElectricalAntennaTxDescriptor(name,
                Eln.obj.getObj("lowpowertransmitterantenna"), 250,
                0.9, 0.75,
                Eln.MVU, P,
                Eln.MVU * 1.3, P * 1.3,
                Eln.meduimVoltageCableDescriptor);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Medium Power Receiver Antenna");
            double P = 1000;
            desc = new ElectricalAntennaRxDescriptor(name,
                Eln.obj.getObj("lowpowerreceiverantenna"), Eln.MVU, P,
                Eln.MVU * 1.3, P * 1.3,
                Eln.meduimVoltageCableDescriptor);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "High Power Transmitter Antenna");
            double P = 2000;
            desc = new ElectricalAntennaTxDescriptor(name,
                Eln.obj.getObj("lowpowertransmitterantenna"), 300,
                0.95, 0.8,
                Eln.HVU, P,
                Eln.HVU * 1.3, P * 1.3,
                Eln.highVoltageCableDescriptor);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "High Power Receiver Antenna");
            double P = 2000;
            desc = new ElectricalAntennaRxDescriptor(name,
                Eln.obj.getObj("lowpowerreceiverantenna"), Eln.HVU, P,
                Eln.HVU * 1.3, P * 1.3,
                Eln.highVoltageCableDescriptor);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeElectricalAntenna() {
        addRecipe(findItemStack("Low Power Transmitter Antenna", 1),
            "R i",
            "CI ",
            "R i",
            'C', Eln.dictCheapChip,
            'i', new ItemStack(Items.iron_ingot),
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Low Power Receiver Antenna", 1),
            "i  ",
            " IC",
            "i  ",
            'C', Eln.dictCheapChip,
            'I', "plateIron",
            'i', new ItemStack(Items.iron_ingot),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Medium Power Transmitter Antenna", 1),
            "c I",
            "CI ",
            "c I",
            'C', Eln.dictAdvancedChip,
            'c', Eln.dictCheapChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Medium Power Receiver Antenna", 1),
            "I  ",
            " IC",
            "I  ",
            'C', Eln.dictAdvancedChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("High Power Transmitter Antenna", 1),
            "C I",
            "CI ",
            "C I",
            'C', Eln.dictAdvancedChip,
            'c', Eln.dictCheapChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("High Power Receiver Antenna", 1),
            "I D",
            " IC",
            "I D",
            'C', Eln.dictAdvancedChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone),
            'D', new ItemStack(Items.diamond));
    }

    private static void registerAutoMiner(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Auto Miner");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-2, -1, 1, 0);
            powerLoad[1] = new Coordonate(-2, -1, -1, 0);
            Coordonate lightCoord = new Coordonate(-3, 0, 0, 0);
            Coordonate miningCoord = new Coordonate(-1, 0, 1, 0);
            AutoMinerDescriptor desc = new AutoMinerDescriptor(name,
                Eln.obj.getObj("AutoMiner"),
                powerLoad, lightCoord, miningCoord,
                2, 1, 0,
                Eln.highVoltageCableDescriptor,
                1, 50
            );
            GhostGroup ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(-2, -1, -1, 0, -1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, 1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, -1, -1);
            ghostGroup.addElement(1, 0, 0);
            ghostGroup.addElement(0, 0, 1);
            ghostGroup.addElement(0, 1, 0);
            ghostGroup.addElement(0, 0, -1);
            ghostGroup.removeElement(-1, -1, 0);
            desc.setGhostGroup(ghostGroup);
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
    private static void recipeAutoMiner() {
        addRecipe(findItemStack("Auto Miner"),
            "MCM",
            "BOB",
            " P ",
            'C', Eln.dictAdvancedChip,
            'O', findItemStack("Ore Scanner"),
            'B', findItemStack("Advanced Machine Block"),
            'M', findItemStack("Advanced Electrical Motor"),
            'P', findItemStack("Mining Pipe"));
    }

    // TODO: split
    private static void recipeDisplays() {
        addRecipe(findItemStack("Digital Display", 1),
            "   ",
            "rrr",
            "iii",
            'r', new ItemStack(Items.redstone),
            'i', findItemStack("Iron Cable")
        );
        addRecipe(findItemStack("Nixie Tube", 1),
            " g ",
            "grg",
            "iii",
            'g', new ItemStack(Blocks.glass_pane),
            'r', new ItemStack(Items.redstone),
            'i', findItemStack("Iron Cable")
        );
    }
}

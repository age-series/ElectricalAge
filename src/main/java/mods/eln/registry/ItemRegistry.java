package mods.eln.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.genericArmorItem;
import mods.eln.i18n.I18N;
import mods.eln.i18n.I18N.Type;
import mods.eln.item.*;
import mods.eln.item.electricalitem.*;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.mechanical.ClutchPinItem;
import mods.eln.mechanical.ClutchPlateItem;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketType;
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;

import static mods.eln.i18n.I18N.TR;
import static mods.eln.i18n.I18N.TR_NAME;

public class ItemRegistry {
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
        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        // ID 0  is currently used in a horrible blunder (see registerDust())
        registerHeatingCorp(1);
        registerRegulatorItem(3);
        registerLampItem(4);
        registerProtection(5);
        registerCombustionChamber(6);
        registerFerromagneticCore(7);
        registerIngot(8);
        registerDust(9);
        registerElectricalMotor(10);
        registerSolarTracker(11);
        registerMeter(14);
        registerElectricalDrill(15);
        registerOreScanner(16);
        registerMiningPipe(17);
        registerTreeResinAndRubber(64);
        registerRawCable(65);
        registerArc(69);
        registerBrush(119);
        registerMiscItem(120);
        registerElectricalTool(121);
        registerPortableItem(122);
        registerFuelBurnerItem(124);
        registerArmor();
        registerTool();
    }

    public static void recipeRegistration() {
        recipeHeatingCorp();
        recipeRegulatorItem();
        recipeLampItem();
        recipeProtection();
        recipeCombustionChamber();
        recipeFerromagneticCore();
        recipeDust();
        recipeElectricalMotor();
        recipeArmor();
        recipeTool();
        recipeSolarTracker();
        recipeMeter();
        recipeGeneral();
        recipeElectricalDrill();
        recipeOreScanner();
        recipeMiningPipe();
        recipeRawCable();
        recipeGraphite();
        recipeElectricalTool();
        recipeBatteryItem();
        recipeMiscItem();
        recipeECoal();
        recipePortableCapacitor();
        recipeFuelBurnerItem();
        recipeFurnace();
        recipeMacerator();
        recipeArcFurnace();
        recipePlateMachine();
        recipeCompressor();
        recipeMagnetizer();
    }

    private static void registerHeatingCorp(int id) {
        int subId;
        HeatingCorpElement element;
        {
            subId = 0;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 50V Copper Heating Corp"),
                Eln.LVU, 150,
                190,
                Eln.lowVoltageCableDescriptor
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 1;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "50V Copper Heating Corp"),
                Eln.LVU, 250,
                320,
                Eln.lowVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 2;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 200V Copper Heating Corp"),
                Eln.MVU, 400,
                500,
                Eln.meduimVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 3;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "200V Copper Heating Corp"),
                Eln.MVU, 600,
                750,
                Eln.highVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 4;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 50V Iron Heating Corp"),
                Eln.LVU, 180,
                225,
                Eln.lowVoltageCableDescriptor
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 5;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "50V Iron Heating Corp"),
                Eln.LVU, 375,
                480,
                Eln.lowVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 6;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 200V Iron Heating Corp"),
                Eln.MVU, 600,
                750,
                Eln.meduimVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 7;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "200V Iron Heating Corp"),
                Eln.MVU, 900,
                1050,
                Eln.highVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 8;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 50V Tungsten Heating Corp"),
                Eln.LVU, 240,
                300,
                Eln.lowVoltageCableDescriptor
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 9;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "50V Tungsten Heating Corp"),
                Eln.LVU, 500,
                640,
                Eln.lowVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 10;
            element = new HeatingCorpElement(
                TR_NAME(I18N.Type.NONE, "Small 200V Tungsten Heating Corp"),
                Eln.MVU, 800,
                1000,
                Eln.meduimVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 11;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "200V Tungsten Heating Corp"),
                Eln.MVU, 1200,
                1500,
                Eln.highVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 12;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 800V Tungsten Heating Corp"),
                Eln.HVU, 3600,
                4800,
                Eln.veryHighVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 13;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "800V Tungsten Heating Corp"),
                Eln.HVU, 4812,
                6015,
                Eln.veryHighVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 14;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 3.2kV Tungsten Heating Corp"),
                Eln.VVU, 4000,
                6000,
                Eln.veryHighVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 15;
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "3.2kV Tungsten Heating Corp"),
                Eln.VVU, 12000,
                15000,
                Eln.veryHighVoltageCableDescriptor);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
    }
    private static void recipeHeatingCorp() {
        addRecipe(findItemStack("Small 50V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Copper Cable"));
        addRecipe(findItemStack("50V Copper Heating Corp"),
            "CC",
            'C', findItemStack("Small 50V Copper Heating Corp"));
        addRecipe(findItemStack("Small 200V Copper Heating Corp"),
            "CC",
            'C', findItemStack("50V Copper Heating Corp"));
        addRecipe(findItemStack("200V Copper Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Copper Heating Corp"));
        addRecipe(findItemStack("Small 50V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C", 'C', findItemStack("Iron Cable"));
        addRecipe(findItemStack("50V Iron Heating Corp"),
            "CC",
            'C', findItemStack("Small 50V Iron Heating Corp"));
        addRecipe(findItemStack("Small 200V Iron Heating Corp"),
            "CC",
            'C', findItemStack("50V Iron Heating Corp"));
        addRecipe(findItemStack("200V Iron Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Iron Heating Corp"));
        addRecipe(findItemStack("Small 50V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Cable"));
        addRecipe(findItemStack("50V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 50V Tungsten Heating Corp"));
        addRecipe(findItemStack("Small 200V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("50V Tungsten Heating Corp"));
        addRecipe(findItemStack("200V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 200V Tungsten Heating Corp"));
        addRecipe(findItemStack("Small 800V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("200V Tungsten Heating Corp"));
        addRecipe(findItemStack("800V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 800V Tungsten Heating Corp"));
        addRecipe(findItemStack("Small 3.2kV Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("800V Tungsten Heating Corp"));
        addRecipe(findItemStack("3.2kV Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 3.2kV Tungsten Heating Corp"));
    }

    private static void registerRegulatorItem(int id) {
        int subId;
        IRegulatorDescriptor element;
        {
            subId = 0;
            element = new RegulatorOnOffDescriptor(TR_NAME(I18N.Type.NONE, "On/OFF Regulator 1 Percent"),
                "onoffregulator", 0.01);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 1;
            element = new RegulatorOnOffDescriptor(TR_NAME(I18N.Type.NONE, "On/OFF Regulator 10 Percent"),
                "onoffregulator", 0.1);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 8;
            element = new RegulatorAnalogDescriptor(TR_NAME(I18N.Type.NONE, "Analogic Regulator"),
                "Analogicregulator");
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
    }
    private static void recipeRegulatorItem() {
        addRecipe(findItemStack("On/OFF Regulator 10 Percent", 1),
            "R R",
            " R ",
            " I ",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("On/OFF Regulator 1 Percent", 1),
            "RRR",
            " I ",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Analogic Regulator", 1),
            "R R",
            " C ",
            " I ",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'C', Eln.dictCheapChip);
    }

    private static void registerLampItem(int id) {
        int subId;
        double[] lightPower = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            15, 20, 25, 30, 40};
        double[] lightLevel = new double[16];
        double economicPowerFactor = 0.5;
        double standardGrowRate = 0.0;
        for (int idx = 0; idx < 16; idx++) {
            lightLevel[idx] = (idx + 0.49) / 15.0;
        }
        LampDescriptor element;
        {
            subId = 0;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "Small 50V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.LVU, lightPower[12],
                lightLevel[12], Eln.incandescentLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 1;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.LVU, lightPower[14],
                lightLevel[14], Eln.incandescentLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 2;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.MVU, lightPower[14],
                lightLevel[14], Eln.incandescentLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 4;
            element = new LampDescriptor(
                TR_NAME(I18N.Type.NONE, "Small 50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.LVU, lightPower[11],
                lightLevel[11], Eln.carbonLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 5;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.LVU, lightPower[13],
                lightLevel[13], Eln.carbonLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 16;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "Small 50V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, Eln.LVU, lightPower[12]
                * economicPowerFactor,
                lightLevel[12], Eln.economicLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 17;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, Eln.LVU, lightPower[14]
                * economicPowerFactor,
                lightLevel[14], Eln.economicLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 18;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, Eln.MVU, lightPower[14]
                * economicPowerFactor,
                lightLevel[14], Eln.economicLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 32;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Farming Lamp"),
                "farminglamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.LVU, 120,
                lightLevel[15], Eln.incandescentLampLife, 0.50
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 36;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V Farming Lamp"),
                "farminglamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Eln.MVU, 120,
                lightLevel[15], Eln.incandescentLampLife, 0.50
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 37;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V LED Bulb"),
                "ledlamp", LampDescriptor.Type.LED,
                LampSocketType.Douille, Eln.LVU, lightPower[14] / 2,
                lightLevel[14], Eln.ledLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 38;
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V LED Bulb"),
                "ledlamp", LampDescriptor.Type.LED,
                LampSocketType.Douille, Eln.MVU, lightPower[14] / 2,
                lightLevel[14], Eln.ledLampLife, standardGrowRate
            );
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
    }
    private static void recipeLampItem() {
        addRecipe(
            findItemStack("Small 50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Eln.dictTungstenIngot,
            'S', findItemStack("Copper Cable"));
        addRecipe(findItemStack("50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Eln.dictTungstenIngot,
            'S', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Eln.dictTungstenIngot,
            'S', findItemStack("Medium Voltage Cable"));
        addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal),
            'S', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal, 1, 1),
            'S', findItemStack("Copper Cable"));
        addRecipe(
            findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal),
            'S', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal, 1, 1),
            'S', findItemStack("Low Voltage Cable"));
        addRecipe(
            findItemStack("Small 50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.glowstone_dust),
            'S', findItemStack("Copper Cable"));
        addRecipe(findItemStack("50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.glowstone_dust),
            'S', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.glowstone_dust),
            'S', findItemStack("Medium Voltage Cable"));
        addRecipe(findItemStack("50V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Eln.dictTungstenIngot,
            'S', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Eln.dictTungstenIngot,
            'S', findItemStack("Medium Voltage Cable"));
        addRecipe(findItemStack("50V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', new ItemStack(Blocks.glass_pane),
            'S', findItemStack("Silicon Ingot"),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', new ItemStack(Blocks.glass_pane),
            'S', findItemStack("Silicon Ingot"),
            'C', findItemStack("Medium Voltage Cable"));
    }

    private static void registerProtection(int id) {
        int subId;
        {
            OverHeatingProtectionDescriptor element;
            subId = 0;
            element = new OverHeatingProtectionDescriptor(
                TR_NAME(I18N.Type.NONE, "Overheating Protection"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            OverVoltageProtectionDescriptor element;
            subId = 1;
            element = new OverVoltageProtectionDescriptor(
                TR_NAME(I18N.Type.NONE, "Overvoltage Protection"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }

    }
    private static void recipeProtection() {
        addRecipe(findItemStack("Overvoltage Protection", 4),
            "SCD",
            'S', findItemStack("Electrical Probe Chip"),
            'C', Eln.dictCheapChip,
            'D', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Overheating Protection", 4),
            "SCD",
            'S', findItemStack("Thermal Probe Chip"),
            'C', Eln.dictCheapChip,
            'D', new ItemStack(Items.redstone));
    }

    private static void registerCombustionChamber(int id) {
        int subId;
        {
            CombustionChamber element;
            subId = 0;
            element = new CombustionChamber(TR_NAME(I18N.Type.NONE, "Combustion Chamber"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }

    }
    private static void recipeCombustionChamber() {
        addRecipe(findItemStack("Combustion Chamber"),
            " L ",
            "L L",
            " L ",
            'L', new ItemStack(Blocks.stone));
    }

    private static void registerFerromagneticCore(int id) {
        int subId;
        FerromagneticCoreDescriptor element;
        {
            subId = 0;
            element = new FerromagneticCoreDescriptor(
                TR_NAME(I18N.Type.NONE, "Cheap Ferromagnetic Core"), Eln.obj.getObj("feromagneticcorea"),
                100);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 1;
            element = new FerromagneticCoreDescriptor(
                TR_NAME(I18N.Type.NONE, "Average Ferromagnetic Core"), Eln.obj.getObj("feromagneticcorea"),
                50);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 2;
            element = new FerromagneticCoreDescriptor(
                TR_NAME(I18N.Type.NONE, "Optimal Ferromagnetic Core"), Eln.obj.getObj("feromagneticcorea"),
                1);
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
    }
    private static void recipeFerromagneticCore() {
        addRecipe(findItemStack("Cheap Ferromagnetic Core"),
            "LLL",
            "L  ",
            "LLL",
            'L', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Average Ferromagnetic Core"),
            "PCP",
            'C', findItemStack("Cheap Ferromagnetic Core"),
            'P', "plateIron");
        addRecipe(findItemStack("Optimal Ferromagnetic Core"),
            " P ",
            "PCP",
            " P ",
            'C', findItemStack("Average Ferromagnetic Core"),
            'P', "plateIron");
    }

    private static void registerDust(int id) {
        int subId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Copper Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.dustCopper = element;
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("dustCopper", element.newItemStack());
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Iron Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.dustCopper = element;
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("dustIron", element.newItemStack());
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Lapis Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.dustCopper = element;
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("dustLapis", element.newItemStack());
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Diamond Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.dustCopper = element;
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("dustDiamond", element.newItemStack());
        }
        {
            // TODO: HERE DOWN - AHMAGHAD!! Not ID!!!!
            id = 5;
            name = TR_NAME(I18N.Type.NONE, "Lead Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.sharedItem.addElement(id, element);
            RegistryUtils.addToOre("dustLead", element.newItemStack());
        }
        {
            id = 6;
            name = TR_NAME(I18N.Type.NONE, "Tungsten Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.sharedItem.addElement(id, element);
            RegistryUtils.addToOre(Eln.dictTungstenDust, element.newItemStack());
        }
        {
            id = 7;
            name = TR_NAME(I18N.Type.NONE, "Gold Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(id, element);
            RegistryUtils.addToOre("dustGold", element.newItemStack());
        }
        {
            id = 8;
            name = TR_NAME(I18N.Type.NONE, "Coal Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.sharedItem.addElement(id, element);
            RegistryUtils.addToOre("dustCoal", element.newItemStack());
        }
        {
            id = 9;
            name = TR_NAME(I18N.Type.NONE, "Alloy Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.sharedItem.addElement(id, element);
            RegistryUtils.addToOre("dustAlloy", element.newItemStack());
        }
        {
            id = 10;
            name = TR_NAME(I18N.Type.NONE, "Cinnabar Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,
                new String[]{});
            Eln.sharedItem.addElement(id, element);
            RegistryUtils.addToOre("dustCinnabar", element.newItemStack());
        }
    }
    private static void recipeDust() {
        addShapelessRecipe(findItemStack("Alloy Dust", 6),
            "dustIron",
            "dustCoal",
            Eln.dictTungstenDust,
            Eln.dictTungstenDust,
            Eln.dictTungstenDust,
            Eln.dictTungstenDust);
        addShapelessRecipe(findItemStack("Inert Canister", 1),
            findItemStack("Lapis Dust"),
            findItemStack("Lapis Dust"),
            findItemStack("Lapis Dust"),
            findItemStack("Lapis Dust"),
            findItemStack("Diamond Dust"),
            findItemStack("Lapis Dust"),
            findItemStack("Lapis Dust"),
            findItemStack("Lapis Dust"),
            findItemStack("Lapis Dust"));
    }

    // todo
    private static void registerIngot(int id) {
        int subId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Copper Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.copperIngot = element;
            RegistryUtils.addToOre("ingotCopper", element.newItemStack());
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Lead Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.plumbIngot = element;
            RegistryUtils.addToOre("ingotLead", element.newItemStack());
        }
        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Tungsten Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.tungstenIngot = element;
            RegistryUtils.addToOre(Eln.dictTungstenIngot, element.newItemStack());
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Ferrite Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{"useless", "Really useless"});
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("ingotFerrite", element.newItemStack());
        }
        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Alloy Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("ingotAlloy", element.newItemStack());
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Mercury");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{"useless", "miaou"});
            Eln.sharedItem.addElement(subId + (id << 6), element);
            RegistryUtils.addToOre("quicksilver", element.newItemStack());
        }
    }

    private static void registerElectricalMotor(int id) {
        int subId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Motor");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Advanced Electrical Motor");
            element = new GenericItemUsingDamageDescriptorWithComment(name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), element);
        }

    }
    private static void recipeElectricalMotor() {
        addRecipe(findItemStack("Electrical Motor"),
            " C ",
            "III",
            "C C",
            'I', findItemStack("Iron Cable"),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("Advanced Electrical Motor"),
            "RCR",
            "MIM",
            "CRC",
            'M', findItemStack("Advanced Magnet"),
            'I', new ItemStack(Items.iron_ingot),
            'R', new ItemStack(Items.redstone),
            'C', findItemStack("Medium Voltage Cable"));
    }

    private static void registerArmor() {
        String name;
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Helmet");
            Eln.helmetCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Helmet, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_helmet").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.helmetCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.helmetCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Chestplate");
            Eln.plateCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Chestplate, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_chestplate").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.plateCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.plateCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Leggings");
            Eln.legsCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Leggings, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_leggings").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.legsCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.legsCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Boots");
            Eln.bootsCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Boots, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_boots").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.bootsCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.bootsCopper));
        }
        String t1, t2;
        t1 = "eln:textures/armor/ecoal_layer_1.png";
        t2 = "eln:textures/armor/ecoal_layer_2.png";
        double energyPerDamage = 500;
        int armor;
        ItemArmor.ArmorMaterial eCoalMaterial = net.minecraftforge.common.util.EnumHelper.addArmorMaterial("ECoal", 10, new int[]{3, 8, 6, 3}, 9);
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Helmet");
            armor = 3;
            Eln.helmetECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Helmet, t1, t2,
                8000, 2000.0,
                armor / 20.0, armor * energyPerDamage,
                energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_helmet").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.helmetECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.helmetECoal));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Chestplate");
            armor = 8;
            Eln.plateECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Chestplate, t1, t2,
                8000, 2000.0,
                armor / 20.0, armor * energyPerDamage,
                energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_chestplate").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.plateECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.plateECoal));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Leggings");
            armor = 6;
            Eln.legsECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Leggings, t1, t2,

                8000, 2000.0,
                armor / 20.0, armor * energyPerDamage,
                energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_leggings").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.legsECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.legsECoal));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Boots");
            armor = 3;
            Eln.bootsECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Boots, t1, t2,
                8000, 2000.0,
                armor / 20.0, armor * energyPerDamage,
                energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_boots").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.bootsECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.bootsECoal));
        }
    }
    private static void recipeArmor() {
        addRecipe(new ItemStack(Eln.helmetCopper),
            "CCC",
            "C C",
            'C', "ingotCopper");
        addRecipe(new ItemStack(Eln.plateCopper),
            "C C",
            "CCC",
            "CCC",
            'C', "ingotCopper");
        addRecipe(new ItemStack(Eln.legsCopper),
            "CCC",
            "C C",
            "C C",
            'C', "ingotCopper");
        addRecipe(new ItemStack(Eln.bootsCopper),
            "C C",
            "C C",
            'C', "ingotCopper");
    }

    private static void registerTool() {
        String name;
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Sword");
            Eln.swordCopper = (new ItemSword(Item.ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_sword").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.swordCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.swordCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Hoe");
            Eln.hoeCopper = (new ItemHoe(Item.ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_hoe").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.hoeCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.hoeCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Shovel");
            Eln.shovelCopper = (new ItemSpade(Item.ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_shovel").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.shovelCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.shovelCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Pickaxe");
            Eln.pickaxeCopper = new ItemPickaxeEln(Item.ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_pickaxe").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.pickaxeCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.pickaxeCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Axe");
            Eln.axeCopper = new ItemAxeEln(Item.ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_axe").setCreativeTab(Eln.creativeTab);
            GameRegistry.registerItem(Eln.axeCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Eln.axeCopper));
        }
    }
    private static void recipeTool() {
        addRecipe(new ItemStack(Eln.shovelCopper),
            "i",
            "s",
            "s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Eln.axeCopper),
            "ii",
            "is",
            " s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Eln.hoeCopper),
            "ii",
            " s",
            " s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Eln.pickaxeCopper),
            "iii",
            " s ",
            " s ",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Eln.swordCopper),
            "i",
            "i",
            "s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
    }

    private static void registerSolarTracker(int id) {
        int subId;
        {
            subId = 0;
            Eln.sharedItem.addElement(subId + (id << 6), new SolarTrackerDescriptor(TR_NAME(I18N.Type.NONE, "Solar Tracker")));
        }
    }
    private static void recipeSolarTracker() {
        addRecipe(findItemStack("Solar Tracker", 4),
            "VVV",
            "RQR",
            "III",
            'Q', new ItemStack(Items.quartz),
            'V', new ItemStack(Blocks.glass_pane),
            'R', new ItemStack(Items.redstone),
            'G', new ItemStack(Items.gold_ingot),
            'I', new ItemStack(Items.iron_ingot));
    }

    private static void registerMeter(int id) {
        int subId;
        GenericItemUsingDamageDescriptor element;
        {
            subId = 0;
            element = new GenericItemUsingDamageDescriptor(TR_NAME(I18N.Type.NONE, "MultiMeter"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.multiMeterElement = element;
        }
        {
            subId = 1;
            element = new GenericItemUsingDamageDescriptor(TR_NAME(I18N.Type.NONE, "Thermometer"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.thermometerElement = element;
        }
        {
            subId = 2;
            element = new GenericItemUsingDamageDescriptor(TR_NAME(I18N.Type.NONE, "AllMeter"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.allMeterElement = element;
        }
        {
            subId = 8;
            element = new WirelessSignalAnalyserItemDescriptor(TR_NAME(I18N.Type.NONE, "Wireless Analyser"));
            Eln.sharedItem.addElement(subId + (id << 6), element);

        }
        {
            subId = 16;
            element = new ConfigCopyToolDescriptor(TR_NAME(I18N.Type.NONE, "Config Copy Tool"));
            Eln.sharedItem.addElement(subId + (id << 6), element);
            Eln.configCopyToolElement = element;
        }
    }
    private static void recipeMeter() {
        addRecipe(findItemStack("MultiMeter"),
            "RGR",
            "RER",
            "RCR",
            'G', new ItemStack(Blocks.glass_pane),
            'C', findItemStack("Electrical Probe Chip"),
            'E', new ItemStack(Items.redstone),
            'R', "itemRubber");
        addRecipe(findItemStack("Thermometer"),
            "RGR",
            "RER",
            "RCR",
            'G', new ItemStack(Blocks.glass_pane),
            'C', findItemStack("Thermal Probe Chip"),
            'E', new ItemStack(Items.redstone),
            'R', "itemRubber");
        addShapelessRecipe(findItemStack("AllMeter"),
            findItemStack("MultiMeter"),
            findItemStack("Thermometer"));
        addRecipe(findItemStack("Wireless Analyser"),
            " S ",
            "RGR",
            "RER",
            'G', new ItemStack(Blocks.glass_pane),
            'S', findItemStack("Signal Antenna"),
            'E', new ItemStack(Items.redstone),
            'R', "itemRubber");
        addRecipe(findItemStack("Config Copy Tool"),
            "wR",
            "RC",
            'w', findItemStack("Wrench"),
            'R', new ItemStack(Items.redstone),
            'C', Eln.dictAdvancedChip
        );
    }

    private static void registerTreeResinAndRubber(int id) {
        int subId;
        String name;
        {
            TreeResin descriptor;
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Tree Resin");
            descriptor = new TreeResin(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
            Eln.treeResin = descriptor;
            RegistryUtils.addToOre("materialResin", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Rubber");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
            RegistryUtils.addToOre("itemRubber", descriptor.newItemStack());
        }
    }
    private static void recipeGeneral() {
        Utils.addSmelting(Eln.treeResin.parentItem,
            Eln.treeResin.parentItemDamage, findItemStack("Rubber", 1), 0f);
    }

    private static void registerElectricalDrill(int id) {
        int subId;
        String name;
        ElectricalDrillDescriptor descriptor;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Cheap Electrical Drill");
            descriptor = new ElectricalDrillDescriptor(name,8, 4000);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Average Electrical Drill");
            descriptor = new ElectricalDrillDescriptor(name,5, 5000);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Fast Electrical Drill");
            descriptor = new ElectricalDrillDescriptor(name, 3, 6000);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Turbo Electrical Drill");
            descriptor = new ElectricalDrillDescriptor(name, 1, 10000);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Irresponsible Electrical Drill");
            descriptor = new ElectricalDrillDescriptor(name, 0.1, 20000);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
    }
    private static void recipeElectricalDrill() {
        addRecipe(findItemStack("Cheap Electrical Drill"),
            "CMC",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', Eln.dictCheapChip,
            'M', findItemStack("Electrical Motor"),
            'P', new ItemStack(Items.iron_pickaxe));
        addRecipe(findItemStack("Average Electrical Drill"),
            "RCR",
            " D ",
            " d ",
            'R', Items.redstone,
            'C', Eln.dictCheapChip,
            'D', findItemStack("Cheap Electrical Drill"),
            'd', new ItemStack(Items.diamond));
        addRecipe(findItemStack("Fast Electrical Drill"),
            "MCM",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', Eln.dictAdvancedChip,
            'M', findItemStack("Advanced Electrical Motor"),
            'P', new ItemStack(Items.diamond_pickaxe));
        addRecipe(findItemStack("Turbo Electrical Drill"),
            "RCR",
            " F ",
            " D ",
            'F', findItemStack("Fast Electrical Drill"),
            'C', Eln.dictAdvancedChip,
            'R', findItemStack("Graphite Rod"),
            'D', findItemStack("Synthetic Diamond"));
        addRecipe(findItemStack("Irresponsible Electrical Drill"),
            "DDD",
            "DFD",
            "DDD",
            'F', findItemStack("Turbo Electrical Drill"),
            'D', findItemStack("Synthetic Diamond"));
    }

    private static void registerOreScanner(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Ore Scanner");
            Eln.sharedItem.addElement(subId + (id << 6), new OreScanner(name));
        }
    }
    private static void recipeOreScanner() {
        addRecipe(findItemStack("Ore Scanner"),
            "IGI",
            "RCR",
            "IGI",
            'C', Eln.dictCheapChip,
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'G', new ItemStack(Items.gold_ingot));
    }

    private static void registerMiningPipe(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Mining Pipe");
            Eln.miningPipeDescriptor = new MiningPipeDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), Eln.miningPipeDescriptor);
        }
    }
    private static void recipeMiningPipe() {
        addRecipe(findItemStack("Mining Pipe", 12),
            "A",
            "A",
            'A', "ingotAlloy");
    }

    private static void registerRawCable(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Copper Cable");
            Eln.copperCableDescriptor = new CopperCableDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), Eln.copperCableDescriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Iron Cable");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Tungsten Cable");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
    }
    private static void recipeRawCable() {
        addRecipe(findItemStack("Copper Cable", 12),
            "III",
            'I', "ingotCopper");
        addRecipe(findItemStack("Iron Cable", 12),
            "III",
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(findItemStack("Tungsten Cable", 6),
            "III",
            'I', Eln.dictTungstenIngot);
    }

    private static void registerArc(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Graphite Rod");
            Eln.GraphiteDescriptor = new GraphiteDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), Eln.GraphiteDescriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            name = TR_NAME(Type.NONE, "2x Graphite Rods");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 2;
            name = TR_NAME(Type.NONE, "3x Graphite Rods");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 3;
            name = TR_NAME(Type.NONE, "4x Graphite Rods");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 4;
            name = TR_NAME(Type.NONE, "Synthetic Diamond");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 5;
            name = TR_NAME(Type.NONE, "unreleasedium");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 6;
            name = TR_NAME(Type.NONE, "Arc Clay Ingot");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
            OreDictionary.registerOre("ingotAluminum", descriptor.newItemStack());
            OreDictionary.registerOre("ingotAluminium", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 7;
            name = TR_NAME(Type.NONE, "Arc Metal Ingot");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
            OreDictionary.registerOre("ingotSteel", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 8;
            name = TR_NAME(Type.NONE, "Inert Canister");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 11;
            name = TR_NAME(Type.NONE, "Canister of Water");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 12;
            name = TR_NAME(Type.NONE, "Canister of Arc Water");
            descriptor = new GenericItemUsingDamageDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), descriptor);
        }
    }
    private static void recipeGraphite() {
        addRecipe(new ItemStack(Eln.arcClayBlock),
            "III",
            "III",
            "III",
            'I', findItemStack("Arc Clay Ingot"));
        addRecipe(findItemStack("Arc Clay Ingot", 9),
            "I",
            'I', new ItemStack(Eln.arcClayBlock));
        addRecipe(new ItemStack(Eln.arcMetalBlock),
            "III",
            "III",
            "III",
            'I', findItemStack("Arc Metal Ingot"));
        addRecipe(findItemStack("Arc Metal Ingot", 9),
            "I",
            'I', new ItemStack(Eln.arcMetalBlock));
        addRecipe(findItemStack("Graphite Rod", 2),
            "I",
            'I', findItemStack("2x Graphite Rods"));
        addRecipe(findItemStack("Graphite Rod", 3),
            "I",
            'I', findItemStack("3x Graphite Rods"));
        addRecipe(findItemStack("Graphite Rod", 4),
            "I",
            'I', findItemStack("4x Graphite Rods"));
        addShapelessRecipe(
            findItemStack("2x Graphite Rods"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"));
        addShapelessRecipe(
            findItemStack("3x Graphite Rods"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"));
        addShapelessRecipe(
            findItemStack("3x Graphite Rods"),
            findItemStack("Graphite Rod"),
            findItemStack("2x Graphite Rods"));
        addShapelessRecipe(
            findItemStack("4x Graphite Rods"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"));
        addShapelessRecipe(
            findItemStack("4x Graphite Rods"),
            findItemStack("2x Graphite Rods"),
            findItemStack("Graphite Rod"),
            findItemStack("Graphite Rod"));
        addShapelessRecipe(
            findItemStack("4x Graphite Rods"),
            findItemStack("2x Graphite Rods"),
            findItemStack("2x Graphite Rods"));
        addShapelessRecipe(
            findItemStack("4x Graphite Rods"),
            findItemStack("3x Graphite Rods"),
            findItemStack("Graphite Rod"));
        addShapelessRecipe(
            new ItemStack(Items.diamond, 2),
            findItemStack("Synthetic Diamond"));
    }

    private static void registerElectricalTool(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Small Flashlight");
            ElectricalLampItem desc = new ElectricalLampItem(name,
                //10, 8, 20, 15, 5, 50, old
                10, 8, 20, 15, 5, 50,
                6000, 100
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Portable Electrical Mining Drill");
            ElectricalPickaxe desc = new ElectricalPickaxe(name,
                22, 1,// Haxorian note: buffed this from 8,3 putting it around eff 4
                40000, 200, 10000
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "Portable Electrical Axe");
            ElectricalAxe desc = new ElectricalAxe(name,
                22, 1,// Haxorian note: buffed this too
                40000, 200, 10000
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
    }
    private static void recipeElectricalTool() {
        addRecipe(findItemStack("Small Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("50V Incandescent Light Bulb"),
            'B', findItemStack("Portable Battery"),
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Portable Electrical Mining Drill"),
            " T ",
            "IBI",
            " I ",
            'T', findItemStack("Average Electrical Drill"),
            'B', findItemStack("Portable Battery"),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Portable Electrical Axe"),
            " T ",
            "IMI",
            "IBI",
            'T', new ItemStack(Items.iron_axe),
            'B', findItemStack("Portable Battery"),
            'M', findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.iron_ingot));
        if (Eln.xRayScannerCanBeCrafted) {
            addRecipe(findItemStack("X-Ray Scanner"),
                "PGP",
                "PCP",
                "PBP",
                'C', Eln.dictAdvancedChip,
                'B', findItemStack("Portable Battery"),
                'P', findItemStack("Iron Cable"),
                'G', findItemStack("Ore Scanner"));
        }
    }


    private static void registerPortableItem(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Portable Battery");
            BatteryItem desc = new BatteryItem(name,
                40000, 125, 250, 2// Haxorian note: doubled storage halved throughput.
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Portable Battery Pack");

            BatteryItem desc = new BatteryItem(name,
                160000, 500, 1000, 2// Haxorian note: Packs are in 4s now
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Portable Condensator");
            BatteryItem desc = new BatteryItem(name,
                4000, 2000, 2000, 1// H: Slightly less power way more throughput
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 17;
            name = TR_NAME(I18N.Type.NONE, "Portable Condensator Pack");
            BatteryItem desc = new BatteryItem(name,16000, 8000, 8000, 1);
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "X-Ray Scanner");
            PortableOreScannerItem desc = new PortableOreScannerItem(name, Eln.obj.getObj("XRayScanner"),
                100000, 400, 300,// That's right, more buffs!
                Eln.xRayScannerRange, (float) (Math.PI / 2),
                32, 20
            );
            Eln.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
    }
    private static void recipeBatteryItem() {
        addRecipe(findItemStack("Portable Battery"),
            " I ",
            "IPI",
            "IPI",
            'P', "ingotLead",
            'I', new ItemStack(Items.iron_ingot));
        addShapelessRecipe(
            findItemStack("Portable Battery Pack"),
            findItemStack("Portable Battery"),
            findItemStack("Portable Battery"),
            findItemStack("Portable Battery"),
            findItemStack("Portable Battery"));
    }

    private static void registerFuelBurnerItem(int id) {
        Eln.sharedItemStackOne.addElement(0 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(I18N.Type.NONE, "Small Fuel Burner"), 5000 * Eln.fuelHeatFurnacePowerFactor, 2, 1.6f));
        Eln.sharedItemStackOne.addElement(1 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(I18N.Type.NONE, "Medium Fuel Burner"), 10000 * Eln.fuelHeatFurnacePowerFactor, 1, 1.4f));
        Eln.sharedItemStackOne.addElement(2 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(I18N.Type.NONE, "Big Fuel Burner"), 25000 * Eln.fuelHeatFurnacePowerFactor, 0, 1f));
    }

    private static void registerMiscItem(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Cheap Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            OreDictionary.registerOre(Eln.dictCheapChip, desc.newItemStack());
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Advanced Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            OreDictionary.registerOre(Eln.dictAdvancedChip, desc.newItemStack());
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Machine Block");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("casingMachine", desc.newItemStack());
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Electrical Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Thermal Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Copper Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateCopper", desc.newItemStack());
        }
        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Iron Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateIron", desc.newItemStack());
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Gold Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateGold", desc.newItemStack());
        }
        {
            subId = 9;
            name = TR_NAME(I18N.Type.NONE, "Lead Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateLead", desc.newItemStack());
        }
        {
            subId = 10;
            name = TR_NAME(I18N.Type.NONE, "Silicon Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateSilicon", desc.newItemStack());
        }
        {
            subId = 11;
            name = TR_NAME(I18N.Type.NONE, "Alloy Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateAlloy", desc.newItemStack());
        }
        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "Coal Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("plateCoal", desc.newItemStack());
        }
        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Silicon Dust");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("dustSilicon", desc.newItemStack());
        }
        {
            subId = 17;
            name = TR_NAME(I18N.Type.NONE, "Silicon Ingot");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("ingotSilicon", desc.newItemStack());
        }
        {
            subId = 22;
            name = TR_NAME(I18N.Type.NONE, "Machine Booster");
            MachineBoosterDescriptor desc = new MachineBoosterDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 23;
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                TR_NAME(I18N.Type.NONE, "Advanced Machine Block"), new String[]{}); // TODO: Description.
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            RegistryUtils.addToOre("casingMachineAdvanced", desc.newItemStack());
        }
        {
            subId = 28;
            name = TR_NAME(I18N.Type.NONE, "Basic Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 29;
            name = TR_NAME(I18N.Type.NONE, "Advanced Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "Data Logger Print");
            DataLogsPrintDescriptor desc = new DataLogsPrintDescriptor(name);
            Eln.dataLogsPrintDescriptor = desc;
            desc.setDefaultIcon("empty-texture");
            Eln.sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }
        {
            subId = 33;
            name = TR_NAME(I18N.Type.NONE, "Signal Antenna");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 40;
            name = TR_NAME(I18N.Type.NONE, "Player Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityPlayer.class, 0f, 1f, 0f);
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 41;
            name = TR_NAME(I18N.Type.NONE, "Monster Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, IMob.class, 1f, 0f, 0f);
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 42;
            name = TR_NAME(I18N.Type.NONE, "Animal Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityAnimal.class, .3f, .3f, 1f);
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 48;
            name = TR_NAME(I18N.Type.NONE, "Wrench");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, TR("Electrical age wrench,\nCan be used to turn\nsmall wall blocks").split("\n"));
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            Eln.wrenchItemStack = desc.newItemStack();
        }
        {
            subId = 52;
            name = TR_NAME(I18N.Type.NONE, "Dielectric");
            DielectricItem desc = new DielectricItem(name, Eln.LVU);
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }

        Eln.sharedItem.addElement(53 + (id << 6), new CaseItemDescriptor(TR_NAME(I18N.Type.NONE, "Casing")));
        Eln.sharedItem.addElement(54 + (id << 6), new ClutchPlateItem("Iron Clutch Plate", 5120f, 640f, 640f, 160f, 0.0001f, false));
        Eln.sharedItem.addElement(55 + (id << 6), new ClutchPinItem("Clutch Pin"));
        Eln.sharedItem.addElement(56 + (id << 6), new ClutchPlateItem("Gold Clutch Plate", 10240f, 2048f, 1024f, 512f, 0.001f, false));
        Eln.sharedItem.addElement(57 + (id << 6), new ClutchPlateItem("Copper Clutch Plate", 8192f, 4096f, 1024f, 512f, 0.0003f, false));
        Eln.sharedItem.addElement(58 + (id << 6), new ClutchPlateItem("Lead Clutch Plate", 15360f, 1024f, 1536f, 768f, 0.0015f, false));
        Eln.sharedItem.addElement(59 + (id << 6), new ClutchPlateItem("Coal Clutch Plate", 1024f, 128f, 128f, 32f, 0.1f, true));
    }
    private static void recipeMiscItem() {
        addRecipe(findItemStack("Cheap Chip"),
            " R ",
            "RSR",
            " R ",
            'S', "ingotSilicon",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Advanced Chip"),
            "LRL",
            "RCR",
            "LRL",
            'C', Eln.dictCheapChip,
            'L', "ingotSilicon",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Machine Block"),
            "rLr",
            "LcL",
            "rLr",
            'L', findItemStack("Iron Cable"),
            'c', findItemStack("Copper Cable"),
            'r', findItemStack("Tree Resin")
        );
        addRecipe(findItemStack("Advanced Machine Block"),
            "rCr",
            "CcC",
            "rCr",
            'C', "plateAlloy",
            'r', findItemStack("Tree Resin"),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electrical Probe Chip"),
            " R ",
            "RCR",
            " R ",
            'C', findItemStack("High Voltage Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Thermal Probe Chip"),
            " C ",
            "RIR",
            " C ",
            'G', new ItemStack(Items.gold_ingot),
            'I', findItemStack("Iron Cable"),
            'C', "ingotCopper",
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Signal Antenna"),
            "c",
            "c",
            'c', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Machine Booster"),
            "m",
            "c",
            "m",
            'm', findItemStack("Electrical Motor"),
            'c', Eln.dictAdvancedChip);
        addRecipe(findItemStack("Wrench"),
            " c ",
            "cc ",
            "  c",
            'c', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Player Filter"),
            " g",
            "gc",
            " g",
            'g', new ItemStack(Blocks.glass_pane),
            'c', new ItemStack(Items.dye, 1, 2));
        addRecipe(findItemStack("Monster Filter"),
            " g",
            "gc",
            " g",
            'g', new ItemStack(Blocks.glass_pane),
            'c', new ItemStack(Items.dye, 1, 1));
        addRecipe(findItemStack("Casing", 1),
            "ppp",
            "p p",
            "ppp",
            'p', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Iron Clutch Plate"),
            " t ",
            "tIt",
            " t ",
            'I', "plateIron",
            't', Eln.dictTungstenDust
        );
        addRecipe(findItemStack("Gold Clutch Plate"),
            " t ",
            "tGt",
            " t ",
            'G', "plateGold",
            't', Eln.dictTungstenDust
        );
        addRecipe(findItemStack("Copper Clutch Plate"),
            " t ",
            "tCt",
            " t ",
            'C', "plateCopper",
            't', Eln.dictTungstenDust
        );
        addRecipe(findItemStack("Lead Clutch Plate"),
            " t ",
            "tLt",
            " t ",
            'L', "plateLead",
            't', Eln.dictTungstenDust
        );
        addRecipe(findItemStack("Coal Clutch Plate"),
            " t ",
            "tCt",
            " t ",
            'C', "plateCoal",
            't', Eln.dictTungstenDust
        );
        addRecipe(findItemStack("Clutch Pin", 4),
            "s",
            "s",
            's', firstExistingOre("ingotSteel", "ingotAlloy")
        );
    }

    private static void registerBrush(int id) {
        int subId;
        BrushDescriptor whiteDesc = null;
        String name;
        String[] subNames = {
            TR_NAME(I18N.Type.NONE, "Black Brush"),
            TR_NAME(I18N.Type.NONE, "Red Brush"),
            TR_NAME(I18N.Type.NONE, "Green Brush"),
            TR_NAME(I18N.Type.NONE, "Brown Brush"),
            TR_NAME(I18N.Type.NONE, "Blue Brush"),
            TR_NAME(I18N.Type.NONE, "Purple Brush"),
            TR_NAME(I18N.Type.NONE, "Cyan Brush"),
            TR_NAME(I18N.Type.NONE, "Silver Brush"),
            TR_NAME(I18N.Type.NONE, "Gray Brush"),
            TR_NAME(I18N.Type.NONE, "Pink Brush"),
            TR_NAME(I18N.Type.NONE, "Lime Brush"),
            TR_NAME(I18N.Type.NONE, "Yellow Brush"),
            TR_NAME(I18N.Type.NONE, "Light Blue Brush"),
            TR_NAME(I18N.Type.NONE, "Magenta Brush"),
            TR_NAME(I18N.Type.NONE, "Orange Brush"),
            TR_NAME(I18N.Type.NONE, "White Brush")};
        for (int idx = 0; idx < 16; idx++) {
            subId = idx;
            name = subNames[idx];
            BrushDescriptor desc = new BrushDescriptor(name);
            Eln.sharedItem.addElement(subId + (id << 6), desc);
            whiteDesc = desc;
        }
        ItemStack emptyStack = RegistryUtils.findItemStack("White Brush");
        whiteDesc.setLife(emptyStack, 0);
        for (int idx = 0; idx < 16; idx++) {
            RegistryUtils.addShapelessRecipe(emptyStack.copy(),
                new ItemStack(Blocks.wool, 1, idx),
                findItemStack("Iron Cable"));
        }
        for (int idx = 0; idx < 16; idx++) {
            name = subNames[idx];
            RegistryUtils.addShapelessRecipe(findItemStack(name, 1),
                new ItemStack(Items.dye, 1, idx),
                emptyStack.copy());
        }
    }


    private static void recipeECoal() {
        addRecipe(findItemStack("E-Coal Helmet"),
            "PPP",
            "PCP",
            'P', "plateCoal",
            'C', findItemStack("Portable Condensator"));
        addRecipe(findItemStack("E-Coal Boots"),
            " C ",
            "P P",
            "P P",
            'P', "plateCoal",
            'C', findItemStack("Portable Condensator"));
        addRecipe(findItemStack("E-Coal Chestplate"),
            "P P",
            "PCP",
            "PPP",
            'P', "plateCoal",
            'C', findItemStack("Portable Condensator"));
        addRecipe(findItemStack("E-Coal Leggings"),
            "PPP",
            "PCP",
            "P P",
            'P', "plateCoal",
            'C', findItemStack("Portable Condensator"));
    }

    private static void recipePortableCapacitor() {
        addRecipe(findItemStack("Portable Condensator"),
            " r ",
            "cDc",
            " r ",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Iron Cable"),
            'D', findItemStack("Dielectric"));
        addShapelessRecipe(findItemStack("Portable Condensator Pack"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"));
    }

    private static void recipeFuelBurnerItem() {
        addRecipe(findItemStack("Small Fuel Burner"),
            "   ",
            " Cc",
            "   ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));
        addRecipe(findItemStack("Medium Fuel Burner"),
            "   ",
            " Cc",
            " C ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));
        addRecipe(findItemStack("Big Fuel Burner"),
            "   ",
            "CCc",
            "CC ",
            'C', findItemStack("Combustion Chamber"),
            'c', findItemStack("Copper Thermal Cable"));
    }

    private static void recipeFurnace() {
        ItemStack in;
        in = findItemStack("Copper Ore");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Copper Ingot"));
        in = findItemStack("dustCopper");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Copper Ingot"));
        in = findItemStack("Lead Ore");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("ingotLead"));
        in = findItemStack("dustLead");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("ingotLead"));
        in = findItemStack("Tungsten Ore");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Tungsten Ingot"));
        in = findItemStack("Tungsten Dust");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Tungsten Ingot"));
        in = findItemStack("dustIron");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            new ItemStack(Items.iron_ingot));
        in = findItemStack("dustGold");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            new ItemStack(Items.gold_ingot));
        in = findItemStack("Tree Resin");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Rubber", 2));
        in = findItemStack("Alloy Dust");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Alloy Ingot"));
        in = findItemStack("Silicon Dust");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Silicon Ingot"));
        in = findItemStack("dustCinnabar");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            findItemStack("Mercury"));
    }

    private static void recipeMacerator() {
        float f = 4000;
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.coal_ore, 1),
            new ItemStack(Items.coal, 3, 0), 1.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Copper Ore"),
            new ItemStack[]{findItemStack("Copper Dust", 2)}, 1.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.iron_ore),
            new ItemStack[]{findItemStack("Iron Dust", 2)}, 1.5 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gold_ore),
            new ItemStack[]{findItemStack("Gold Dust", 2)}, 3.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Lead Ore"),
            new ItemStack[]{findItemStack("Lead Dust", 2)}, 2.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ore"),
            new ItemStack[]{findItemStack("Tungsten Dust", 2)}, 2.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.coal, 1, 0),
            new ItemStack[]{findItemStack("Coal Dust", 1)}, 1.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.coal, 1, 1),
            new ItemStack[]{findItemStack("Coal Dust", 1)}, 1.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.sand, 1),
            new ItemStack[]{findItemStack("Silicon Dust", 1)}, 3.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Cinnabar Ore"),
            new ItemStack[]{findItemStack("Cinnabar Dust", 1)}, 2.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.dye, 1, 4),
            new ItemStack[]{findItemStack("Lapis Dust", 1)}, 2.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.diamond, 1),
            new ItemStack[]{findItemStack("Diamond Dust", 1)}, 2.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Copper Ingot"),
            new ItemStack[]{findItemStack("Copper Dust", 1)}, 0.5 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot),
            new ItemStack[]{findItemStack("Iron Dust", 1)}, 0.5 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.gold_ingot),
            new ItemStack[]{findItemStack("Gold Dust", 1)}, 0.5 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Lead Ingot"),
            new ItemStack[]{findItemStack("Lead Dust", 1)}, 0.5 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ingot"),
            new ItemStack[]{findItemStack("Tungsten Dust", 1)}, 0.5 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.cobblestone),
            new ItemStack[]{new ItemStack(Blocks.gravel)}, 1.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gravel),
            new ItemStack[]{new ItemStack(Items.flint)}, 1.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.dirt),
            new ItemStack[]{new ItemStack(Blocks.sand)}, 1.0 * f));
        //recycling recipes
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("E-Coal Helmet"),
            new ItemStack[]{findItemStack("Coal Dust", 16)}, 10.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("E-Coal Boots"),
            new ItemStack[]{findItemStack("Coal Dust", 12)}, 10.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("E-Coal Chestplate"),
            new ItemStack[]{findItemStack("Coal Dust", 24)}, 10.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("E-Coal Leggings"),
            new ItemStack[]{findItemStack("Coal Dust", 24)}, 10.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Cost Oriented Battery"),
            new ItemStack[]{findItemStack("Lead Dust", 6)}, 50.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Life Oriented Battery"),
            new ItemStack[]{findItemStack("Lead Dust", 6)}, 50.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Current Oriented Battery"),
            new ItemStack[]{findItemStack("Lead Dust", 6)}, 50.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Voltage Oriented Battery"),
            new ItemStack[]{findItemStack("Lead Dust", 6)}, 50.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Capacity Oriented Battery"),
            new ItemStack[]{findItemStack("Lead Dust", 6)}, 50.0 * f));
        Eln.maceratorRecipes.addRecipe(new Recipe(findItemStack("Single-use Battery"),
            new ItemStack[]{findItemStack("Copper Dust", 3)}, 10.0 * f));
        //end recycling recipes
    }

    private static void recipeArcFurnace() {
        float f = 200000;
        float smeltf = 5000;
        //start smelting recipes
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.iron_ore, 1),
            new ItemStack[]{new ItemStack(Items.iron_ingot, 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gold_ore, 1),
            new ItemStack[]{new ItemStack(Items.gold_ingot, 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.coal_ore, 1),
            new ItemStack[]{new ItemStack(Items.coal, 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.redstone_ore, 1),
            new ItemStack[]{new ItemStack(Items.redstone, 6)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.lapis_ore, 1),
            new ItemStack[]{new ItemStack(Blocks.lapis_block, 1)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.diamond_ore, 1),
            new ItemStack[]{new ItemStack(Items.diamond, 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.emerald_ore, 1),
            new ItemStack[]{new ItemStack(Items.emerald, 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.quartz_ore, 1),
            new ItemStack[]{new ItemStack(Items.quartz, 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(findItemStack("Copper Ore", 1),
            new ItemStack[]{findItemStack("Copper Ingot", 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(findItemStack("Lead Ore", 1),
            new ItemStack[]{findItemStack("Lead Ingot", 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(findItemStack("Tungsten Ore", 1),
            new ItemStack[]{findItemStack("Tungsten Ingot", 2)}, smeltf));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(findItemStack("Alloy Dust", 1),
            new ItemStack[]{findItemStack("Alloy Ingot", 1)}, smeltf));
        //end smelting recipes
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Items.clay_ball, 2),
            new ItemStack[]{findItemStack("Arc Clay Ingot", 1)}, 2.0 * f));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, 1),
            new ItemStack[]{findItemStack("Arc Metal Ingot", 1)}, 1.0 * f));
        Eln.arcFurnaceRecipes.addRecipe(new Recipe(findItemStack("Canister of Water", 1),
            new ItemStack[]{findItemStack("Canister of Arc Water", 1)}, 7000000)); //hardcoded 7MJ to prevent overunity
    }

    private static void recipePlateMachine() {
        float f = 10000;
        Eln.plateMachineRecipes.addRecipe(new Recipe(
            findItemStack("Copper Ingot", Eln.plateConversionRatio),
            findItemStack("Copper Plate"), 1.0 * f));
        Eln.plateMachineRecipes.addRecipe(new Recipe(findItemStack("Lead Ingot", Eln.plateConversionRatio),
            findItemStack("Lead Plate"), 1.0 * f));
        Eln.plateMachineRecipes.addRecipe(new Recipe(
            findItemStack("Silicon Ingot", 4),
            findItemStack("Silicon Plate"), 1.0 * f));
        Eln.plateMachineRecipes.addRecipe(new Recipe(findItemStack("Alloy Ingot", Eln.plateConversionRatio),
            findItemStack("Alloy Plate"), 1.0 * f));
        Eln.plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, Eln.plateConversionRatio,
            0), findItemStack("Iron Plate"), 1.0 * f));
        Eln.plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.gold_ingot, Eln.plateConversionRatio,
            0), findItemStack("Gold Plate"), 1.0 * f));
    }

    private static void recipeCompressor() {
        Eln.compressorRecipes.addRecipe(new Recipe(findItemStack("4x Graphite Rods", 1),
            findItemStack("Synthetic Diamond"), 80000.0));
        Eln.compressorRecipes.addRecipe(new Recipe(findItemStack("Coal Dust", 4),
            findItemStack("Coal Plate"), 40000.0));
        Eln.compressorRecipes.addRecipe(new Recipe(findItemStack("Coal Plate", 4),
            findItemStack("Graphite Rod"), 80000.0));
        Eln.compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.sand),
            findItemStack("Dielectric"), 2000.0));
        Eln.compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.log),
            findItemStack("Tree Resin"), 3000.0));
    }

    private static void recipeMagnetizer() {
        Eln.magnetiserRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, 2),
            new ItemStack[]{findItemStack("Basic Magnet")}, 5000.0));
        Eln.magnetiserRecipes.addRecipe(new Recipe(findItemStack("Alloy Ingot", 2),
            new ItemStack[]{findItemStack("Advanced Magnet")}, 15000.0));
        Eln.magnetiserRecipes.addRecipe(new Recipe(findItemStack("Copper Dust", 1),
            new ItemStack[]{new ItemStack(Items.redstone)}, 5000.0));
        Eln.magnetiserRecipes.addRecipe(new Recipe(findItemStack("Basic Magnet", 3),
            new ItemStack[]{findItemStack("Optimal Ferromagnetic Core")}, 5000.0));
        Eln.magnetiserRecipes.addRecipe(new Recipe(findItemStack("Inert Canister", 1),
            new ItemStack[]{new ItemStack(Items.ender_pearl)}, 150000.0));
    }
}

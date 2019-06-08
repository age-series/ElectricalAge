package mods.eln.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static mods.eln.i18n.I18N.TR_NAME;

public class RecipeRegistry {
    public RecipeRegistry() {}

    /**
     * register - registers ALL crafting recipes
     */
    public void register() {
        recipeEnergyConverter();
        recipeComputerProbe();

        recipeArmor();
        recipeTool();

        recipeGround();
        recipeElectricalCable();
        recipeThermalCable();
        recipeLampSocket();
        recipeLampSupply();
        recipePowerSocket();
        recipePassiveComponent();
        recipeSwitch();
        recipeWirelessSignal();
        recipeElectricalRelay();
        recipeElectricalDataLogger();
        recipeElectricalGateSource();
        recipeElectricalBreaker();
        recipeFuses();
        recipeElectricalVuMeter();
        recipeElectricalEnvironmentalSensor();
        recipeElectricalRedstone();
        recipeElectricalGate();
        recipeElectricalAlarm();
        recipeElectricalSensor();
        recipeThermalSensor();
        recipeSixNodeMisc();


        recipeTurret();
        recipeMachine();
        recipeChips();
        recipeTransformer();
        recipeHeatFurnace();
        recipeTurbine();
        recipeBattery();
        recipeElectricalFurnace();
        recipeAutoMiner();
        recipeSolarPanel();

        recipeThermalDissipatorPassiveAndActive();
        recipeElectricalAntenna();
        recipeEggIncubator();
        recipeBatteryCharger();
        recipeTransporter();
        recipeWindTurbine();
        recipeFuelGenerator();

        recipeGeneral();
        recipeHeatingCorp();
        recipeRegulatorItem();
        recipeLampItem();
        recipeProtection();
        recipeCombustionChamber();
        recipeFerromagneticCore();
        recipeDust();
        recipeElectricalMotor();
        recipeSolarTracker();
        recipeMeter();
        recipeElectricalDrill();
        recipeOreScanner();
        recipeMiningPipe();
        recipeTreeResinAndRubber();
        recipeRawCable();
        recipeGraphite();
        recipeMiscItem();
        recipeBatteryItem();
        recipeElectricalTool();
        recipePortableCapacitor();

        recipeFurnace();
        recipeArcFurnace();
        recipeMacerator();
        recipeCompressor();
        recipePlateMachine();
        recipeMagnetizer();
        recipeFuelBurnerItem();
        recipeDisplays();

        recipeECoal();

        HashSet<String> oreNames = new HashSet<String>();
        {
            final String[] names = OreDictionary.getOreNames();
            Collections.addAll(oreNames, names);
        }

        recipeGridDevices(oreNames);

    }

    public static void addRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(new ShapedOreRecipe(output, params));
    }

    public static void addShapelessRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(output, params));
    }

    public static ItemStack findItemStack(String name, int stackSize) {
        ItemStack stack = GameRegistry.findItemStack("Eln", name, stackSize);
        if (stack == null) {
            stack = Eln.dictionnaryOreFromMod.get(name);
            stack = Utils.newItemStack(Item.getIdFromItem(stack.getItem()), stackSize, stack.getItemDamage());
        }
        return stack;
    }

    public static String firstExistingOre(String... oreNames) {
        for (String oreName : oreNames) {
            if (OreDictionary.doesOreNameExist(oreName)) {
                return oreName;
            }
        }
        return "";
    }

    public static ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    public static void recipeMaceratorModOre(float f, String inputName, String outputName, int outputCount) {
        if (!OreDictionary.doesOreNameExist(inputName)) {
            LogWrapper.info("No entries for oredict: " + inputName);
            return;
        }
        if (!OreDictionary.doesOreNameExist(outputName)) {
            LogWrapper.info("No entries for oredict: " + outputName);
            return;
        }
        ArrayList<ItemStack> inOres = OreDictionary.getOres(inputName);
        ArrayList<ItemStack> outOres = OreDictionary.getOres(outputName);
        if (inOres.size() == 0) {
            LogWrapper.info("No ores in oredict entry: " + inputName);
        }
        if (outOres.size() == 0) {
            LogWrapper.info("No ores in oredict entry: " + outputName);
            return;
        }
        ItemStack output = outOres.get(0).copy();
        output.stackSize = outputCount;
        LogWrapper.info("Adding mod recipe from " + inputName + " to " + outputName);
        for (ItemStack input : inOres) {
            Eln.maceratorRecipes.addRecipe(new Recipe(input, output, f));
        }
    }

    private void recipeGround() {
        addRecipe(findItemStack("Ground Cable"),
            " C ",
            " C ",
            "CCC",
            'C', findItemStack("Copper Cable"));
    }

    private void recipeElectricalCable() {
        addRecipe(Eln.signalCableDescriptor.newItemStack(2),
            "R",
            "C",
            "C",
            'C', findItemStack("Iron Cable"),
            'R', "itemRubber");
        addRecipe(Eln.lowVoltageCableDescriptor.newItemStack(2),
            "R",
            "C",
            "C",
            'C', findItemStack("Copper Cable"),
            'R', "itemRubber");
        addRecipe(Eln.meduimVoltageCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', Eln.lowVoltageCableDescriptor.newItemStack(1),
            'R', "itemRubber");
        addRecipe(Eln.highVoltageCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', Eln.meduimVoltageCableDescriptor.newItemStack(1),
            'R', "itemRubber");
        addRecipe(Eln.signalCableDescriptor.newItemStack(12),
            "RRR",
            "CCC",
            "RRR",
            'C', new ItemStack(Items.iron_ingot),
            'R', "itemRubber");
        addRecipe(Eln.signalBusCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', Eln.signalCableDescriptor.newItemStack(1),
            'R', "itemRubber");
        addRecipe(Eln.lowVoltageCableDescriptor.newItemStack(12),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotCopper",
            'R', "itemRubber");
        addRecipe(Eln.veryHighVoltageCableDescriptor.newItemStack(12),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotAlloy",
            'R', "itemRubber");
    }

    private void recipeThermalCable() {
        addRecipe(findItemStack("Copper Thermal Cable", 12),
            "SSS",
            "CCC",
            "SSS",
            'S', new ItemStack(Blocks.cobblestone),
            'C', "ingotCopper");
        addRecipe(findItemStack("Copper Thermal Cable", 1),
            "S",
            "C",
            'S', new ItemStack(Blocks.cobblestone),
            'C', findItemStack("Copper Cable"));
    }

    private void recipeLampSocket() {
        addRecipe(findItemStack("Lamp Socket A", 3),
            "G ",
            "IG",
            "G ",
            'G', new ItemStack(Blocks.glass_pane),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Lamp Socket B Projector", 3),
            " G",
            "GI",
            " G",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Street Light", 1),
            "G",
            "I",
            "I",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Robust Lamp Socket", 3),
            "GIG",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Flat Lamp Socket", 3),
            "IGI",
            'G', new ItemStack(Blocks.glass_pane),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Simple Lamp Socket", 3),
            " I ",
            "GGG",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Fluorescent Lamp Socket", 3),
            " I ",
            "G G",
            'G', findItemStack("Iron Cable"),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("Suspended Lamp Socket", 2),
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Long Suspended Lamp Socket", 2),
            "I",
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Sconce Lamp Socket", 2),
            "GCG",
            "GIG",
            'G', new ItemStack(Blocks.glass_pane),
            'C', "dustCoal",
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(findItemStack("50V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Low Voltage Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("50V LED Bulb"),
            'g', new ItemStack(Blocks.glass_pane));
        addRecipe(findItemStack("200V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Medium Voltage Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("200V LED Bulb"),
            'g', new ItemStack(Blocks.glass_pane));
    }

    private void recipeLampSupply() {
        addRecipe(findItemStack("Lamp Supply", 1),
            " I ",
            "ICI",
            " I ",
            'C', "ingotCopper",
            'I', new ItemStack(Items.iron_ingot));
    }

    private void recipePowerSocket() {
        addRecipe(findItemStack("50V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', findItemStack("Copper Plate"),
            'A', findItemStack("Alloy Plate"),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("200V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', findItemStack("Copper Plate"),
            'A', findItemStack("Alloy Plate"),
            'C', findItemStack("Medium Voltage Cable"));
    }

    private void recipePassiveComponent() {
        addRecipe(findItemStack("Signal Diode", 4),
            " RB",
            " IR",
            " RB",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'B', "itemRubber");
        addRecipe(findItemStack("10A Diode", 3),
            " RB",
            "IIR",
            " RB",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'B', "itemRubber");
        addRecipe(findItemStack("25A Diode"),
            "D",
            "D",
            "D",
            'D', findItemStack("10A Diode"));
        addRecipe(findItemStack("Power Capacitor"),
            "cPc",
            "III",
            'I', new ItemStack(Items.iron_ingot),
            'c', findItemStack("Iron Cable"),
            'P', "plateIron");
        addRecipe(findItemStack("Power Inductor"),
            "   ",
            "cIc",
            "   ",
            'I', new ItemStack(Items.iron_ingot),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Power Resistor"),
            "   ",
            "cCc",
            "   ",
            'c', findItemStack("Copper Cable"),
            'C', findItemStack("Coal Dust"));
        addRecipe(findItemStack("Rheostat"),
            " R ",
            " MS",
            "cmc",
            'R', findItemStack("Power Resistor"),
            'c', findItemStack("Copper Cable"),
            'm', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"),
            'S', findItemStack("Signal Cable")
        );
        addRecipe(findItemStack("Thermistor"),
            "   ",
            "csc",
            "   ",
            's', "dustSilicon",
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Large Rheostat"),
            "   ",
            " D ",
            "CRC",
            'R', findItemStack("Rheostat"),
            'C', findItemStack("Copper Thermal Cable"),
            'D', findItemStack("Small Passive Thermal Dissipator")
        );
    }

    private void recipeSwitch() {
        addRecipe(findItemStack("Low Voltage Switch"),
            "  I",
            " I ",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("Medium Voltage Switch"),
            "  I",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Medium Voltage Cable"));
        addRecipe(findItemStack("High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("High Voltage Cable"));
        addRecipe(findItemStack("Very High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Very High Voltage Cable"));
    }

    private void recipeElectricalRelay() {
        addRecipe(findItemStack("Low Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("Medium Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Medium Voltage Cable"));
        addRecipe(findItemStack("High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("High Voltage Cable"));
        addRecipe(findItemStack("Very High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Very High Voltage Cable"));

        addRecipe(findItemStack("Signal Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Signal Cable"));
    }

    private void recipeWirelessSignal() {
        addRecipe(findItemStack("Wireless Signal Transmitter"),
            " S ",
            " R ",
            "ICI",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'C', Eln.dictCheapChip,
            'S', findItemStack("Signal Antenna"));
        addRecipe(findItemStack("Wireless Signal Repeater"),
            "S S",
            "R R",
            "ICI",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'C', Eln.dictCheapChip,
            'S', findItemStack("Signal Antenna"));
        addRecipe(findItemStack("Wireless Signal Receiver"),
            " S ",
            "ICI",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'C', Eln.dictCheapChip,
            'S', findItemStack("Signal Antenna"));
    }

    private void recipeChips() {
        addRecipe(findItemStack("NOT Chip"),
            "   ",
            "cCr",
            "   ",
            'C', Eln.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("AND Chip"),
            " c ",
            "cCc",
            " c ",
            'C', Eln.dictCheapChip,
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("NAND Chip"),
            " c ",
            "cCr",
            " c ",
            'C', Eln.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("OR Chip"),
            " r ",
            "rCr",
            " r ",
            'C', Eln.dictCheapChip,
            'r', new ItemStack(Items.redstone));
        addRecipe(findItemStack("NOR Chip"),
            " r ",
            "rCc",
            " r ",
            'C', Eln.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("XOR Chip"),
            " rr",
            "rCr",
            " rr",
            'C', Eln.dictCheapChip,
            'r', new ItemStack(Items.redstone));
        addRecipe(findItemStack("XNOR Chip"),
            " rr",
            "rCc",
            " rr",
            'C', Eln.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("PAL Chip"),
            "rcr",
            "cCc",
            "rcr",
            'C', Eln.dictAdvancedChip,
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Schmitt Trigger Chip"),
            "   ",
            "cCc",
            "   ",
            'C', Eln.dictAdvancedChip,
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("D Flip Flop Chip"),
            "   ",
            "cCc",
            " p ",
            'C', Eln.dictAdvancedChip,
            'p', findItemStack("Copper Plate"),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Oscillator Chip"),
            "pdp",
            "cCc",
            "   ",
            'C', Eln.dictAdvancedChip,
            'p', findItemStack("Copper Plate"),
            'c', findItemStack("Copper Cable"),
            'd', findItemStack("Dielectric"));
        addRecipe(findItemStack("JK Flip Flop Chip"),
            " p ",
            "cCc",
            " p ",
            'C', Eln.dictAdvancedChip,
            'p', findItemStack("Copper Plate"),
            'c', findItemStack("Copper Cable"));
        addRecipe(findItemStack("Amplifier"),
            "  r",
            "cCc",
            "   ",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("OpAmp"),
            "  r",
            "cCc",
            " c ",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("Configurable summing unit"),
            " cr",
            "cCc",
            " c ",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("Sample and hold"),
            " rr",
            "cCc",
            " c ",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("Voltage controlled sine oscillator"),
            "rrr",
            "cCc",
            "   ",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("Voltage controlled sawtooth oscillator"),
            "   ",
            "cCc",
            "rrr",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("PID Regulator"),
            "rrr",
            "cCc",
            "rcr",
            'r', new ItemStack(Items.redstone),
            'c', findItemStack("Copper Cable"),
            'C', Eln.dictAdvancedChip);
        addRecipe(findItemStack("Lowpass filter"),
            "CdC",
            "cDc",
            " s ",
            'd', findItemStack("Dielectric"),
            'c', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Plate"),
            'D', findItemStack("Coal Dust"),
            's', Eln.dictCheapChip);
    }

    private void recipeTransformer() {
        addRecipe(findItemStack("DC-DC Converter"),
            "C C",
            "III",
            'C', findItemStack("Copper Cable"),
            'I', new ItemStack(Items.iron_ingot));
    }

    private void recipeHeatFurnace() {
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

    private void recipeTurbine() {
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

    private void recipeBattery() {
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

    private void recipeGridDevices(HashSet<String> oreNames) {
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

    private void recipeElectricalFurnace() {
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

    private void recipeSixNodeMisc() {
        addRecipe(findItemStack("Analog Watch"),
            "crc",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Digital Watch"),
            "rcr",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Hub"),
            "I I",
            " c ",
            "I I",
            'c', findItemStack("Copper Cable"),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Energy Meter"),
            "IcI",
            "IRI",
            "IcI",
            'c', findItemStack("Copper Cable"),
            'R', Eln.dictCheapChip,
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Advanced Energy Meter"),
            " c ",
            "PRP",
            " c ",
            'c', findItemStack("Copper Cable"),
            'R', Eln.dictAdvancedChip,
            'P', findItemStack("Iron Plate"));
    }

    private void recipeAutoMiner() {
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

    private void recipeWindTurbine() {
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

    private void recipeFuelGenerator() {
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

    private void recipeSolarPanel() {
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

    private void recipeThermalDissipatorPassiveAndActive() {
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

    private void recipeGeneral() {
        Utils.addSmelting(Eln.treeResin.parentItem,
            Eln.treeResin.parentItemDamage, findItemStack("Rubber", 1), 0f);
    }

    private void recipeHeatingCorp() {
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

    private void recipeRegulatorItem() {
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

    private void recipeLampItem() {
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

    private void recipeProtection() {
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

    private void recipeCombustionChamber() {
        addRecipe(findItemStack("Combustion Chamber"),
            " L ",
            "L L",
            " L ",
            'L', new ItemStack(Blocks.stone));
    }

    private void recipeFerromagneticCore() {
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

    private void recipeDust() {
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

    private void recipeElectricalMotor() {
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

    private void recipeSolarTracker() {
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

    private void recipeMeter() {
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

    private void recipeElectricalDrill() {
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

    private void recipeOreScanner() {
        addRecipe(findItemStack("Ore Scanner"),
            "IGI",
            "RCR",
            "IGI",
            'C', Eln.dictCheapChip,
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"),
            'G', new ItemStack(Items.gold_ingot));
    }

    private void recipeMiningPipe() {
        addRecipe(findItemStack("Mining Pipe", 12),
            "A",
            "A",
            'A', "ingotAlloy");
    }

    private void recipeTreeResinAndRubber() {
        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            "WW ", 'W', "plankWood");
        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            " WW", 'W', "plankWood");
    }

    private void recipeRawCable() {
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

    private void recipeGraphite() {
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

    private void recipeBatteryItem() {
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

    private void recipeElectricalTool() {
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

    private void recipeECoal() {
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

    private void recipePortableCapacitor() {
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

    private void recipeMiscItem() {
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

    private void recipeMacerator() {
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

    private void recipeArcFurnace() {
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

    private void recipePlateMachine() {
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

    private void recipeCompressor() {
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

    private void recipeMagnetizer() {
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

    private void recipeFuelBurnerItem() {
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

    private void recipeFurnace() {
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

    private void recipeElectricalSensor() {
        addRecipe(findItemStack("Voltage Probe", 1),
            "SC",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Electrical Probe", 1),
            "SCS",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));
    }

    private void recipeThermalSensor() {
        addRecipe(findItemStack("Thermal Probe", 1),
            "SCS",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));
        addRecipe(findItemStack("Temperature Probe", 1),
            "SC",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));
    }

    private void recipeTransporter() {
        addRecipe(findItemStack("Experimental Transporter", 1),
            "RMR",
            "RMR",
            " R ",
            'M', findItemStack("Advanced Machine Block"),
            'C', findItemStack("High Voltage Cable"),
            'R', Eln.dictAdvancedChip);
    }

    private void recipeTurret() {
        addRecipe(findItemStack("800V Defence Turret", 1),
            " R ",
            "CMC",
            " c ",
            'M', findItemStack("Advanced Machine Block"),
            'C', Eln.dictAdvancedChip,
            'c', Eln.highVoltageCableDescriptor.newItemStack(),
            'R', new ItemStack(Blocks.redstone_block));
    }

    private void recipeMachine() {
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

    private void recipeElectricalGate() {
        addShapelessRecipe(findItemStack("Electrical Timer"),
            new ItemStack(Items.repeater),
            Eln.dictCheapChip);
        addRecipe(findItemStack("Signal Processor", 1),
            "IcI",
            "cCc",
            "IcI",
            'I', new ItemStack(Items.iron_ingot),
            'c', findItemStack("Signal Cable"),
            'C', Eln.dictCheapChip);
    }

    private void recipeElectricalRedstone() {
        addRecipe(findItemStack("Redstone-to-Voltage Converter", 1),
            "TCS",
            'S', findItemStack("Signal Cable"),
            'C', Eln.dictCheapChip,
            'T', new ItemStack(Blocks.redstone_torch));

        addRecipe(findItemStack("Voltage-to-Redstone Converter", 1),
            "CTR",
            'R', new ItemStack(Items.redstone),
            'C', Eln.dictCheapChip,
            'T', new ItemStack(Blocks.redstone_torch));
    }

    private void recipeElectricalEnvironmentalSensor() {
        addShapelessRecipe(findItemStack("Electrical Daylight Sensor"),
            new ItemStack(Blocks.daylight_detector),
            findItemStack("Redstone-to-Voltage Converter"));
        addShapelessRecipe(findItemStack("Electrical Light Sensor"),
            new ItemStack(Blocks.daylight_detector),
            new ItemStack(Items.quartz),
            findItemStack("Redstone-to-Voltage Converter"));
        addRecipe(findItemStack("Electrical Weather Sensor"),
            " r ",
            "rRr",
            " r ",
            'R', new ItemStack(Items.redstone),
            'r', "itemRubber");
        addRecipe(findItemStack("Electrical Anemometer Sensor"),
            " I ",
            " R ",
            "I I",
            'R', new ItemStack(Items.redstone),
            'I', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Electrical Entity Sensor"),
            " G ",
            "GRG",
            " G ",
            'G', new ItemStack(Blocks.glass_pane),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Electrical Fire Detector"),
            "cbr",
            "p p",
            "r r",
            'c', findItemStack("Signal Cable"),
            'b', Eln.dictCheapChip,
            'r', "itemRubber",
            'p', "plateCopper");
        addRecipe(findItemStack("Electrical Fire Buzzer"),
            "rar",
            "p p",
            "r r",
            'a', Eln.dictAdvancedChip,
            'r', "itemRubber",
            'p', "plateCopper");
        addShapelessRecipe(findItemStack("Scanner"),
            new ItemStack(Items.comparator),
            Eln.dictAdvancedChip);
    }

    private void recipeElectricalVuMeter() {
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("Analog vuMeter", 1),
                "WWW",
                "RIr",
                "WSW",
                'W', new ItemStack(Blocks.planks, 1, idx),
                'R', new ItemStack(Items.redstone),
                'I', findItemStack("Iron Cable"),
                'r', new ItemStack(Items.dye, 1, 1),
                'S', findItemStack("Signal Cable"));
        }
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("LED vuMeter", 1),
                " W ",
                "WTW",
                " S ",
                'W', new ItemStack(Blocks.planks, 1, idx),
                'T', new ItemStack(Blocks.redstone_torch),
                'S', findItemStack("Signal Cable"));
        }
    }

    private void recipeElectricalBreaker() {
        addRecipe(findItemStack("Electrical Breaker", 1),
            "crC",
            'c', findItemStack("Overvoltage Protection"),
            'C', findItemStack("Overheating Protection"),
            'r', findItemStack("High Voltage Relay"));

    }

    private void recipeFuses() {
        addRecipe(findItemStack("Electrical Fuse Holder", 1),
            "i",
            " ",
            "i",
            'i', findItemStack("Iron Cable"));
        addRecipe(findItemStack("Lead Fuse for low voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Low Voltage Cable"));
        addRecipe(findItemStack("Lead Fuse for medium voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Medium Voltage Cable"));
        addRecipe(findItemStack("Lead Fuse for high voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("High Voltage Cable"));
        addRecipe(findItemStack("Lead Fuse for very high voltage cables", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Very High Voltage Cable"));
    }

    private void recipeElectricalGateSource() {
        addRecipe(findItemStack("Signal Trimmer", 1),
            "RsR",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            's', new ItemStack(Items.stick),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Signal Switch", 3),
            " r ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Signal Button", 3),
            " R ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Wireless Switch", 3),
            " a ",
            "rCr",
            " r ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', Eln.dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("Wireless Button", 3),
            " a ",
            "rCr",
            " R ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', Eln.dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
    }

    private void recipeElectricalDataLogger() {
        addRecipe(findItemStack("Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', Eln.dictCheapChip,
            'G', new ItemStack(Blocks.glass_pane));
        addRecipe(findItemStack("Modern Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', Eln.dictAdvancedChip,
            'G', new ItemStack(Blocks.glass_pane));
        addRecipe(findItemStack("Industrial Data Logger", 1),
            "RRR",
            "GGG",
            "RCR",
            'R', "itemRubber",
            'C', Eln.dictAdvancedChip,
            'G', new ItemStack(Blocks.glass_pane));
    }

    private void recipeElectricalAlarm() {
        addRecipe(findItemStack("Nuclear Alarm", 1),
            "ITI",
            "IMI",
            "IcI",
            'c', findItemStack("Signal Cable"),
            'T', new ItemStack(Blocks.redstone_torch),
            'I', findItemStack("Iron Cable"),
            'M', new ItemStack(Blocks.noteblock));
        addRecipe(findItemStack("Standard Alarm", 1),
            "MTM",
            "IcI",
            "III",
            'c', findItemStack("Signal Cable"),
            'T', new ItemStack(Blocks.redstone_torch),
            'I', findItemStack("Iron Cable"),
            'M', new ItemStack(Blocks.noteblock));
    }

    private void recipeElectricalAntenna() {
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

    private void recipeBatteryCharger() {
        addRecipe(findItemStack("Weak 50V Battery Charger", 1),
            "RIR",
            "III",
            "RcR",
            'c', findItemStack("Low Voltage Cable"),
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(findItemStack("50V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', Eln.dictCheapChip,
            'c', findItemStack("Low Voltage Cable"),
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(findItemStack("200V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', Eln.dictAdvancedChip,
            'c', findItemStack("Medium Voltage Cable"),
            'I', findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
    }

    private void recipeEggIncubator() {
        addRecipe(findItemStack("50V Egg Incubator", 1),
            "IGG",
            "E G",
            "CII",
            'C', Eln.dictCheapChip,
            'E', findItemStack("Small 50V Tungsten Heating Corp"),
            'I', new ItemStack(Items.iron_ingot),
            'G', new ItemStack(Blocks.glass_pane));
    }

    private void recipeEnergyConverter() {
        if (Eln.ElnToOtherEnergyConverterEnable) {
            addRecipe(new ItemStack(Eln.elnToOtherBlockLvu),
                "III",
                "cCR",
                "III",
                'C', Eln.dictCheapChip,
                'c', findItemStack("Low Voltage Cable"),
                'I', findItemStack("Iron Cable"),
                'R', "ingotCopper");
            addRecipe(new ItemStack(Eln.elnToOtherBlockMvu),
                "III",
                "cCR",
                "III",
                'C', Eln.dictCheapChip,
                'c', findItemStack("Medium Voltage Cable"),
                'I', findItemStack("Iron Cable"),
                'R', Eln.dictTungstenIngot);
            addRecipe(new ItemStack(Eln.elnToOtherBlockHvu),
                "III",
                "cCR",
                "III",
                'C', Eln.dictAdvancedChip,
                'c', findItemStack("High Voltage Cable"),
                'I', findItemStack("Iron Cable"),
                'R', new ItemStack(Items.gold_ingot));
        }
    }

    private void recipeComputerProbe() {
        if (Eln.ComputerProbeEnable) {
            addRecipe(new ItemStack(Eln.computerProbeBlock),
                "cIw",
                "ICI",
                "WIc",
                'C', Eln.dictAdvancedChip,
                'c', findItemStack("Signal Cable"),
                'I', findItemStack("Iron Cable"),
                'w', findItemStack("Wireless Signal Receiver"),
                'W', findItemStack("Wireless Signal Transmitter"));
        }
    }

    private void recipeArmor() {
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

    private void recipeTool() {
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

    private void recipeDisplays() {
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

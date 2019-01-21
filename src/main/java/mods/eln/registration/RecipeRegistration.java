package mods.eln.registration;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Vars;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RecipeRegistration {

    public RecipeRegistration() {

    }

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
        recipeSixNodeCache();
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
        recipeIngot();
        recipeDust();
        recipeElectricalMotor();
        recipeSolarTracker();
        recipeDynamo();
        recipeWindRotor();
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

        recipeGridDevices(Vars.oreNames);
    }

    public void addRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(new ShapedOreRecipe(output, params));
    }

    public void addShapelessRecipe(ItemStack output, Object... params) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(output, params));
    }





    public void recipeMaceratorModOres() {
        float f = 4000;

        // AE2:
        recipeMaceratorModOre(f * 3f, "oreCertusQuartz", "dustCertusQuartz", 3);
        recipeMaceratorModOre(f * 1.5f, "crystalCertusQuartz", "dustCertusQuartz", 1);
        recipeMaceratorModOre(f * 3f, "oreNetherQuartz", "dustNetherQuartz", 3);
        recipeMaceratorModOre(f * 1.5f, "crystalNetherQuartz", "dustNetherQuartz", 1);
        recipeMaceratorModOre(f * 1.5f, "crystalFluix", "dustFluix", 1);
    }

    public void checkRecipe() {
        Utils.println("No recipe for ");
        for (SixNodeDescriptor d : Vars.sixNodeItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
        for (TransparentNodeDescriptor d : Vars.transparentNodeItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
        for (GenericItemUsingDamageDescriptor d : Vars.sharedItem.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
        for (GenericItemUsingDamageDescriptor d : Vars.sharedItemStackOne.subItemList.values()) {
            ItemStack stack = d.newItemStack();
            if (!recipeExists(stack)) {
                Utils.println("  " + d.name);
            }
        }
    }

    private boolean recipeExists(ItemStack stack) {
        if (stack == null)
            return false;
        List list = CraftingManager.getInstance().getRecipeList();
        for (Object o : list) {
            if (o instanceof IRecipe) {
                IRecipe r = (IRecipe) o;
                if (r.getRecipeOutput() == null)
                    continue;
                if (Utils.areSame(stack, r.getRecipeOutput()))
                    return true;
            }
        }
        return false;
    }










    private void recipeGround() {
        addRecipe(Vars.findItemStack("Ground Cable"),
            " C ",
            " C ",
            "CCC",
            'C', Vars.findItemStack("Copper Cable"));
    }

    private void recipeElectricalCable() {
        addRecipe(Vars.signalCableDescriptor.newItemStack(2), //signal wire
            "R", //rubber
            "C", //iron cable
            "C",
            'C', Vars.findItemStack("Iron Cable"),
            'R', "itemRubber");

        addRecipe(Vars.lowVoltageCableDescriptor.newItemStack(2), //Low Voltage Cable
            "R",
            "C",
            "C",
            'C', Vars.findItemStack("Copper Cable"),
            'R', "itemRubber");

        addRecipe(Vars.meduimVoltageCableDescriptor.newItemStack(1), //Meduim Voltage Cable (Medium Voltage Cable)
            "R",
            "C",
            'C', Vars.lowVoltageCableDescriptor.newItemStack(1),
            'R', "itemRubber");

        addRecipe(Vars.highVoltageCableDescriptor.newItemStack(1), //High Voltage Cable
            "R",
            "C",
            'C', Vars.meduimVoltageCableDescriptor.newItemStack(1),
            'R', "itemRubber");

        addRecipe(Vars.signalCableDescriptor.newItemStack(12), //Signal Wire
            "RRR",
            "CCC",
            "RRR",
            'C', new ItemStack(Items.iron_ingot),
            'R', "itemRubber");

        addRecipe(Vars.signalBusCableDescriptor.newItemStack(1),
            "R",
            "C",
            'C', Vars.signalCableDescriptor.newItemStack(1),
            'R', "itemRubber");

        addRecipe(Vars.lowVoltageCableDescriptor.newItemStack(12),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotCopper",
            'R', "itemRubber");


        addRecipe(Vars.veryHighVoltageCableDescriptor.newItemStack(12),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotAlloy",
            'R', "itemRubber");

    }

    private void recipeThermalCable() {
        addRecipe(Vars.findItemStack("Copper Thermal Cable", 12),
            "SSS",
            "CCC",
            "SSS",
            'S', new ItemStack(Blocks.cobblestone),
            'C', "ingotCopper");

        addRecipe(Vars.findItemStack("Copper Thermal Cable", 1),
            "S",
            "C",
            'S', new ItemStack(Blocks.cobblestone),
            'C', Vars.findItemStack("Copper Cable"));
    }

    private void recipeLampSocket() {
        addRecipe(Vars.findItemStack("Lamp Socket A", 3),
            "G ",
            "IG",
            "G ",
            'G', new ItemStack(Blocks.glass_pane),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Lamp Socket B Projector", 3),
            " G",
            "GI",
            " G",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Street Light", 1),
            "G",
            "I",
            "I",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Robust Lamp Socket", 3),
            "GIG",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));
        addRecipe(Vars.findItemStack("Flat Lamp Socket", 3),
            "IGI",
            'G', new ItemStack(Blocks.glass_pane),
            'I', Vars.findItemStack("Iron Cable"));
        addRecipe(Vars.findItemStack("Simple Lamp Socket", 3),
            " I ",
            "GGG",
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Fluorescent Lamp Socket", 3),
            " I ",
            "G G",
            'G', Vars.findItemStack("Iron Cable"),
            'I', new ItemStack(Items.iron_ingot));


        addRecipe(Vars.findItemStack("Suspended Lamp Socket", 2),
            "I",
            "G",
            'G', Vars.findItemStack("Robust Lamp Socket"),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Long Suspended Lamp Socket", 2),
            "I",
            "I",
            "G",
            'G', Vars.findItemStack("Robust Lamp Socket"),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Sconce Lamp Socket", 2),
            "GCG",
            "GIG",
            'G', new ItemStack(Blocks.glass_pane),
            'C', "dustCoal",
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("50V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', Vars.findItemStack("Low Voltage Cable"),
            'b', Vars.findItemStack("Portable Battery Pack"),
            'l', Vars.findItemStack("50V LED Bulb"),
            'g', new ItemStack(Blocks.glass_pane));

        addRecipe(Vars.findItemStack("200V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', Vars.findItemStack("Medium Voltage Cable"),
            'b', Vars.findItemStack("Portable Battery Pack"),
            'l', Vars.findItemStack("200V LED Bulb"),
            'g', new ItemStack(Blocks.glass_pane));
    }

    private void recipeLampSupply() {
        addRecipe(Vars.findItemStack("Lamp Supply", 1),
            " I ",
            "ICI",
            " I ",
            'C', "ingotCopper",
            'I', new ItemStack(Items.iron_ingot));

    }

    private void recipePowerSocket() {
        addRecipe(Vars.findItemStack("50V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', Vars.findItemStack("Copper Plate"),
            'A', Vars.findItemStack("Alloy Plate"),
            'C', Vars.findItemStack("Low Voltage Cable"));
        addRecipe(Vars.findItemStack("200V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', Vars.findItemStack("Copper Plate"),
            'A', Vars.findItemStack("Alloy Plate"),
            'C', Vars.findItemStack("Medium Voltage Cable"));
    }

    private void recipePassiveComponent() {
        addRecipe(Vars.findItemStack("Signal Diode", 4),
            " RB",
            " IR",
            " RB",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'B', "itemRubber");

        addRecipe(Vars.findItemStack("10A Diode", 3),
            " RB",
            "IIR",
            " RB",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'B', "itemRubber");

        addRecipe(Vars.findItemStack("25A Diode"),
            "D",
            "D",
            "D",
            'D', Vars.findItemStack("10A Diode"));


        addRecipe(Vars.findItemStack("Power Capacitor"),
            "cPc",
            "III",
            'I', new ItemStack(Items.iron_ingot),
            'c', Vars.findItemStack("Iron Cable"),
            'P', "plateIron");

        addRecipe(Vars.findItemStack("Power Inductor"),
            "   ",
            "cIc",
            "   ",
            'I', new ItemStack(Items.iron_ingot),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("Power Resistor"),
            "   ",
            "cCc",
            "   ",
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Coal Dust"));

        addRecipe(Vars.findItemStack("Rheostat"),
            " R ",
            " MS",
            "cmc",
            'R', Vars.findItemStack("Power Resistor"),
            'c', Vars.findItemStack("Copper Cable"),
            'm', Vars.findItemStack("Machine Block"),
            'M', Vars.findItemStack("Electrical Motor"),
            'S', Vars.findItemStack("Signal Cable")
        );

        addRecipe(Vars.findItemStack("Thermistor"),
            "   ",
            "csc",
            "   ",
            's', "dustSilicon",
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("Large Rheostat"),
            "   ",
            " D ",
            "CRC",
            'R', Vars.findItemStack("Rheostat"),
            'C', Vars.findItemStack("Copper Thermal Cable"),
            'D', Vars.findItemStack("Small Passive Thermal Dissipator")
        );
    }

    private void recipeSwitch() {
        addRecipe(Vars.findItemStack("Low Voltage Switch"),
            "  I",
            " I ",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("Medium Voltage Switch"),
            "  I",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Medium Voltage Cable"));

        addRecipe(Vars.findItemStack("High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("High Voltage Cable"));

        addRecipe(Vars.findItemStack("Very High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', new ItemStack(Items.redstone),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Very High Voltage Cable"));

    }

    private void recipeElectricalRelay() {
        addRecipe(Vars.findItemStack("Low Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', Vars.findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("Medium Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', Vars.findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Medium Voltage Cable"));

        addRecipe(Vars.findItemStack("High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', Vars.findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("High Voltage Cable"));

        addRecipe(Vars.findItemStack("Very High Voltage Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', Vars.findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'A', "itemRubber",
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Very High Voltage Cable"));

        addRecipe(Vars.findItemStack("Signal Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', new ItemStack(Items.redstone),
            'O', Vars.findItemStack("Iron Cable"),
            'G', new ItemStack(Blocks.glass_pane),
            'I', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Signal Cable"));
    }

    private void recipeWirelessSignal() {
        addRecipe(Vars.findItemStack("Wireless Signal Transmitter"),
            " S ",
            " R ",
            "ICI",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'C', Vars.dictCheapChip,
            'S', Vars.findItemStack("Signal Antenna"));

        addRecipe(Vars.findItemStack("Wireless Signal Repeater"),
            "S S",
            "R R",
            "ICI",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'C', Vars.dictCheapChip,
            'S', Vars.findItemStack("Signal Antenna"));

        addRecipe(Vars.findItemStack("Wireless Signal Receiver"),
            " S ",
            "ICI",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'C', Vars.dictCheapChip,
            'S', Vars.findItemStack("Signal Antenna"));
    }

    private void recipeChips() {
        addRecipe(Vars.findItemStack("NOT Chip"),
            "   ",
            "cCr",
            "   ",
            'C', Vars.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("AND Chip"),
            " c ",
            "cCc",
            " c ",
            'C', Vars.dictCheapChip,
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("NAND Chip"),
            " c ",
            "cCr",
            " c ",
            'C', Vars.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("OR Chip"),
            " r ",
            "rCr",
            " r ",
            'C', Vars.dictCheapChip,
            'r', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("NOR Chip"),
            " r ",
            "rCc",
            " r ",
            'C', Vars.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("XOR Chip"),
            " rr",
            "rCr",
            " rr",
            'C', Vars.dictCheapChip,
            'r', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("XNOR Chip"),
            " rr",
            "rCc",
            " rr",
            'C', Vars.dictCheapChip,
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("PAL Chip"),
            "rcr",
            "cCc",
            "rcr",
            'C', Vars.dictAdvancedChip,
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("Schmitt Trigger Chip"),
            "   ",
            "cCc",
            "   ",
            'C', Vars.dictAdvancedChip,
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("D Flip Flop Chip"),
            "   ",
            "cCc",
            " p ",
            'C', Vars.dictAdvancedChip,
            'p', Vars.findItemStack("Copper Plate"),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("Oscillator Chip"),
            "pdp",
            "cCc",
            "   ",
            'C', Vars.dictAdvancedChip,
            'p', Vars.findItemStack("Copper Plate"),
            'c', Vars.findItemStack("Copper Cable"),
            'd', Vars.findItemStack("Dielectric"));

        addRecipe(Vars.findItemStack("JK Flip Flop Chip"),
            " p ",
            "cCc",
            " p ",
            'C', Vars.dictAdvancedChip,
            'p', Vars.findItemStack("Copper Plate"),
            'c', Vars.findItemStack("Copper Cable"));


        addRecipe(Vars.findItemStack("Amplifier"),
            "  r",
            "cCc",
            "   ",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("OpAmp"),
            "  r",
            "cCc",
            " c ",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("Configurable summing unit"),
            " cr",
            "cCc",
            " c ",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("Sample and hold"),
            " rr",
            "cCc",
            " c ",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("Voltage controlled sine oscillator"),
            "rrr",
            "cCc",
            "   ",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("Voltage controlled sawtooth oscillator"),
            "   ",
            "cCc",
            "rrr",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("PID Regulator"),
            "rrr",
            "cCc",
            "rcr",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("Lowpass filter"),
            "CdC",
            "cDc",
            " s ",
            'd', Vars.findItemStack("Dielectric"),
            'c', Vars.findItemStack("Copper Cable"),
            'C', Vars.findItemStack("Copper Plate"),
            'D', Vars.findItemStack("Coal Dust"),
            's', Vars.dictCheapChip);
    }

    private void recipeTransformer() {
        addRecipe(Vars.findItemStack("DC-DC Converter"),
            "C C",
            "III",
            'C', Vars.findItemStack("Copper Cable"),
            'I', new ItemStack(Items.iron_ingot));
    }

    private void recipeHeatFurnace() {
        addRecipe(Vars.findItemStack("Stone Heat Furnace"),
            "BBB",
            "BIB",
            "BiB",
            'B', new ItemStack(Blocks.stone),
            'i', Vars.findItemStack("Copper Thermal Cable"),
            'I', Vars.findItemStack("Combustion Chamber"));

        addRecipe(Vars.findItemStack("Fuel Heat Furnace"),
            "IcI",
            "mCI",
            "IiI",
            'c', Vars.findItemStack("Cheap Chip"),
            'm', Vars.findItemStack("Electrical Motor"),
            'C', new ItemStack(Items.cauldron),
            'I', new ItemStack(Items.iron_ingot),
            'i', Vars.findItemStack("Copper Thermal Cable"));
    }

    private void recipeTurbine() {
        addRecipe(Vars.findItemStack("50V Turbine"),
            " m ",
            "HMH",
            " E ",
            'M', Vars.findItemStack("Machine Block"),
            'E', Vars.findItemStack("Low Voltage Cable"),
            'H', Vars.findItemStack("Copper Thermal Cable"),
            'm', Vars.findItemStack("Electrical Motor")

        );
        addRecipe(Vars.findItemStack("200V Turbine"),
            "ImI",
            "HMH",
            "IEI",
            'I', "itemRubber",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'E', Vars.findItemStack("Medium Voltage Cable"),
            'H', Vars.findItemStack("Copper Thermal Cable"),
            'm', Vars.findItemStack("Advanced Electrical Motor"));
        addRecipe(Vars.findItemStack("Generator"),
            "mmm",
            "ama",
            " ME",
            'm', Vars.findItemStack("Advanced Electrical Motor"),
            'M', Vars.findItemStack("Advanced Machine Block"),
            'a', Vars.firstExistingOre("ingotAluminum", "ingotIron"),
            'E', Vars.findItemStack("High Voltage Cable")
        );
        addRecipe(Vars.findItemStack("Shaft Motor"),
            "imi",
            " ME",
            'i', "ingotIron",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'm', Vars.findItemStack("Advanced Electrical Motor"),
            'E', Vars.findItemStack("Very High Voltage Cable")
        );
        addRecipe(Vars.findItemStack("Steam Turbine"),
            " a ",
            "aAa",
            " M ",
            'a', Vars.firstExistingOre("ingotAluminum", "ingotIron"),
            'A', Vars.firstExistingOre("blockAluminum", "blockIron"),
            'M', Vars.findItemStack("Advanced Machine Block")
        );
        addRecipe(Vars.findItemStack("Gas Turbine"),
            "msH",
            "sSs",
            " M ",
            'm', Vars.findItemStack("Advanced Electrical Motor"),
            'H', Vars.findItemStack("Copper Thermal Cable"),
            's', Vars.firstExistingOre("ingotSteel", "ingotIron"),
            'S', Vars.firstExistingOre("blockSteel", "blockIron"),
            'M', Vars.findItemStack("Advanced Machine Block")
        );

        addRecipe(Vars.findItemStack("Joint"),
            "   ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', Vars.findItemStack("Machine Block")
        );

        addRecipe(Vars.findItemStack("Joint hub"),
            " i ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', Vars.findItemStack("Machine Block")
        );

        addRecipe(Vars.findItemStack("Flywheel"),
            "PPP",
            "PmP",
            "PPP",
            'P', "ingotLead",
            'm', Vars.findItemStack("Machine Block")
        );

        addRecipe(Vars.findItemStack("Tachometer"),
            "p  ",
            "iii",
            "cm ",
            'i', "ingotIron",
            'm', Vars.findItemStack("Machine Block"),
            'p', Vars.findItemStack("Electrical Probe Chip"),
            'c', Vars.findItemStack("Signal Cable")
        );
        addRecipe(Vars.findItemStack("Clutch"),
            "iIi",
            " c ",
            'i', "ingotIron",
            'I', "plateIron",
            'c', Vars.findItemStack("Machine Block")
        );
        addRecipe(Vars.findItemStack("Fixed Shaft"),
            "iBi",
            " c ",
            'i', "ingotIron",
            'B', "blockIron",
            'c', Vars.findItemStack("Machine Block")
        );
    }

    private void recipeBattery() {
        addRecipe(Vars.findItemStack("Cost Oriented Battery"),
            "C C",
            "PPP",
            "PPP",
            'C', Vars.findItemStack("Low Voltage Cable"),
            'P', "ingotLead",
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Capacity Oriented Battery"),
            "PBP",
            'B', Vars.findItemStack("Cost Oriented Battery"),
            'P', "ingotLead");

        addRecipe(Vars.findItemStack("Voltage Oriented Battery"),
            "PBP",
            'B', Vars.findItemStack("Cost Oriented Battery"),
            'P', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Current Oriented Battery"),
            "PBP",
            'B', Vars.findItemStack("Cost Oriented Battery"),
            'P', "ingotCopper");

        addRecipe(Vars.findItemStack("Life Oriented Battery"),
            "PBP",
            'B', Vars.findItemStack("Cost Oriented Battery"),
            'P', new ItemStack(Items.gold_ingot));
        addRecipe(Vars.findItemStack("Experimental Battery"),
            " S ",
            "LDV",
            " C ",
            'S', Vars.findItemStack("Capacity Oriented Battery"),
            'L', Vars.findItemStack("Life Oriented Battery"),
            'V', Vars.findItemStack("Voltage Oriented Battery"),
            'C', Vars.findItemStack("Current Oriented Battery"),
            'D', new ItemStack(Items.diamond));

        addRecipe(Vars.findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', Vars.findItemStack("Low Voltage Cable"),
            'p', new ItemStack(Items.coal, 1, 0),
            'I', "ingotCopper");

        addRecipe(Vars.findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', Vars.findItemStack("Low Voltage Cable"),
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
                addRecipe(Vars.findItemStack("Utility Pole"),
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
            addRecipe(Vars.findItemStack("Utility Pole"),
                "WWW",
                "IWI",
                " W ",
                'I', "ingotIron",
                'W', "logWood"
            );
        }
        addRecipe(Vars.findItemStack("Utility Pole w/DC-DC Converter"),
            "HHH",
            " TC",
            " PH",
            'P', Vars.findItemStack("Utility Pole"),
            'H', Vars.findItemStack("High Voltage Cable"),
            'C', Vars.findItemStack("Optimal Ferromagnetic Core"),
            'T', Vars.findItemStack("DC-DC Converter")
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
                addRecipe(Vars.findItemStack("Transmission Tower"),
                    "ii ",
                    "mi ",
                    " B ",
                    'i', ingotType,
                    'B', blockType,
                    'm', Vars.findItemStack("Machine Block"));
                addRecipe(Vars.findItemStack("Grid DC-DC Converter"),
                    "i i",
                    "mtm",
                    "imi",
                    'i', ingotType,
                    't', Vars.findItemStack("DC-DC Converter"),
                    'm', Vars.findItemStack("Advanced Machine Block"));
            }
        }

//		if (oreNames.contains("sheetPlastic")) {
//			addRecipe(Vars.findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "sheetPlastic",
//					'M', Vars.findItemStack("Machine Block"),
//					'H', Vars.findItemStack("High Voltage Cable")
//			);
//		} else {
//			addRecipe(Vars.findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "itemRubber",
//					'M', Vars.findItemStack("Machine Block"),
//					'H', Vars.findItemStack("High Voltage Cable")
//			);
//		}
    }


    private void recipeElectricalFurnace() {
        addRecipe(Vars.findItemStack("Electrical Furnace"),
            "III",
            "IFI",
            "ICI",
            'C', Vars.findItemStack("Low Voltage Cable"),
            'F', new ItemStack(Blocks.furnace),
            'I', new ItemStack(Items.iron_ingot));
        addShapelessRecipe(Vars.findItemStack("Canister of Water", 1),
            Vars.findItemStack("Inert Canister"),
            new ItemStack(Items.water_bucket));

    }

    private void recipeSixNodeMisc() {
        addRecipe(Vars.findItemStack("Analog Watch"),
            "crc",
            "III",
            'c', Vars.findItemStack("Iron Cable"),
            'r', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Digital Watch"),
            "rcr",
            "III",
            'c', Vars.findItemStack("Iron Cable"),
            'r', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Hub"),
            "I I",
            " c ",
            "I I",
            'c', Vars.findItemStack("Copper Cable"),
            'I', Vars.findItemStack("Iron Cable"));


        addRecipe(Vars.findItemStack("Energy Meter"),
            "IcI",
            "IRI",
            "IcI",
            'c', Vars.findItemStack("Copper Cable"),
            'R', Vars.dictCheapChip,
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Advanced Energy Meter"),
            " c ",
            "PRP",
            " c ",
            'c', Vars.findItemStack("Copper Cable"),
            'R', Vars.dictAdvancedChip,
            'P', Vars.findItemStack("Iron Plate"));
    }

    private void recipeAutoMiner() {
        addRecipe(Vars.findItemStack("Auto Miner"),
            "MCM",
            "BOB",
            " P ",
            'C', Vars.dictAdvancedChip,
            'O', Vars.findItemStack("Ore Scanner"),
            'B', Vars.findItemStack("Advanced Machine Block"),
            'M', Vars.findItemStack("Advanced Electrical Motor"),
            'P', Vars.findItemStack("Mining Pipe"));
    }

    private void recipeWindTurbine() {
        addRecipe(Vars.findItemStack("Wind Turbine"),
            " I ",
            "IMI",
            " B ",
            'B', Vars.findItemStack("Machine Block"),
            'I', "plateIron",
            'M', Vars.findItemStack("Electrical Motor"));

        /*addRecipe(Vars.findItemStack("Large Wind Turbine"), //todo add recipe to large wind turbine
            "TTT",
            "TCT",
            "TTT",
            'T', Vars.findItemStack("Wind Turbine"),
            'C', Vars.findItemStack("Advanced Machine Block")); */

        addRecipe(Vars.findItemStack("Water Turbine"),
            "  I",
            "BMI",
            "  I",
            'I', "plateIron",
            'B', Vars.findItemStack("Machine Block"),
            'M', Vars.findItemStack("Electrical Motor"));

    }

    private void recipeFuelGenerator() {
        addRecipe(Vars.findItemStack("50V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', Vars.findItemStack("Machine Block"),
            'A', Vars.findItemStack("Analogic Regulator"),
            'C', Vars.findItemStack("Low Voltage Cable"),
            'M', Vars.findItemStack("Electrical Motor"));

        addRecipe(Vars.findItemStack("200V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', Vars.findItemStack("Advanced Machine Block"),
            'A', Vars.findItemStack("Analogic Regulator"),
            'C', Vars.findItemStack("Medium Voltage Cable"),
            'M', Vars.findItemStack("Advanced Electrical Motor"));
    }

    private void recipeSolarPanel() {
        addRecipe(Vars.findItemStack("Small Solar Panel"),
            "LLL",
            "CSC",
            "III",
            'S', "plateSilicon",
            'L', Vars.findItemStack("Lapis Dust"),
            'I', new ItemStack(Items.iron_ingot),
            'C', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("Small Rotating Solar Panel"),
            "ISI",
            "I I",
            'S', Vars.findItemStack("Small Solar Panel"),
            'M', Vars.findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.iron_ingot));

        for (String metal : new String[]{"blockSteel", "blockAluminum", "blockAluminium", "casingMachineAdvanced"}) {
            for (String panel : new String[]{"Small Solar Panel", "Small Rotating Solar Panel"}) {
                addRecipe(Vars.findItemStack("2x3 Solar Panel"),
                    "PPP",
                    "PPP",
                    "I I",
                    'P', Vars.findItemStack(panel),
                    'I', metal);
            }
        }
        addRecipe(Vars.findItemStack("2x3 Rotating Solar Panel"),
            "ISI",
            "IMI",
            "I I",
            'S', Vars.findItemStack("2x3 Solar Panel"),
            'M', Vars.findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.iron_ingot));
    }

    private void recipeThermalDissipatorPassiveAndActive() {
        addRecipe(
            Vars.findItemStack("Small Passive Thermal Dissipator"),
            "I I",
            "III",
            "CIC",
            'I', "ingotCopper",
            'C', Vars.findItemStack("Copper Thermal Cable"));

        addRecipe(
            Vars.findItemStack("Small Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', Vars.findItemStack("Small Passive Thermal Dissipator"),
            'M', Vars.findItemStack("Electrical Motor"),
            'R', "itemRubber");

        addRecipe(
            Vars.findItemStack("200V Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', Vars.findItemStack("Small Passive Thermal Dissipator"),
            'M', Vars.findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber");

    }

    private void recipeGeneral() {
        Utils.addSmelting(Vars.treeResin.parentItem,
            Vars.treeResin.parentItemDamage, Vars.findItemStack("Rubber", 1), 0f);

    }

    private void recipeHeatingCorp() {
        addRecipe(Vars.findItemStack("Small 50V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("50V Copper Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 50V Copper Heating Corp"));

        addRecipe(Vars.findItemStack("Small 200V Copper Heating Corp"),
            "CC",
            'C', Vars.findItemStack("50V Copper Heating Corp"));

        addRecipe(Vars.findItemStack("200V Copper Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 200V Copper Heating Corp"));

        addRecipe(Vars.findItemStack("Small 50V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C", 'C', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("50V Iron Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 50V Iron Heating Corp"));

        addRecipe(Vars.findItemStack("Small 200V Iron Heating Corp"),
            "CC",
            'C', Vars.findItemStack("50V Iron Heating Corp"));

        addRecipe(Vars.findItemStack("200V Iron Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 200V Iron Heating Corp"));

        addRecipe(Vars.findItemStack("Small 50V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', Vars.findItemStack("Tungsten Cable"));

        addRecipe(Vars.findItemStack("50V Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 50V Tungsten Heating Corp"));

        addRecipe(Vars.findItemStack("Small 200V Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("50V Tungsten Heating Corp"));
        addRecipe(Vars.findItemStack("200V Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 200V Tungsten Heating Corp"));
        addRecipe(Vars.findItemStack("Small 800V Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("200V Tungsten Heating Corp"));
        addRecipe(Vars.findItemStack("800V Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 800V Tungsten Heating Corp"));
        addRecipe(Vars.findItemStack("Small 3.2kV Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("800V Tungsten Heating Corp"));
        addRecipe(Vars.findItemStack("3.2kV Tungsten Heating Corp"),
            "CC",
            'C', Vars.findItemStack("Small 3.2kV Tungsten Heating Corp"));
    }

    private void recipeRegulatorItem() {
        addRecipe(Vars.findItemStack("On/OFF Regulator 10 Percent", 1),
            "R R",
            " R ",
            " I ",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("On/OFF Regulator 1 Percent", 1),
            "RRR",
            " I ",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Analogic Regulator", 1),
            "R R",
            " C ",
            " I ",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'C', Vars.dictCheapChip);
    }

    private void recipeLampItem() {
        // Tungsten
        addRecipe(
            Vars.findItemStack("Small 50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Vars.dictTungstenIngot,
            'S', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("50V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Vars.dictTungstenIngot,
            'S', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("200V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Vars.dictTungstenIngot,
            'S', Vars.findItemStack("Medium Voltage Cable"));

        // CARBON
        addRecipe(Vars.findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal),
            'S', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("Small 50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal, 1, 1),
            'S', Vars.findItemStack("Copper Cable"));

        addRecipe(
            Vars.findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal),
            'S', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("50V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.coal, 1, 1),
            'S', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(
            Vars.findItemStack("Small 50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.glowstone_dust),
            'S', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("50V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.glowstone_dust),
            'S', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("200V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', new ItemStack(Blocks.glass_pane),
            'F', new ItemStack(Items.glowstone_dust),
            'S', Vars.findItemStack("Medium Voltage Cable"));

        addRecipe(Vars.findItemStack("50V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Vars.dictTungstenIngot,
            'S', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("200V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', new ItemStack(Blocks.glass_pane),
            'F', Vars.dictTungstenIngot,
            'S', Vars.findItemStack("Medium Voltage Cable"));

        addRecipe(Vars.findItemStack("50V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', new ItemStack(Blocks.glass_pane),
            'S', Vars.findItemStack("Silicon Ingot"),
            'C', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("200V LED Bulb", 2),
            "GGG",
            "SSS",
            " C ",
            'G', new ItemStack(Blocks.glass_pane),
            'S', Vars.findItemStack("Silicon Ingot"),
            'C', Vars.findItemStack("Medium Voltage Cable"));

    }

    private void recipeProtection() {
        addRecipe(Vars.findItemStack("Overvoltage Protection", 4),
            "SCD",
            'S', Vars.findItemStack("Electrical Probe Chip"),
            'C', Vars.dictCheapChip,
            'D', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Overheating Protection", 4),
            "SCD",
            'S', Vars.findItemStack("Thermal Probe Chip"),
            'C', Vars.dictCheapChip,
            'D', new ItemStack(Items.redstone));

    }

    private void recipeCombustionChamber() {
        addRecipe(Vars.findItemStack("Combustion Chamber"),
            " L ",
            "L L",
            " L ",
            'L', new ItemStack(Blocks.stone));
    }

    private void recipeFerromagneticCore() {
        addRecipe(Vars.findItemStack("Cheap Ferromagnetic Core"),
            "LLL",
            "L  ",
            "LLL",
            'L', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Average Ferromagnetic Core"),
            "PCP",
            'C', Vars.findItemStack("Cheap Ferromagnetic Core"),
            'P', "plateIron");

        addRecipe(Vars.findItemStack("Optimal Ferromagnetic Core"),
            " P ",
            "PCP",
            " P ",
            'C', Vars.findItemStack("Average Ferromagnetic Core"),
            'P', "plateIron");
    }

    private void recipeIngot() {
        // Done
    }

    private void recipeDust() {
        addShapelessRecipe(Vars.findItemStack("Alloy Dust", 2),
            "dustIron",
            "dustCoal",
            Vars.dictTungstenDust,
            Vars.dictTungstenDust,
            Vars.dictTungstenDust,
            Vars.dictTungstenDust);
        addShapelessRecipe(Vars.findItemStack("Inert Canister", 1),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Diamond Dust"),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Lapis Dust"),
            Vars.findItemStack("Lapis Dust"));


    }

    private void recipeElectricalMotor() {
        addRecipe(Vars.findItemStack("Electrical Motor"),
            " C ",
            "III",
            "C C",
            'I', Vars.findItemStack("Iron Cable"),
            'C', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("Advanced Electrical Motor"),
            "RCR",
            "MIM",
            "CRC",
            'M', Vars.findItemStack("Advanced Magnet"),
            'I', new ItemStack(Items.iron_ingot),
            'R', new ItemStack(Items.redstone),
            'C', Vars.findItemStack("Medium Voltage Cable"));
    }

    private void recipeSolarTracker() {
        addRecipe(Vars.findItemStack("Solar Tracker", 4),
            "VVV",
            "RQR",
            "III",
            'Q', new ItemStack(Items.quartz),
            'V', new ItemStack(Blocks.glass_pane),
            'R', new ItemStack(Items.redstone),
            'G', new ItemStack(Items.gold_ingot),
            'I', new ItemStack(Items.iron_ingot));

    }

    private void recipeDynamo() {

    }

    private void recipeWindRotor() {

    }

    private void recipeMeter() {
        addRecipe(Vars.findItemStack("MultiMeter"),
            "RGR",
            "RER",
            "RCR",
            'G', new ItemStack(Blocks.glass_pane),
            'C', Vars.findItemStack("Electrical Probe Chip"),
            'E', new ItemStack(Items.redstone),
            'R', "itemRubber");

        addRecipe(Vars.findItemStack("Thermometer"),
            "RGR",
            "RER",
            "RCR",
            'G', new ItemStack(Blocks.glass_pane),
            'C', Vars.findItemStack("Thermal Probe Chip"),
            'E', new ItemStack(Items.redstone),
            'R', "itemRubber");

        addShapelessRecipe(Vars.findItemStack("AllMeter"),
            Vars.findItemStack("MultiMeter"),
            Vars.findItemStack("Thermometer"));

        addRecipe(Vars.findItemStack("Wireless Analyser"),
            " S ",
            "RGR",
            "RER",
            'G', new ItemStack(Blocks.glass_pane),
            'S', Vars.findItemStack("Signal Antenna"),
            'E', new ItemStack(Items.redstone),
            'R', "itemRubber");
        addRecipe(Vars.findItemStack("Config Copy Tool"),
            "wR",
            "RC",
            'w', Vars.findItemStack("Wrench"),
            'R', new ItemStack(Items.redstone),
            'C', Vars.dictAdvancedChip
        );

    }

    private void recipeElectricalDrill() {
        addRecipe(Vars.findItemStack("Cheap Electrical Drill"),
            "CMC",
            " T ",
            " P ",
            'T', Vars.findItemStack("Mining Pipe"),
            'C', Vars.dictCheapChip,
            'M', Vars.findItemStack("Electrical Motor"),
            'P', new ItemStack(Items.iron_pickaxe));

        addRecipe(Vars.findItemStack("Average Electrical Drill"),
            "RCR",
            " D ",
            " d ",
            'R', Items.redstone,
            'C', Vars.dictCheapChip,
            'D', Vars.findItemStack("Cheap Electrical Drill"),
            'd', new ItemStack(Items.diamond));

        addRecipe(Vars.findItemStack("Fast Electrical Drill"),
            "MCM",
            " T ",
            " P ",
            'T', Vars.findItemStack("Mining Pipe"),
            'C', Vars.dictAdvancedChip,
            'M', Vars.findItemStack("Advanced Electrical Motor"),
            'P', new ItemStack(Items.diamond_pickaxe));
        addRecipe(Vars.findItemStack("Turbo Electrical Drill"),
            "RCR",
            " F ",
            " D ",
            'F', Vars.findItemStack("Fast Electrical Drill"),
            'C', Vars.dictAdvancedChip,
            'R', Vars.findItemStack("Graphite Rod"),
            'D', Vars.findItemStack("Synthetic Diamond"));
        addRecipe(Vars.findItemStack("Irresponsible Electrical Drill"),
            "DDD",
            "DFD",
            "DDD",
            'F', Vars.findItemStack("Turbo Electrical Drill"),
            'D', Vars.findItemStack("Synthetic Diamond"));
    }

    private void recipeOreScanner() {
        addRecipe(Vars.findItemStack("Ore Scanner"),
            "IGI",
            "RCR",
            "IGI",
            'C', Vars.dictCheapChip,
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"),
            'G', new ItemStack(Items.gold_ingot));

    }

    private void recipeMiningPipe() {
        addRecipe(Vars.findItemStack("Mining Pipe", 4),
            "A",
            "A",
            'A', "ingotAlloy");
    }

    private void recipeTreeResinAndRubber() {
        addRecipe(Vars.findItemStack("Tree Resin Collector"),
            "W W",
            "WW ", 'W', "plankWood");

        addRecipe(Vars.findItemStack("Tree Resin Collector"),
            "W W",
            " WW", 'W', "plankWood");

    }

    private void recipeRawCable() {
        addRecipe(Vars.findItemStack("Copper Cable", 12),
            "III",
            'I', "ingotCopper");

        addRecipe(Vars.findItemStack("Iron Cable", 12),
            "III",
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Tungsten Cable", 6),
            "III",
            'I', Vars.dictTungstenIngot);
    }

    private void recipeGraphite() {
        addRecipe(Vars.findItemStack("Creative Cable", 1),
            "I",
            "S",
            'S', Vars.findItemStack("unreleasedium"),
            'I', Vars.findItemStack("Synthetic Diamond"));
        addRecipe(new ItemStack(Vars.arcClayBlock),
            "III",
            "III",
            "III",
            'I', Vars.findItemStack("Arc Clay Ingot"));
        addRecipe(Vars.findItemStack("Arc Clay Ingot", 9),
            "I",
            'I', new ItemStack(Vars.arcClayBlock));
        addRecipe(new ItemStack(Vars.arcMetalBlock),
            "III",
            "III",
            "III",
            'I', Vars.findItemStack("Arc Metal Ingot"));
        addRecipe(Vars.findItemStack("Arc Metal Ingot", 9),
            "I",
            'I', new ItemStack(Vars.arcMetalBlock));
        addRecipe(Vars.findItemStack("Graphite Rod", 2),
            "I",
            'I', Vars.findItemStack("2x Graphite Rods"));
        addRecipe(Vars.findItemStack("Graphite Rod", 3),
            "I",
            'I', Vars.findItemStack("3x Graphite Rods"));
        addRecipe(Vars.findItemStack("Graphite Rod", 4),
            "I",
            'I', Vars.findItemStack("4x Graphite Rods"));
        addShapelessRecipe(
            Vars.findItemStack("2x Graphite Rods"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"));
        addShapelessRecipe(
            Vars.findItemStack("3x Graphite Rods"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"));
        addShapelessRecipe(
            Vars.findItemStack("3x Graphite Rods"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("2x Graphite Rods"));
        addShapelessRecipe(
            Vars.findItemStack("4x Graphite Rods"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"));
        addShapelessRecipe(
            Vars.findItemStack("4x Graphite Rods"),
            Vars.findItemStack("2x Graphite Rods"),
            Vars.findItemStack("Graphite Rod"),
            Vars.findItemStack("Graphite Rod"));
        addShapelessRecipe(
            Vars.findItemStack("4x Graphite Rods"),
            Vars.findItemStack("2x Graphite Rods"),
            Vars.findItemStack("2x Graphite Rods"));
        addShapelessRecipe(
            Vars.findItemStack("4x Graphite Rods"),
            Vars.findItemStack("3x Graphite Rods"),
            Vars.findItemStack("Graphite Rod"));
        addShapelessRecipe(
            new ItemStack(Items.diamond, 2),
            Vars.findItemStack("Synthetic Diamond"));
    }

    private void recipeBatteryItem() {
        addRecipe(Vars.findItemStack("Portable Battery"),
            " I ",
            "IPI",
            "IPI",
            'P', "ingotLead",
            'I', new ItemStack(Items.iron_ingot));
        addShapelessRecipe(
            Vars.findItemStack("Portable Battery Pack"),
            Vars.findItemStack("Portable Battery"),
            Vars.findItemStack("Portable Battery"),
            Vars.findItemStack("Portable Battery"),
            Vars.findItemStack("Portable Battery"));
    }

    private void recipeElectricalTool() {
        addRecipe(Vars.findItemStack("Small Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', Vars.findItemStack("50V Incandescent Light Bulb"),
            'B', Vars.findItemStack("Portable Battery"),
            'G', new ItemStack(Blocks.glass_pane),
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Portable Electrical Mining Drill"),
            " T ",
            "IBI",
            " I ",
            'T', Vars.findItemStack("Average Electrical Drill"),
            'B', Vars.findItemStack("Portable Battery"),
            'I', new ItemStack(Items.iron_ingot));

        addRecipe(Vars.findItemStack("Portable Electrical Axe"),
            " T ",
            "IMI",
            "IBI",
            'T', new ItemStack(Items.iron_axe),
            'B', Vars.findItemStack("Portable Battery"),
            'M', Vars.findItemStack("Electrical Motor"),
            'I', new ItemStack(Items.iron_ingot));

        if (Vars.xRayScannerCanBeCrafted) {
            addRecipe(Vars.findItemStack("X-Ray Scanner"),
                "PGP",
                "PCP",
                "PBP",
                'C', Vars.dictAdvancedChip,
                'B', Vars.findItemStack("Portable Battery"),
                'P', Vars.findItemStack("Iron Cable"),
                'G', Vars.findItemStack("Ore Scanner"));
        }

    }

    private void recipeECoal() {
        addRecipe(Vars.findItemStack("E-Coal Helmet"),
            "PPP",
            "PCP",
            'P', "plateCoal",
            'C', Vars.findItemStack("Portable Condensator"));
        addRecipe(Vars.findItemStack("E-Coal Boots"),
            " C ",
            "P P",
            "P P",
            'P', "plateCoal",
            'C', Vars.findItemStack("Portable Condensator"));

        addRecipe(Vars.findItemStack("E-Coal Chestplate"),
            "P P",
            "PCP",
            "PPP",
            'P', "plateCoal",
            'C', Vars.findItemStack("Portable Condensator"));

        addRecipe(Vars.findItemStack("E-Coal Leggings"),
            "PPP",
            "PCP",
            "P P",
            'P', "plateCoal",
            'C', Vars.findItemStack("Portable Condensator"));

    }

    private void recipePortableCapacitor() {
        addRecipe(Vars.findItemStack("Portable Condensator"),
            " r ",
            "cDc",
            " r ",
            'r', new ItemStack(Items.redstone),
            'c', Vars.findItemStack("Iron Cable"),
            'D', Vars.findItemStack("Dielectric"));

        addShapelessRecipe(Vars.findItemStack("Portable Condensator Pack"),
            Vars.findItemStack("Portable Condensator"),
            Vars.findItemStack("Portable Condensator"),
            Vars.findItemStack("Portable Condensator"),
            Vars.findItemStack("Portable Condensator"));
    }

    private void recipeMiscItem() {
        addRecipe(Vars.findItemStack("Cheap Chip"),
            " R ",
            "RSR",
            " R ",
            'S', "ingotSilicon",
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("Advanced Chip"),
            "LRL",
            "RCR",
            "LRL",
            'C', Vars.dictCheapChip,
            'L', "ingotSilicon",
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Machine Block"),
            "rLr",
            "LcL",
            "rLr",
            'L', Vars.findItemStack("Iron Cable"),
            'c', Vars.findItemStack("Copper Cable"),
            'r', Vars.findItemStack("Tree Resin")
        );

        addRecipe(Vars.findItemStack("Advanced Machine Block"),
            "rCr",
            "CcC",
            "rCr",
            'C', "plateAlloy",
            'r', Vars.findItemStack("Tree Resin"),
            'c', Vars.findItemStack("Copper Cable"));

        addRecipe(Vars.findItemStack("Electrical Probe Chip"),
            " R ",
            "RCR",
            " R ",
            'C', Vars.findItemStack("High Voltage Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Thermal Probe Chip"),
            " C ",
            "RIR",
            " C ",
            'G', new ItemStack(Items.gold_ingot),
            'I', Vars.findItemStack("Iron Cable"),
            'C', "ingotCopper",
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Signal Antenna"),
            "c",
            "c",
            'c', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Machine Booster"),
            "m",
            "c",
            "m",
            'm', Vars.findItemStack("Electrical Motor"),
            'c', Vars.dictAdvancedChip);

        addRecipe(Vars.findItemStack("Wrench"),
            " c ",
            "cc ",
            "  c",
            'c', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Player Filter"),
            " g",
            "gc",
            " g",
            'g', new ItemStack(Blocks.glass_pane),
            'c', new ItemStack(Items.dye, 1, 2));

        addRecipe(Vars.findItemStack("Monster Filter"),
            " g",
            "gc",
            " g",
            'g', new ItemStack(Blocks.glass_pane),
            'c', new ItemStack(Items.dye, 1, 1));

        addRecipe(Vars.findItemStack("Casing", 1),
            "ppp",
            "p p",
            "ppp",
            'p', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Iron Clutch Plate"),
            " t ",
            "tIt",
            " t ",
            'I', "plateIron",
            't', Vars.dictTungstenDust
        );

        addRecipe(Vars.findItemStack("Gold Clutch Plate"),
            " t ",
            "tGt",
            " t ",
            'G', "plateGold",
            't', Vars.dictTungstenDust
        );

        addRecipe(Vars.findItemStack("Copper Clutch Plate"),
            " t ",
            "tCt",
            " t ",
            'C', "plateCopper",
            't', Vars.dictTungstenDust
        );

        addRecipe(Vars.findItemStack("Lead Clutch Plate"),
            " t ",
            "tLt",
            " t ",
            'L', "plateLead",
            't', Vars.dictTungstenDust
        );

        addRecipe(Vars.findItemStack("Coal Clutch Plate"),
            " t ",
            "tCt",
            " t ",
            'C', "plateCoal",
            't', Vars.dictTungstenDust
        );

        addRecipe(Vars.findItemStack("Clutch Pin", 4),
            "s",
            "s",
            's', Vars.firstExistingOre("ingotSteel", "ingotAlloy")
        );

    }

    private void recipeMacerator() {
        float f = 4000;
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.coal_ore, 1),
            new ItemStack(Items.coal, 3, 0), 1.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Copper Ore"),
            new ItemStack[]{Vars.findItemStack("Copper Dust", 2)}, 1.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.iron_ore),
            new ItemStack[]{Vars.findItemStack("Iron Dust", 2)}, 1.5 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gold_ore),
            new ItemStack[]{Vars.findItemStack("Gold Dust", 2)}, 3.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Lead Ore"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 2)}, 2.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Tungsten Ore"),
            new ItemStack[]{Vars.findItemStack("Tungsten Dust", 2)}, 2.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.coal, 1, 0),
            new ItemStack[]{Vars.findItemStack("Coal Dust", 1)}, 1.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.coal, 1, 1),
            new ItemStack[]{Vars.findItemStack("Coal Dust", 1)}, 1.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.sand, 1),
            new ItemStack[]{Vars.findItemStack("Silicon Dust", 1)}, 3.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Cinnabar Ore"),
            new ItemStack[]{Vars.findItemStack("Cinnabar Dust", 1)}, 2.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.dye, 1, 4),
            new ItemStack[]{Vars.findItemStack("Lapis Dust", 1)}, 2.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.diamond, 1),
            new ItemStack[]{Vars.findItemStack("Diamond Dust", 1)}, 2.0 * f));

        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Copper Ingot"),
            new ItemStack[]{Vars.findItemStack("Copper Dust", 1)}, 0.5 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot),
            new ItemStack[]{Vars.findItemStack("Iron Dust", 1)}, 0.5 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Items.gold_ingot),
            new ItemStack[]{Vars.findItemStack("Gold Dust", 1)}, 0.5 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Lead Ingot"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 1)}, 0.5 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Tungsten Ingot"),
            new ItemStack[]{Vars.findItemStack("Tungsten Dust", 1)}, 0.5 * f));

        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.cobblestone),
            new ItemStack[]{new ItemStack(Blocks.gravel)}, 1.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gravel),
            new ItemStack[]{new ItemStack(Items.flint)}, 1.0 * f));

        Vars.maceratorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.dirt),
            new ItemStack[]{new ItemStack(Blocks.sand)}, 1.0 * f));
        //recycling recipes
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("E-Coal Helmet"),
            new ItemStack[]{Vars.findItemStack("Coal Dust", 16)}, 10.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("E-Coal Boots"),
            new ItemStack[]{Vars.findItemStack("Coal Dust", 12)}, 10.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("E-Coal Chestplate"),
            new ItemStack[]{Vars.findItemStack("Coal Dust", 24)}, 10.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("E-Coal Leggings"),
            new ItemStack[]{Vars.findItemStack("Coal Dust", 24)}, 10.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Cost Oriented Battery"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 6)}, 50.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Life Oriented Battery"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 6)}, 50.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Current Oriented Battery"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 6)}, 50.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Voltage Oriented Battery"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 6)}, 50.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Capacity Oriented Battery"),
            new ItemStack[]{Vars.findItemStack("Lead Dust", 6)}, 50.0 * f));
        Vars.maceratorRecipes.addRecipe(new Recipe(Vars.findItemStack("Single-use Battery"),
            new ItemStack[]{Vars.findItemStack("Copper Dust", 3)}, 10.0 * f));

        //end recycling recipes
    }

    private void recipeArcFurnace() {
        float f = 200000;
        float smeltf = 5000;
        //start smelting recipes
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.iron_ore, 1),
            new ItemStack[]{new ItemStack(Items.iron_ingot, 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.gold_ore, 1),
            new ItemStack[]{new ItemStack(Items.gold_ingot, 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.coal_ore, 1),
            new ItemStack[]{new ItemStack(Items.coal, 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.redstone_ore, 1),
            new ItemStack[]{new ItemStack(Items.redstone, 6)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.lapis_ore, 1),
            new ItemStack[]{new ItemStack(Blocks.lapis_block, 1)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.diamond_ore, 1),
            new ItemStack[]{new ItemStack(Items.diamond, 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.emerald_ore, 1),
            new ItemStack[]{new ItemStack(Items.emerald, 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Blocks.quartz_ore, 1),
            new ItemStack[]{new ItemStack(Items.quartz, 2)}, smeltf));

        Vars.arcFurnaceRecipes.addRecipe(new Recipe(Vars.findItemStack("Copper Ore", 1),
            new ItemStack[]{Vars.findItemStack("Copper Ingot", 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(Vars.findItemStack("Lead Ore", 1),
            new ItemStack[]{Vars.findItemStack("Lead Ingot", 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(Vars.findItemStack("Tungsten Ore", 1),
            new ItemStack[]{Vars.findItemStack("Tungsten Ingot", 2)}, smeltf));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(Vars.findItemStack("Alloy Dust", 1),
            new ItemStack[]{Vars.findItemStack("Alloy Ingot", 1)}, smeltf));
        //end smelting recipes
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Items.clay_ball, 2),
            new ItemStack[]{Vars.findItemStack("Arc Clay Ingot", 1)}, 2.0 * f));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, 1),
            new ItemStack[]{Vars.findItemStack("Arc Metal Ingot", 1)}, 1.0 * f));
        Vars.arcFurnaceRecipes.addRecipe(new Recipe(Vars.findItemStack("Canister of Water", 1),
            new ItemStack[]{Vars.findItemStack("Canister of Arc Water", 1)}, 7000000)); //hardcoded 7MJ to prevent overunity
    }

    private void recipeMaceratorModOre(float f, String inputName, String outputName, int outputCount) {
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
            Vars.maceratorRecipes.addRecipe(new Recipe(input, output, f));
        }
    }

    private void recipePlateMachine() {
        float f = 10000;
        Vars.plateMachineRecipes.addRecipe(new Recipe(
            Vars.findItemStack("Copper Ingot", Vars.plateConversionRatio),
            Vars.findItemStack("Copper Plate"), 1.0 * f));

        Vars.plateMachineRecipes.addRecipe(new Recipe(Vars.findItemStack("Lead Ingot", Vars.plateConversionRatio),
            Vars.findItemStack("Lead Plate"), 1.0 * f));

        Vars.plateMachineRecipes.addRecipe(new Recipe(
            Vars.findItemStack("Silicon Ingot", 4),
            Vars.findItemStack("Silicon Plate"), 1.0 * f));

        Vars.plateMachineRecipes.addRecipe(new Recipe(Vars.findItemStack("Alloy Ingot", Vars.plateConversionRatio),
            Vars.findItemStack("Alloy Plate"), 1.0 * f));

        Vars.plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, Vars.plateConversionRatio,
            0), Vars.findItemStack("Iron Plate"), 1.0 * f));

        Vars.plateMachineRecipes.addRecipe(new Recipe(new ItemStack(Items.gold_ingot, Vars.plateConversionRatio,
            0), Vars.findItemStack("Gold Plate"), 1.0 * f));

    }

    private void recipeCompressor() {
        Vars.compressorRecipes.addRecipe(new Recipe(Vars.findItemStack("4x Graphite Rods", 1),
            Vars.findItemStack("Synthetic Diamond"), 80000.0));

        Vars.compressorRecipes.addRecipe(new Recipe(Vars.findItemStack("Coal Dust", 4),
            Vars.findItemStack("Coal Plate"), 40000.0));

        Vars.compressorRecipes.addRecipe(new Recipe(Vars.findItemStack("Coal Plate", 4),
            Vars.findItemStack("Graphite Rod"), 80000.0));

        Vars.compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.sand),
            Vars.findItemStack("Dielectric"), 2000.0));

        Vars.compressorRecipes.addRecipe(new Recipe(new ItemStack(Blocks.log),
            Vars.findItemStack("Tree Resin"), 3000.0));

    }

    private void recipeMagnetizer() {
        Vars.magnetiserRecipes.addRecipe(new Recipe(new ItemStack(Items.iron_ingot, 2),
            new ItemStack[]{Vars.findItemStack("Basic Magnet")}, 5000.0));
        Vars.magnetiserRecipes.addRecipe(new Recipe(Vars.findItemStack("Alloy Ingot", 2),
            new ItemStack[]{Vars.findItemStack("Advanced Magnet")}, 15000.0));
        Vars.magnetiserRecipes.addRecipe(new Recipe(Vars.findItemStack("Copper Dust", 1),
            new ItemStack[]{new ItemStack(Items.redstone)}, 5000.0));
        Vars.magnetiserRecipes.addRecipe(new Recipe(Vars.findItemStack("Basic Magnet", 3),
            new ItemStack[]{Vars.findItemStack("Optimal Ferromagnetic Core")}, 5000.0));

        Vars.magnetiserRecipes.addRecipe(new Recipe(Vars.findItemStack("Inert Canister", 1),
            new ItemStack[]{new ItemStack(Items.ender_pearl)}, 150000.0));
    }

    private void recipeFuelBurnerItem() {
        addRecipe(Vars.findItemStack("Small Fuel Burner"),
            "   ",
            " Cc",
            "   ",
            'C', Vars.findItemStack("Combustion Chamber"),
            'c', Vars.findItemStack("Copper Thermal Cable"));

        addRecipe(Vars.findItemStack("Medium Fuel Burner"),
            "   ",
            " Cc",
            " C ",
            'C', Vars.findItemStack("Combustion Chamber"),
            'c', Vars.findItemStack("Copper Thermal Cable"));

        addRecipe(Vars.findItemStack("Big Fuel Burner"),
            "   ",
            "CCc",
            "CC ",
            'C', Vars.findItemStack("Combustion Chamber"),
            'c', Vars.findItemStack("Copper Thermal Cable"));
    }

    private void recipeFurnace() {
        ItemStack in;

        in = Vars.findItemStack("Copper Ore");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Copper Ingot"));
        in = Vars.findItemStack("dustCopper");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Copper Ingot"));
        in = Vars.findItemStack("Lead Ore");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("ingotLead"));
        in = Vars.findItemStack("dustLead");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("ingotLead"));
        in = Vars.findItemStack("Tungsten Ore");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Tungsten Ingot"));
        in = Vars.findItemStack("Tungsten Dust");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Tungsten Ingot"));
        in = Vars.findItemStack("dustIron");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            new ItemStack(Items.iron_ingot));

        in = Vars.findItemStack("dustGold");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            new ItemStack(Items.gold_ingot));

        in = Vars.findItemStack("Tree Resin");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Rubber", 2));

        in = Vars.findItemStack("Alloy Dust");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Alloy Ingot"));

        in = Vars.findItemStack("Silicon Dust");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Silicon Ingot"));

        // in = Vars.findItemStack("Purified Cinnabar Dust");
        in = Vars.findItemStack("dustCinnabar");
        Utils.addSmelting(in.getItem(), in.getItemDamage(),
            Vars.findItemStack("Mercury"));

    }

    private void recipeElectricalSensor() {
        addRecipe(Vars.findItemStack("Voltage Probe", 1),
            "SC",
            'S', Vars.findItemStack("Electrical Probe Chip"),
            'C', Vars.findItemStack("Signal Cable"));

        addRecipe(Vars.findItemStack("Electrical Probe", 1),
            "SCS",
            'S', Vars.findItemStack("Electrical Probe Chip"),
            'C', Vars.findItemStack("Signal Cable"));

    }

    private void recipeThermalSensor() {
        addRecipe(Vars.findItemStack("Thermal Probe", 1),
            "SCS",
            'S', Vars.findItemStack("Thermal Probe Chip"),
            'C', Vars.findItemStack("Signal Cable"));

        addRecipe(Vars.findItemStack("Temperature Probe", 1),
            "SC",
            'S', Vars.findItemStack("Thermal Probe Chip"),
            'C', Vars.findItemStack("Signal Cable"));

    }

    private void recipeTransporter() {
        addRecipe(Vars.findItemStack("Experimental Transporter", 1),
            "RMR",
            "RMR",
            " R ",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.findItemStack("High Voltage Cable"),
            'R', Vars.dictAdvancedChip);
    }


    private void recipeTurret() {
        addRecipe(Vars.findItemStack("800V Defence Turret", 1),
            " R ",
            "CMC",
            " c ",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.dictAdvancedChip,
            'c', Vars.highVoltageCableDescriptor.newItemStack(),
            'R', new ItemStack(Blocks.redstone_block));

    }

    private void recipeMachine() {
        addRecipe(Vars.findItemStack("50V Macerator", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Electrical Motor"),
            'F', new ItemStack(Items.flint),
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("200V Macerator", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.dictAdvancedChip,
            'c', Vars.findItemStack("Advanced Electrical Motor"),
            'D', new ItemStack(Items.diamond),
            'I', "ingotAlloy");

        addRecipe(Vars.findItemStack("50V Compressor", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Electrical Motor"),
            'F', "plateIron",
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("200V Compressor", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.dictAdvancedChip,
            'c', Vars.findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(Vars.findItemStack("50V Plate Machine", 1),
            "IRI",
            "IMI",
            "IcI",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Electrical Motor"),
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("200V Plate Machine", 1),
            "DCD",
            "DMD",
            "DcD",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.dictAdvancedChip,
            'c', Vars.findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(Vars.findItemStack("50V Magnetizer", 1),
            "IRI",
            "cMc",
            "III",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Electrical Motor"),
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("200V Magnetizer", 1),
            "ICI",
            "cMc",
            "III",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.dictAdvancedChip,
            'c', Vars.findItemStack("Advanced Electrical Motor"),
            'I', "ingotAlloy");
        addRecipe(Vars.findItemStack("800V Arc Furnace", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', Vars.findItemStack("Advanced Machine Block"),
            'C', Vars.findItemStack("3x Graphite Rods"),
            'c', Vars.findItemStack("Synthetic Diamond"),
            'D', "plateGold",
            'I', "ingotAlloy");

    }

    private void recipeElectricalGate() {
        addShapelessRecipe(Vars.findItemStack("Electrical Timer"),
            new ItemStack(Items.repeater),
            Vars.dictCheapChip);

        addRecipe(Vars.findItemStack("Signal Processor", 1),
            "IcI",
            "cCc",
            "IcI",
            'I', new ItemStack(Items.iron_ingot),
            'c', Vars.findItemStack("Signal Cable"),
            'C', Vars.dictCheapChip);
    }

    private void recipeElectricalRedstone() {
        addRecipe(Vars.findItemStack("Redstone-to-Voltage Converter", 1),
            "TCS",
            'S', Vars.findItemStack("Signal Cable"),
            'C', Vars.dictCheapChip,
            'T', new ItemStack(Blocks.redstone_torch));

        addRecipe(Vars.findItemStack("Voltage-to-Redstone Converter", 1),
            "CTR",
            'R', new ItemStack(Items.redstone),
            'C', Vars.dictCheapChip,
            'T', new ItemStack(Blocks.redstone_torch));

    }

    private void recipeElectricalEnvironmentalSensor() {
        addShapelessRecipe(Vars.findItemStack("Electrical Daylight Sensor"),
            new ItemStack(Blocks.daylight_detector),
            Vars.findItemStack("Redstone-to-Voltage Converter"));

        addShapelessRecipe(Vars.findItemStack("Electrical Light Sensor"),
            new ItemStack(Blocks.daylight_detector),
            new ItemStack(Items.quartz),
            Vars.findItemStack("Redstone-to-Voltage Converter"));

        addRecipe(Vars.findItemStack("Electrical Weather Sensor"),
            " r ",
            "rRr",
            " r ",
            'R', new ItemStack(Items.redstone),
            'r', "itemRubber");

        addRecipe(Vars.findItemStack("Electrical Anemometer Sensor"),
            " I ",
            " R ",
            "I I",
            'R', new ItemStack(Items.redstone),
            'I', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Electrical Entity Sensor"),
            " G ",
            "GRG",
            " G ",
            'G', new ItemStack(Blocks.glass_pane),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Electrical Fire Detector"),
            "cbr",
            "p p",
            "r r",
            'c', Vars.findItemStack("Signal Cable"),
            'b', Vars.dictCheapChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addRecipe(Vars.findItemStack("Electrical Fire Buzzer"),
            "rar",
            "p p",
            "r r",
            'a', Vars.dictAdvancedChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addShapelessRecipe(Vars.findItemStack("Scanner"),
            new ItemStack(Items.comparator),
            Vars.dictAdvancedChip);

    }

    private void recipeElectricalVuMeter() {
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(Vars.findItemStack("Analog vuMeter", 1),
                "WWW",
                "RIr",
                "WSW",
                'W', new ItemStack(Blocks.planks, 1, idx),
                'R', new ItemStack(Items.redstone),
                'I', Vars.findItemStack("Iron Cable"),
                'r', new ItemStack(Items.dye, 1, 1),
                'S', Vars.findItemStack("Signal Cable"));
        }
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(Vars.findItemStack("LED vuMeter", 1),
                " W ",
                "WTW",
                " S ",
                'W', new ItemStack(Blocks.planks, 1, idx),
                'T', new ItemStack(Blocks.redstone_torch),
                'S', Vars.findItemStack("Signal Cable"));
        }
    }

    private void recipeElectricalBreaker() {

        addRecipe(Vars.findItemStack("Electrical Breaker", 1),
            "crC",
            'c', Vars.findItemStack("Overvoltage Protection"),
            'C', Vars.findItemStack("Overheating Protection"),
            'r', Vars.findItemStack("High Voltage Relay"));

    }

    private void recipeFuses() {

        addRecipe(Vars.findItemStack("Electrical Fuse Holder", 1),
            "i",
            " ",
            "i",
            'i', Vars.findItemStack("Iron Cable"));

        addRecipe(Vars.findItemStack("Lead Fuse for low voltage cables", 4),
            "rcr",
            'r', Vars.findItemStack("itemRubber"),
            'c', Vars.findItemStack("Low Voltage Cable"));

        addRecipe(Vars.findItemStack("Lead Fuse for medium voltage cables", 4),
            "rcr",
            'r', Vars.findItemStack("itemRubber"),
            'c', Vars.findItemStack("Medium Voltage Cable"));

        addRecipe(Vars.findItemStack("Lead Fuse for high voltage cables", 4),
            "rcr",
            'r', Vars.findItemStack("itemRubber"),
            'c', Vars.findItemStack("High Voltage Cable"));

        addRecipe(Vars.findItemStack("Lead Fuse for very high voltage cables", 4),
            "rcr",
            'r', Vars.findItemStack("itemRubber"),
            'c', Vars.findItemStack("Very High Voltage Cable"));

    }

    private void recipeElectricalGateSource() {
        addRecipe(Vars.findItemStack("Signal Trimmer", 1),
            "RsR",
            "rRr",
            " c ",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Signal Cable"),
            'r', "itemRubber",
            's', new ItemStack(Items.stick),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Signal Switch", 3),
            " r ",
            "rRr",
            " c ",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Signal Button", 3),
            " R ",
            "rRr",
            " c ",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Wireless Switch", 3),
            " a ",
            "rCr",
            " r ",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Signal Cable"),
            'C', Vars.dictCheapChip,
            'a', Vars.findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("Wireless Button", 3),
            " a ",
            "rCr",
            " R ",
            'M', Vars.findItemStack("Machine Block"),
            'c', Vars.findItemStack("Signal Cable"),
            'C', Vars.dictCheapChip,
            'a', Vars.findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        // Wireless Switch
        // Wireless Button
    }

    private void recipeElectricalDataLogger() {
        addRecipe(Vars.findItemStack("Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', Vars.dictCheapChip,
            'G', new ItemStack(Blocks.glass_pane));

        addRecipe(Vars.findItemStack("Modern Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', Vars.dictAdvancedChip,
            'G', new ItemStack(Blocks.glass_pane));

        addRecipe(Vars.findItemStack("Industrial Data Logger", 1),
            "RRR",
            "GGG",
            "RCR",
            'R', "itemRubber",
            'C', Vars.dictAdvancedChip,
            'G', new ItemStack(Blocks.glass_pane));
    }

    private void recipeSixNodeCache() {

    }

    private void recipeElectricalAlarm() {
        addRecipe(Vars.findItemStack("Nuclear Alarm", 1),
            "ITI",
            "IMI",
            "IcI",
            'c', Vars.findItemStack("Signal Cable"),
            'T', new ItemStack(Blocks.redstone_torch),
            'I', Vars.findItemStack("Iron Cable"),
            'M', new ItemStack(Blocks.noteblock));
        addRecipe(Vars.findItemStack("Standard Alarm", 1),
            "MTM",
            "IcI",
            "III",
            'c', Vars.findItemStack("Signal Cable"),
            'T', new ItemStack(Blocks.redstone_torch),
            'I', Vars.findItemStack("Iron Cable"),
            'M', new ItemStack(Blocks.noteblock));

    }

    private void recipeElectricalAntenna() {
        addRecipe(Vars.findItemStack("Low Power Transmitter Antenna", 1),
            "R i",
            "CI ",
            "R i",
            'C', Vars.dictCheapChip,
            'i', new ItemStack(Items.iron_ingot),
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("Low Power Receiver Antenna", 1),
            "i  ",
            " IC",
            "i  ",
            'C', Vars.dictCheapChip,
            'I', "plateIron",
            'i', new ItemStack(Items.iron_ingot),
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("Medium Power Transmitter Antenna", 1),
            "c I",
            "CI ",
            "c I",
            'C', Vars.dictAdvancedChip,
            'c', Vars.dictCheapChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("Medium Power Receiver Antenna", 1),
            "I  ",
            " IC",
            "I  ",
            'C', Vars.dictAdvancedChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("High Power Transmitter Antenna", 1),
            "C I",
            "CI ",
            "C I",
            'C', Vars.dictAdvancedChip,
            'c', Vars.dictCheapChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("High Power Receiver Antenna", 1),
            "I D",
            " IC",
            "I D",
            'C', Vars.dictAdvancedChip,
            'I', "plateIron",
            'R', new ItemStack(Items.redstone),
            'D', new ItemStack(Items.diamond));

    }

    private void recipeBatteryCharger() {
        addRecipe(Vars.findItemStack("Weak 50V Battery Charger", 1),
            "RIR",
            "III",
            "RcR",
            'c', Vars.findItemStack("Low Voltage Cable"),
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));
        addRecipe(Vars.findItemStack("50V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', Vars.dictCheapChip,
            'c', Vars.findItemStack("Low Voltage Cable"),
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

        addRecipe(Vars.findItemStack("200V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', Vars.dictAdvancedChip,
            'c', Vars.findItemStack("Medium Voltage Cable"),
            'I', Vars.findItemStack("Iron Cable"),
            'R', new ItemStack(Items.redstone));

    }

    private void recipeEggIncubator() {
        addRecipe(Vars.findItemStack("50V Egg Incubator", 1),
            "IGG",
            "E G",
            "CII",
            'C', Vars.dictCheapChip,
            'E', Vars.findItemStack("Small 50V Tungsten Heating Corp"),
            'I', new ItemStack(Items.iron_ingot),
            'G', new ItemStack(Blocks.glass_pane));

    }

    private void recipeEnergyConverter() {
        if (Vars.ElnToOtherEnergyConverterEnable) {
            addRecipe(new ItemStack(Vars.elnToOtherBlockLvu),
                "III",
                "cCR",
                "III",
                'C', Vars.dictCheapChip,
                'c', Vars.findItemStack("Low Voltage Cable"),
                'I', Vars.findItemStack("Iron Cable"),
                'R', "ingotCopper");

            addRecipe(new ItemStack(Vars.elnToOtherBlockMvu),
                "III",
                "cCR",
                "III",
                'C', Vars.dictCheapChip,
                'c', Vars.findItemStack("Medium Voltage Cable"),
                'I', Vars.findItemStack("Iron Cable"),
                'R', Vars.dictTungstenIngot);

            addRecipe(new ItemStack(Vars.elnToOtherBlockHvu),
                "III",
                "cCR",
                "III",
                'C', Vars.dictAdvancedChip,
                'c', Vars.findItemStack("High Voltage Cable"),
                'I', Vars.findItemStack("Iron Cable"),
                'R', new ItemStack(Items.gold_ingot));

        }
    }

    private void recipeComputerProbe() {
        if (Vars.ComputerProbeEnable) {
            addRecipe(new ItemStack(Vars.computerProbeBlock),
                "cIw",
                "ICI",
                "WIc",
                'C', Vars.dictAdvancedChip,
                'c', Vars.findItemStack("Signal Cable"),
                'I', Vars.findItemStack("Iron Cable"),
                'w', Vars.findItemStack("Wireless Signal Receiver"),
                'W', Vars.findItemStack("Wireless Signal Transmitter"));
        }
    }

    private void recipeArmor() {
        addRecipe(new ItemStack(Vars.helmetCopper),
            "CCC",
            "C C",
            'C', "ingotCopper");

        addRecipe(new ItemStack(Vars.plateCopper),
            "C C",
            "CCC",
            "CCC",
            'C', "ingotCopper");

        addRecipe(new ItemStack(Vars.legsCopper),
            "CCC",
            "C C",
            "C C",
            'C', "ingotCopper");

        addRecipe(new ItemStack(Vars.bootsCopper),
            "C C",
            "C C",
            'C', "ingotCopper");
    }

    private void recipeTool() {
        addRecipe(new ItemStack(Vars.shovelCopper),
            "i",
            "s",
            "s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Vars.axeCopper),
            "ii",
            "is",
            " s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Vars.hoeCopper),
            "ii",
            " s",
            " s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Vars.pickaxeCopper),
            "iii",
            " s ",
            " s ",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));
        addRecipe(new ItemStack(Vars.swordCopper),
            "i",
            "i",
            "s",
            'i', "ingotCopper",
            's', new ItemStack(Items.stick));

    }

    private void recipeDisplays() {
        addRecipe(Vars.findItemStack("Digital Display", 1),
            "   ",
            "rrr",
            "iii",
            'r', new ItemStack(Items.redstone),
            'i', Vars.findItemStack("Iron Cable")
        );

        addRecipe(Vars.findItemStack("Nixie Tube", 1),
            " g ",
            "grg",
            "iii",
            'g', new ItemStack(Blocks.glass_pane),
            'r', new ItemStack(Items.redstone),
            'i', Vars.findItemStack("Iron Cable")
        );
    }

}

package mods.eln;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.block.ArcClayBlock;
import mods.eln.block.ArcMetalBlock;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.SharedItem;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostManager;
import mods.eln.ghost.GhostManagerNbt;
import mods.eln.item.CopperCableDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.TreeResin;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.PortableOreScannerItem;
import mods.eln.misc.*;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeManagerNbt;
import mods.eln.node.NodeServer;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeItem;
import mods.eln.node.transparent.TransparentNodeBlock;
import mods.eln.node.transparent.TransparentNodeItem;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreDescriptor;
import mods.eln.ore.OreItem;
import mods.eln.server.*;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.simplenode.computerprobe.ComputerProbeBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.lampsocket.LightBlock;
import mods.eln.sixnode.modbusrtu.ModbusTcpServer;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Vars {

    public static final String channelName = "miaouMod";
    public static final double solarPanelBasePower = 65.0;
    public static ArrayList<IConfigSharing> configShared = new ArrayList<IConfigSharing>();
    public static SimpleNetworkWrapper elnNetwork;
    public static final byte packetPlayerKey = 14;
    public static final byte packetNodeSingleSerialized = 15;
    public static final byte packetPublishForNode = 16;
    public static final byte packetOpenLocalGui = 17;
    public static final byte packetForClientNode = 18;
    public static final byte packetPlaySound = 19;
    public static final byte packetDestroyUuid = 20;
    public static final byte packetClientToServerConnection = 21;
    public static final byte packetServerToClientInfo = 22;

    public static PacketHandler packetHandler;
    public static NodeServer nodeServer;
    public static LiveDataManager clientLiveDataManager;
    public static ClientKeyHandler clientKeyHandler;
    public static SaveConfig saveConfig;
    public static GhostManager ghostManager;
    public static GhostManagerNbt ghostManagerNbt;
    public static NodeManager nodeManager;
    public static PlayerManager playerManager;
    public static ModbusTcpServer modbusServer;
    public static NodeManagerNbt nodeManagerNbt;
    public static Simulator simulator = null;
    public static DelayedTaskManager delayedTask;
    public static ItemEnergyInventoryProcess itemEnergyInventoryProcess;
    public static CreativeTabs creativeTab;

    public static Item swordCopper, hoeCopper, shovelCopper, pickaxeCopper, axeCopper;

    public static ItemArmor helmetCopper, plateCopper, legsCopper, bootsCopper;
    public static ItemArmor helmetECoal, plateECoal, legsECoal, bootsECoal;

    public static SharedItem sharedItem;
    public static SharedItem sharedItemStackOne;
    public static ItemStack wrenchItemStack;
    public static SixNodeBlock sixNodeBlock;
    public static TransparentNodeBlock transparentNodeBlock;
    public static OreBlock oreBlock;
    public static GhostBlock ghostBlock;
    public static LightBlock lightBlock;
    public static ArcClayBlock arcClayBlock;
    public static ArcMetalBlock arcMetalBlock;

    public static SixNodeItem sixNodeItem;
    public static TransparentNodeItem transparentNodeItem;
    public static OreItem oreItem;



    public static double electricalFrequency, thermalFrequency;
    public static int electricalInterSystemOverSampling;

    public static CopperCableDescriptor copperCableDescriptor;
    public static mods.eln.item.GraphiteDescriptor GraphiteDescriptor;

    public static ElectricalCableDescriptor creativeCableDescriptor;
    /*public ElectricalCableDescriptor T2TransmissionCableDescriptor;
    public ElectricalCableDescriptor T1TransmissionCableDescriptor;*/
    public static ElectricalCableDescriptor veryHighVoltageCableDescriptor;
    public static ElectricalCableDescriptor highVoltageCableDescriptor;
    public static ElectricalCableDescriptor signalCableDescriptor;
    public static ElectricalCableDescriptor lowVoltageCableDescriptor;
    public static ElectricalCableDescriptor batteryCableDescriptor;
    public static ElectricalCableDescriptor meduimVoltageCableDescriptor;
    public static ElectricalCableDescriptor signalBusCableDescriptor;

    public static OreRegenerate oreRegenerate;

    public static final Obj3DFolder obj = new Obj3DFolder();

    public static boolean oredictTungsten, oredictChips;
    public static boolean genCopper, genLead, genTungsten, genCinnabar;
    public static String dictTungstenOre, dictTungstenDust, dictTungstenIngot;
    public static String dictCheapChip, dictAdvancedChip;
    public static final ArrayList<PortableOreScannerItem.RenderStorage.OreScannerConfigElement> oreScannerConfig = new ArrayList<PortableOreScannerItem.RenderStorage.OreScannerConfigElement>();
    public static boolean modbusEnable = false;
    public static int modbusPort;

    public static float xRayScannerRange;
    public static boolean addOtherModOreToXRay;

    public static boolean replicatorPop;

    public static boolean xRayScannerCanBeCrafted = true;
    public static boolean forceOreRegen;
    public static boolean explosionEnable;

    public static boolean debugEnabled = false;  // Read from configuration file. Default is `false`.
    public static boolean versionCheckEnabled = true; // Read from configuration file. Default is `true`.
    public static boolean analyticsEnabled = true; // Read from configuration file. Default is `true`.
    public static String playerUUID = null; // Read from configuration file. Default is `null`.

    public static double heatTurbinePowerFactor = 1;
    public static double solarPanelPowerFactor = 1;
    public static double windTurbinePowerFactor = 1;
    public static double waterTurbinePowerFactor = 1;
    public static double fuelGeneratorPowerFactor = 1;
    public static double fuelHeatFurnacePowerFactor = 1;
    public static int autominerRange = 10;

    public static double cableRsFactor = 1.0;
    public static double cablePace = 1.0;

    public static boolean killMonstersAroundLamps;
    public static int killMonstersAroundLampsRange;

    public static int maxReplicators = 100;

    public static double stdBatteryHalfLife = 2 * Utils.minecraftDay;
    public static double batteryCapacityFactor = 1.;

    public static boolean wailaEasyMode = false;

    public static double fuelHeatValueFactor = 0.0000675;
    public static int plateConversionRatio;

    public static boolean noSymbols = false;
    public static boolean noVoltageBackground = false;

    public static double maxSoundDistance = 16;

    public static CableRenderDescriptor stdCableRenderSignal;
    public static CableRenderDescriptor stdCableRenderSignalBus;
    public static CableRenderDescriptor stdCableRender50V;
    public static CableRenderDescriptor stdCableRender200V;
    public static CableRenderDescriptor stdCableRender800V;
    public static CableRenderDescriptor stdCableRender3200V;
    public static CableRenderDescriptor stdCableRenderCreative;

    public static final double gateOutputCurrent = 0.100;
    public static final double SVU = 50, SVII = gateOutputCurrent / 50, SVUinv = 1.0 / SVU;
    public static final double LVU = 50;
    public static final double MVU = 200;
    public static final double HVU = 800;
    public static final double VVU = 3200;

    public static final double SVP = gateOutputCurrent * SVU;
    public static final double LVP = 1000 * cablePace;
    public static final double MVP = 2000 * cablePace;
    public static final double HVP = 5000 * cablePace;
    public static final double VVP = 15000 * cablePace;
    public static final double electricalCableDeltaTMax = 20;

    public static final double cableHeatingTime = 30;
    public static final double cableWarmLimit = 130;
    public static final double cableThermalConductionTao = 0.5;
    public static final ThermalLoadInitializer cableThermalLoadInitializer = new ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, cableThermalConductionTao);
    public static final ThermalLoadInitializer sixNodeThermalLoadInitializer = new ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, 1000);

    public static int wirelessTxRange = 32;

    public static FunctionTable batteryVoltageFunctionTable;

    public static ArrayList<ItemStack> furnaceList = new ArrayList<>();

    public static ElectricalFurnaceDescriptor electricalFurnace;

    public static RecipesList maceratorRecipes = new RecipesList();
    public static RecipesList compressorRecipes = new RecipesList();
    public static RecipesList plateMachineRecipes = new RecipesList();
    public static RecipesList arcFurnaceRecipes = new RecipesList();
    public static RecipesList magnetiserRecipes = new RecipesList();

    public static double incandescentLampLife;
    public static double economicLampLife;
    public static double carbonLampLife;
    public static double ledLampLife;
    public static boolean ledLampInfiniteLife = false;

    public static OreDescriptor oreTin, oreCopper, oreSilver;

    public static GenericItemUsingDamageDescriptorWithComment dustTin,
        dustCopper, dustSilver;

    public static final HashMap<String, ItemStack> dictionnaryOreFromMod = new HashMap<String, ItemStack>();

    public static GenericItemUsingDamageDescriptorWithComment tinIngot, copperIngot,
        silverIngot, plumbIngot, tungstenIngot;

    public static double fuelGeneratorTankCapacity = 20 * 60;

    public static GenericItemUsingDamageDescriptor multiMeterElement,
        thermometerElement, allMeterElement;
    public static GenericItemUsingDamageDescriptor configCopyToolElement;

    public static TreeResin treeResin;

    public static MiningPipeDescriptor miningPipeDescriptor;

    public static DataLogsPrintDescriptor dataLogsPrintDescriptor;

    public static int replicatorRegistrationId = -1;

    public static boolean ic2Loaded = false;
    public static boolean ocLoaded = false;
    public static boolean ccLoaded = false;
    public static boolean teLoaded = false;

    public static double ElnToIc2ConversionRatio;
    public static double ElnToOcConversionRatio;
    public static double ElnToTeConversionRatio;

    public static final String modIdIc2 = "IC2";
    public static final String modIdOc = "OpenComputers";
    public static final String modIdTe = "Eln";
    public static final String modIdCc = "ComputerCraft";

    public static FMLEventChannel eventChannel;
    //boolean computerCraftReady = false;
    public static boolean ComputerProbeEnable;
    public static boolean ElnToOtherEnergyConverterEnable;


    public static EnergyConverterElnToOtherBlock elnToOtherBlockLvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockMvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockHvu;

    public static ServerEventListener serverEventListener;

    public static HashSet<String> oreNames;

    public static void addToOre(String name, ItemStack ore) {
        OreDictionary.registerOre(name, ore);
        Vars.dictionnaryOreFromMod.put(name, ore);
    }

    public static ItemStack findItemStack(String name, int stackSize) {
        ItemStack stack = GameRegistry.findItemStack("Eln", name, stackSize);
        if (stack == null) {
            stack = dictionnaryOreFromMod.get(name);
            stack = Utils.newItemStack(Item.getIdFromItem(stack.getItem()), stackSize, stack.getItemDamage());
        }
        return stack;
    }

    public static ItemStack findItemStack(String name) {
        return findItemStack(name, 1);
    }

    public static String firstExistingOre(String... oreNames) {
        for (String oreName : oreNames) {
            if (OreDictionary.doesOreNameExist(oreName)) {
                return oreName;
            }
        }

        return "";
    }

    public static ComputerProbeBlock computerProbeBlock;

    public static double getElnToIc2ConversionRatio() {
        return ElnToIc2ConversionRatio;
    }

    public static double getElnToOcConversionRatio() {
        return ElnToOcConversionRatio;
    }

    public static double getElnToTeConversionRatio() {
        return ElnToTeConversionRatio;
    }

    public static void check() {
        ic2Loaded = Loader.isModLoaded(modIdIc2);
        ocLoaded = Loader.isModLoaded(modIdOc);
        ccLoaded = Loader.isModLoaded(modIdCc);
        teLoaded = Loader.isModLoaded(modIdTe);
    }
}

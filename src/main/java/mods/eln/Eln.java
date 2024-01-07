package mods.eln;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.block.ArcClayBlock;
import mods.eln.block.ArcClayItemBlock;
import mods.eln.block.ArcMetalBlock;
import mods.eln.block.ArcMetalItemBlock;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.client.SoundLoader;
import mods.eln.config.ConfigHandler;
import mods.eln.craft.CraftingRecipes;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.eventhandlers.ElnFMLEventsHandler;
import mods.eln.eventhandlers.ElnForgeEventsHandler;
import mods.eln.fluid.ElnFluidRegistry;
import mods.eln.fluid.FluidRegistrationKt;
import mods.eln.generic.GenericCreativeTab;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.SharedItem;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostManager;
import mods.eln.ghost.GhostManagerNbt;
import mods.eln.item.*;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.OreColorMapping;
import mods.eln.item.electricalitem.PortableOreScannerItem.RenderStorage.OreScannerConfigElement;
import mods.eln.misc.*;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeManagerNbt;
import mods.eln.node.NodeServer;
import mods.eln.node.six.*;
import mods.eln.node.transparent.*;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreDescriptor;
import mods.eln.ore.OreItem;
import mods.eln.packets.*;
import mods.eln.registration.ItemRegistration;
import mods.eln.registration.SingleNodeRegistration;
import mods.eln.registration.SixNodeRegistration;
import mods.eln.registration.TransparentNodeRegistration;
import mods.eln.server.*;
import mods.eln.server.console.ElnConsoleCommands;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.simplenode.computerprobe.ComputerProbeBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.sixnode.PortableNaNDescriptor;
import mods.eln.sixnode.currentcable.CurrentCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.lampsocket.LightBlock;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import mods.eln.sixnode.modbusrtu.ModbusTcpServer;
import mods.eln.sixnode.tutorialsign.TutorialSignElement;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import mods.eln.transparentnode.computercraftio.PeripheralHandler;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterElement;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static mods.eln.i18n.I18N.TR;
import static mods.eln.i18n.I18N.tr;

@Mod(modid = Eln.MODID, name = Eln.NAME, version = Tags.VERSION, dependencies = "after:CoFHCore;after:CoFHAPI;" +
        "after:CoFHAPI|energy")
public class Eln {
    @Instance("Eln")
    public static Eln instance;
    @SidedProxy(clientSide = "mods.eln.client.ClientProxy", serverSide = "mods.eln.CommonProxy")
    public static CommonProxy proxy;
    public final static String MODID = Tags.MODID;
    public final static String NAME = Tags.MODNAME;
    public final static String MODDESC = "Electricity in your base!";
    public final static String URL = "https://eln.ja13.org";
    public final static String UPDATE_URL = "https://github.com/jrddunbr/ElectricalAge/releases";
    public final static String SRC_URL = "https://github.com/jrddunbr/ElectricalAge";
    public final static String[] AUTHORS = {"Dolu1990", "jrddunbr", "Baughn", "Grissess", "Caeleron", "Omega_Haxors",
     "lambdaShade", "cm0x4D", "metc"};
    public static final String channelName = "miaouMod";
    public static final double solarPanelBasePower = 65.0;
    public static final byte packetPlayerKey = 14;
    public static final byte packetNodeSingleSerialized = 15;
    public static final byte packetPublishForNode = 16;
    public static final byte packetOpenLocalGui = 17;
    public static final byte packetForClientNode = 18;
    public static final byte packetPlaySound = 19;
    public static final byte packetDestroyUuid = 20;
    public static final byte packetClientToServerConnection = 21;
    public static final byte packetServerToClientInfo = 22;
    public static final Obj3DFolder obj = new Obj3DFolder();
    public static final ArrayList<OreScannerConfigElement> oreScannerConfig = new ArrayList<OreScannerConfigElement>();
    public static final double gateOutputCurrent = 0.100;
    public static final double LVU = 50;
    public static final double MVU = 200;
    public static final double HVU = 800;
    public static final double VVU = 3200;
    public static final double CCU = 120_000;
    public static final double cableHeatingTime = 30;
    public static final double cableWarmLimit = 130;
    public static final double cableThermalConductionTao = 0.5;
    public static final ThermalLoadInitializer cableThermalLoadInitializer =
     new ThermalLoadInitializer(cableWarmLimit, -100, cableHeatingTime, cableThermalConductionTao);
    public static final ThermalLoadInitializer sixNodeThermalLoadInitializer =
     new ThermalLoadInitializer(cableWarmLimit, -100, cableHeatingTime, 1000);
    public static final HashMap<String, ItemStack> dictionnaryOreFromMod = new HashMap<>();
    public static Logger logger = LogManager.getLogger("ELN");
    public static SimpleNetworkWrapper elnNetwork;
    public static PacketHandler packetHandler;
    public static LiveDataManager clientLiveDataManager;
    public static ClientKeyHandler clientKeyHandler;
    public static SaveConfig saveConfig;
    public static GhostManager ghostManager;
    public static GhostManagerNbt ghostManagerNbt;
    public static ModbusTcpServer modbusServer;
    public static NodeManagerNbt nodeManagerNbt;
    public static Simulator simulator = null;
    public static DelayedTaskManager delayedTask;
    public static ItemEnergyInventoryProcess itemEnergyInventoryProcess;
    public static CreativeTabs creativeTab;
    public static Item swordCopper, hoeCopper, shovelCopper, pickaxeCopper, axeCopper;
    public static GenericItemUsingDamageDescriptorWithComment plateCopper;
    public static ItemArmor helmetCopper, chestplateCopper, legsCopper, bootsCopper;
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
    public static String analyticsURL = "";
    public static boolean analyticsPlayerUUIDOptIn = false;

    public static PortableNaNDescriptor portableNaNDescriptor = null;
    public static CableRenderDescriptor stdPortableNaN = null;
    public static boolean oredictTungsten, oredictChips;
    public static boolean genCopper, genLead, genTungsten, genCinnabar;
    public static String dictTungstenOre, dictTungstenDust, dictTungstenIngot;
    public static String dictCheapChip, dictAdvancedChip;
    public static boolean modbusEnable = false;
    public static int modbusPort;
    public static boolean explosionEnable;
    public static boolean debugEnabled = false;  // Read from configuration file. Default is `false`.
    public static boolean debugExplosions = false;
    public static boolean versionCheckEnabled = true; // Read from configuration file. Default is `true`.
    public static boolean analyticsEnabled = true; // Read from configuration file. Default is `true`.
    public static String playerUUID = null; // Read from configuration file. Default is `null`.
    public static boolean wailaEasyMode = false;
    public static double shaftEnergyFactor = 0.05;
    public static double fuelHeatValueFactor = 0.0000675;
    public static boolean noSymbols = false;
    public static boolean noVoltageBackground = false;
    public static double maxSoundDistance = 16;
    public static int soundChannels = 200;
    public static double cablePowerFactor;
    public static boolean allowSwingingLamps = true;
    public static boolean enableFestivities = true;
    public static boolean verticalIronCableCrafting = false;
    public static Double flywheelMass = 0.0;
    public static boolean directPoles = false;
    public static SiliconWafer siliconWafer;
    public static Transistor transistor;
    public static Thermistor thermistor;
    public static NibbleMemory nibbleMemory;
    public static ArithmeticLogicUnit alu;
    public static String dictSiliconWafer;
    public static String dictTransistor;
    public static String dictThermistor;
    public static String dictNibbleMemory;
    public static String dictALU;
    public static Configuration config;
    public static FMLEventChannel eventChannel;
    public static Map<ElnFluidRegistry, Fluid> fluids = new EnumMap(ElnFluidRegistry.class);
    public static Map<ElnFluidRegistry, Block> fluidBlocks = new EnumMap(ElnFluidRegistry.class);
    public static WindProcess wind;
    public static int wirelessTxRange = 32;
    public static boolean ledLampInfiniteLife = false;
    static public GenericItemUsingDamageDescriptor multiMeterElement, thermometerElement, allMeterElement;
    static public GenericItemUsingDamageDescriptor configCopyToolElement;
    public static TreeResin treeResin;
    public static MiningPipeDescriptor miningPipeDescriptor;
    static NodeServer nodeServer;
    private static NodeManager nodeManager;
    public static OreDescriptor oreCopper;
    public static GenericItemUsingDamageDescriptorWithComment dustCopper;
    public ArrayList<IConfigSharing> configShared = new ArrayList<>();
    public double electricalFrequency, thermalFrequency;
    public int electricalInterSystemOverSampling;
    public CopperCableDescriptor copperCableDescriptor;
    public ElectricalCableDescriptor creativeCableDescriptor;
    public ElectricalCableDescriptor veryHighVoltageCableDescriptor;
    public ElectricalCableDescriptor highVoltageCableDescriptor;
    public ElectricalCableDescriptor signalCableDescriptor;
    public ElectricalCableDescriptor lowVoltageCableDescriptor;
    public ElectricalCableDescriptor meduimVoltageCableDescriptor;
    public ElectricalCableDescriptor signalBusCableDescriptor;
    public CurrentCableDescriptor lowCurrentCableDescriptor;
    public CurrentCableDescriptor mediumCurrentCableDescriptor;
    public CurrentCableDescriptor highCurrentCableDescriptor;
    public OreRegenerate oreRegenerate;
    public boolean forceOreRegen;
    public double heatTurbinePowerFactor = 1;
    public double solarPanelPowerFactor = 1;
    public double windTurbinePowerFactor = 1;
    public double waterTurbinePowerFactor = 1;
    public double fuelGeneratorPowerFactor = 1;
    public double fuelHeatFurnacePowerFactor = 1;
    public int autominerRange = 10;
    public boolean killMonstersAroundLamps;
    public int killMonstersAroundLampsRange;
    public int maxReplicators = 100;
    public Double ELN_CONVERTER_MAX_POWER = 120_000.0;
    public ServerEventListener serverEventListener;
    public CableRenderDescriptor stdCableRenderSignal;
    public CableRenderDescriptor stdCableRenderSignalBus;
    public CableRenderDescriptor stdCableRender50V;
    public CableRenderDescriptor stdCableRender200V;
    public CableRenderDescriptor stdCableRender800V;
    public CableRenderDescriptor stdCableRender3200V;
    public CableRenderDescriptor stdCableRenderCreative;
    public CableRenderDescriptor lowCurrentCableRender;
    public CableRenderDescriptor mediumCurrentCableRender;
    public CableRenderDescriptor highCurrentCableRender;
    public FunctionTable batteryVoltageFunctionTable;
    public ArrayList<ItemStack> furnaceList = new ArrayList<ItemStack>();
    public RecipesList maceratorRecipes = new RecipesList();
    public RecipesList compressorRecipes = new RecipesList();
    public RecipesList plateMachineRecipes = new RecipesList();
    public RecipesList arcFurnaceRecipes = new RecipesList();
    public RecipesList magnetiserRecipes = new RecipesList();
    public GenericItemUsingDamageDescriptorWithComment copperIngot, plumbIngot, tungstenIngot;
    public DataLogsPrintDescriptor dataLogsPrintDescriptor;
    public float xRayScannerRange;
    public boolean addOtherModOreToXRay;
    public boolean xRayScannerCanBeCrafted = true;
    public double stdBatteryHalfLife = 2 * Utils.minecraftDay;
    public static final double SVU = 5, SVII = gateOutputCurrent / SVU, SVUinv = 1.0 / SVU;
    public double batteryCapacityFactor = 1.;
    public boolean replicatorPop;
    public int plateConversionRatio;
    public boolean ComputerProbeEnable;
    public boolean ElnToOtherEnergyConverterEnable;
    public EnergyConverterElnToOtherBlock elnToOtherBlockConverter;
    public ComputerProbeBlock computerProbeBlock;
    public static final double SVP = gateOutputCurrent * SVU;
    public ElectricalFurnaceDescriptor electricalFurnace;
    public double incandescentLampLife;
    public double economicLampLife;
    public double carbonLampLife;
    public double ledLampLife;
    public double fuelGeneratorTankCapacity = 20 * 60;
    public int replicatorRegistrationId = -1;

    public static HashSet<String> oreNames = new HashSet<>();

    public static BrushDescriptor whiteDesc;

    public static List<String> brushSubNames;

    public static double getSmallRs() {
        return instance.lowVoltageCableDescriptor.electricalRs;
    }

    public static void applySmallRs(NbtElectricalLoad aLoad) {
        instance.lowVoltageCableDescriptor.applyTo(aLoad);
    }

    public static void applySmallRs(Resistor r) {
        instance.lowVoltageCableDescriptor.applyTo(r);
    }

    public static ItemStack findItemStack(String name, int stackSize) {
        ItemStack stack = GameRegistry.findItemStack("Eln", name, stackSize);
        if (stack == null) {
            stack = dictionnaryOreFromMod.get(name);
            stack = Utils.newItemStack(Item.getIdFromItem(stack.getItem()), stackSize, stack.getItemDamage());
        }
        return stack;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        elnNetwork = NetworkRegistry.INSTANCE.newSimpleChannel("electrical-age");
        elnNetwork.registerMessage(AchievePacketHandler.class, AchievePacket.class, 0, Side.SERVER);
        elnNetwork.registerMessage(TransparentNodeRequestPacketHandler.class, TransparentNodeRequestPacket.class, 1,
                Side.SERVER);
        elnNetwork.registerMessage(TransparentNodeResponsePacketHandler.class, TransparentNodeResponsePacket.class, 2,
                Side.CLIENT);
        elnNetwork.registerMessage(GhostNodeWailaRequestPacketHandler.class, GhostNodeWailaRequestPacket.class, 3,
                Side.SERVER);
        elnNetwork.registerMessage(GhostNodeWailaResponsePacketHandler.class, GhostNodeWailaResponsePacket.class, 4,
                Side.CLIENT);
        elnNetwork.registerMessage(SixNodeWailaRequestPacketHandler.class, SixNodeWailaRequestPacket.class, 5,
                Side.SERVER);
        elnNetwork.registerMessage(SixNodeWailaResponsePacketHandler.class, SixNodeWailaResponsePacket.class, 6,
                Side.CLIENT);

        ModContainer container = FMLCommonHandler.instance().findContainerFor(this);
        ModMetadata meta = event.getModMetadata();
        meta.modId = MODID;
        meta.version = Version.INSTANCE.getSimpleVersionName();
        meta.name = NAME;
        meta.description = tr("mod.meta.desc");
        meta.url = URL;
        meta.updateUrl = UPDATE_URL;
        meta.authorList = Arrays.asList(AUTHORS);
        meta.autogenerated = false; // Force to update from code

        Utils.println(Version.print());

        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT) MinecraftForge.EVENT_BUS.register(new SoundLoader());

        config = new Configuration(event.getSuggestedConfigurationFile());

        ConfigHandler.INSTANCE.loadConfig(this);

        eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);

        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling,
                1 / thermalFrequency);
        nodeManager = new NodeManager("caca");
        ghostManager = new GhostManager("caca2");
        delayedTask = new DelayedTaskManager();

        oreRegenerate = new OreRegenerate();
        nodeServer = new NodeServer();
        clientLiveDataManager = new LiveDataManager();

        packetHandler = new PacketHandler();
        instance = this;

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        Item itemCreativeTab = new Item().setUnlocalizedName("eln:elncreativetab").setTextureName("eln:elncreativetab");
        GameRegistry.registerItem(itemCreativeTab, "eln.itemCreativeTab");
        creativeTab = new GenericCreativeTab("Eln", itemCreativeTab);

        oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setBlockName("OreEln");

        arcClayBlock = new ArcClayBlock();
        arcMetalBlock = new ArcMetalBlock();

        sharedItem =
                (SharedItem) new SharedItem().setCreativeTab(creativeTab).setMaxStackSize(64).setUnlocalizedName("sharedItem");

        sharedItemStackOne =
                (SharedItem) new SharedItem().setCreativeTab(creativeTab).setMaxStackSize(1).setUnlocalizedName(
                        "sharedItemStackOne");

        transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(Material.iron,
                TransparentNodeEntity.class).setCreativeTab(creativeTab).setBlockTextureName("iron_block");
        sixNodeBlock =
                (SixNodeBlock) new SixNodeBlock(Material.plants, SixNodeEntity.class).setCreativeTab(creativeTab).setBlockTextureName("iron_block");

        ghostBlock = (GhostBlock) new GhostBlock().setBlockTextureName("iron_block");
        lightBlock = new LightBlock();

        obj.loadAllElnModels();

        GameRegistry.registerItem(sharedItem, "Eln.sharedItem");
        GameRegistry.registerItem(sharedItemStackOne, "Eln.sharedItemStackOne");
        GameRegistry.registerBlock(ghostBlock, "Eln.ghostBlock");
        GameRegistry.registerBlock(lightBlock, "Eln.lightBlock");
        GameRegistry.registerBlock(sixNodeBlock, SixNodeItem.class, "Eln.SixNode");
        GameRegistry.registerBlock(transparentNodeBlock, TransparentNodeItem.class, "Eln.TransparentNode");
        GameRegistry.registerBlock(oreBlock, OreItem.class, "Eln.Ore");
        GameRegistry.registerBlock(arcClayBlock, ArcClayItemBlock.class, "Eln.arc_clay_block");
        GameRegistry.registerBlock(arcMetalBlock, ArcMetalItemBlock.class, "Eln.arc_metal_block");
        TileEntity.addMapping(TransparentNodeEntity.class, "TransparentNodeEntity");
        TileEntity.addMapping(TransparentNodeEntityWithFluid.class, "TransparentNodeEntityWF");
        TileEntity.addMapping(SixNodeEntity.class, "SixNodeEntity");
        TileEntity.addMapping(LightBlockEntity.class, "LightBlockEntity");

        NodeManager.registerUuid(sixNodeBlock.getNodeUuid(), SixNode.class);
        NodeManager.registerUuid(transparentNodeBlock.getNodeUuid(), TransparentNode.class);

        sixNodeItem = (SixNodeItem) Item.getItemFromBlock(sixNodeBlock);
        transparentNodeItem = (TransparentNodeItem) Item.getItemFromBlock(transparentNodeBlock);

        oreItem = (OreItem) Item.getItemFromBlock(oreBlock);

        SixNode.sixNodeCacheList.add(new SixNodeCacheStd());

        SingleNodeRegistration.INSTANCE.registerSingle();
        SixNodeRegistration.INSTANCE.registerSix();
        TransparentNodeRegistration.INSTANCE.registerTransparent();
        ItemRegistration.INSTANCE.registerItem();

        OreDictionary.registerOre("blockAluminum", arcClayBlock);
        OreDictionary.registerOre("blockSteel", arcMetalBlock);

        AnalyticsHandler.INSTANCE.submitUpstreamAnalytics();
        AnalyticsHandler.INSTANCE.submitAgeSeriesAnalytics();
    }

    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        Other.check();
        if (Other.ccLoaded) {
            PeripheralHandler.register();
        }
        CraftingRecipes.INSTANCE.itemCrafting();
    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        final String[] names = OreDictionary.getOreNames();
        Collections.addAll(oreNames, names);
        proxy.registerRenderers();
        TR("itemGroup.Eln");
        if (isDevelopmentRun()) {
            Achievements.init();
        }
        FluidRegistrationKt.registerElnFluids();
        MinecraftForge.EVENT_BUS.register(new ElnForgeEventsHandler());
        FMLCommonHandler.instance().bus().register(new ElnFMLEventsHandler());
        MinecraftForge.EVENT_BUS.register(this);
        FMLInterModComms.sendMessage("Waila", "register", "mods.eln.integration.waila.WailaIntegration" +
                ".callbackRegister");
        Utils.println("Electrical age init done");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        serverEventListener = new ServerEventListener();
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent ev) {
        TutorialSignElement.resetBalise();
        if (modbusServer != null) {
            modbusServer.destroy();
            modbusServer = null;
        }
        LightBlockEntity.observers.clear();
        NodeBlockEntity.clientList.clear();
        TeleporterElement.teleporterList.clear();
        IWirelessSignalSpot.spots.clear();
        clientLiveDataManager.stop();
        nodeManager.clear();
        ghostManager.clear();
        saveConfig = null;
        modbusServer = null;
        oreRegenerate.clear();
        delayedTask.clear();
        DelayedBlockRemove.clear();
        serverEventListener.clear();
        nodeServer.stop();
        simulator.stop();
        LampSupplyElement.channelMap.clear();
        WirelessSignalTxElement.channelMap.clear();

    }

    @EventHandler
    public void onServerStart(FMLServerAboutToStartEvent ev) {
        modbusServer = new ModbusTcpServer(modbusPort);
        TeleporterElement.teleporterList.clear();
        LightBlockEntity.observers.clear();
        WirelessSignalTxElement.channelMap.clear();
        LampSupplyElement.channelMap.clear();
        clientLiveDataManager.start();
        simulator.init();
        simulator.addSlowProcess(wind = new WindProcess());

        if (replicatorPop) simulator.addSlowProcess(new ReplicatorPopProcess());
        simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent ev) {
        {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            WorldServer worldServer = server.worldServers[0];
            ghostManagerNbt = (GhostManagerNbt) worldServer.mapStorage.loadData(GhostManagerNbt.class, "GhostManager");
            if (ghostManagerNbt == null) {
                ghostManagerNbt = new GhostManagerNbt("GhostManager");
                worldServer.mapStorage.setData("GhostManager", ghostManagerNbt);
            }
            saveConfig = (SaveConfig) worldServer.mapStorage.loadData(SaveConfig.class, "SaveConfig");
            if (saveConfig == null) {
                saveConfig = new SaveConfig("SaveConfig");
                worldServer.mapStorage.setData("SaveConfig", saveConfig);
            }
            nodeManagerNbt = (NodeManagerNbt) worldServer.mapStorage.loadData(NodeManagerNbt.class, "NodeManager");
            if (nodeManagerNbt == null) {
                nodeManagerNbt = new NodeManagerNbt("NodeManager");
                worldServer.mapStorage.setData("NodeManager", nodeManagerNbt);
            }
            nodeServer.init();
        }
        {
            MinecraftServer s = MinecraftServer.getServer();
            ICommandManager command = s.getCommandManager();
            ServerCommandManager manager = (ServerCommandManager) command;
            manager.registerCommand(new ElnConsoleCommands());
        }
        regenOreScannerFactors();
    }

    public double LVP() {
        return 1000 * cablePowerFactor;
    }
    public double MVP() {
        return 2000 * cablePowerFactor;
    }
    public double HVP() {
        return 5000 * cablePowerFactor;
    }
    public double VVP() {
        return 15000 * cablePowerFactor;
    }

    public void regenOreScannerFactors() {
        OreColorMapping.INSTANCE.updateColorMapping();

        oreScannerConfig.clear();

        if (addOtherModOreToXRay) {
            for (String name : OreDictionary.getOreNames()) {
                if (name == null) continue;
                if (name.startsWith("ore")) {
                    for (ItemStack stack : OreDictionary.getOres(name)) {
                        int id = Utils.getItemId(stack) + 4096 * stack.getItem().getMetadata(stack.getItemDamage());
                        boolean find = false;
                        for (OreScannerConfigElement c : oreScannerConfig) {
                            if (c.getBlockKey() == id) {
                                find = true;
                                break;
                            }
                        }

                        if (!find) {
                            Utils.println(id + " added to xRay (other mod)");
                            oreScannerConfig.add(new OreScannerConfigElement(id, 0.15f));
                        }
                    }
                }
            }
        }

        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.coal_ore), 5 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.iron_ore), 15 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.gold_ore), 40 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.lapis_ore), 40 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.redstone_ore), 40 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.diamond_ore), 100 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.emerald_ore), 40 / 100f));

        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (1 << 12), 10 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (4 << 12), 20 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (5 << 12), 20 / 100f));
        oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(oreBlock) + (6 << 12), 20 / 100f));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void textureHook(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() == 0) {
            for (ElnFluidRegistry name : fluids.keySet()) {
                Block block = fluidBlocks.get(name);
                Fluid fluid = fluids.get(name);
                fluid.setIcons(block.getBlockTextureFromSide(1), block.getBlockTextureFromSide(2));
            }
        }
    }

    public boolean isDevelopmentRun() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }
}

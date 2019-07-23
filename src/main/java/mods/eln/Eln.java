package mods.eln;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import mods.eln.block.ArcClayBlock;
import mods.eln.block.ArcClayItemBlock;
import mods.eln.block.ArcMetalBlock;
import mods.eln.block.ArcMetalItemBlock;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.debug.DP;
import mods.eln.debug.DPType;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.eventhandlers.ElnFMLEventsHandler;
import mods.eln.eventhandlers.ElnForgeEventsHandler;
import mods.eln.generic.GenericCreativeTab;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.SharedItem;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostManager;
import mods.eln.ghost.GhostManagerNbt;
import mods.eln.item.CopperCableDescriptor;
import mods.eln.item.GraphiteDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.TreeResin;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.PortableOreScannerItem;
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
import mods.eln.registry.*;
import mods.eln.server.*;
import mods.eln.sim.ProcessType;
import mods.eln.sim.Simulator;
import mods.eln.sim.thermal.ThermalLoadInitializer;
import mods.eln.sim.mna.passive.Resistor;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static mods.eln.i18n.I18N.*;

@SuppressWarnings({"SameParameterValue"})
@Mod(modid = Eln.MODID, name = Eln.NAME, version = "@VERSION@", acceptedMinecraftVersions = "@VERSION@", acceptableRemoteVersions = "@VERSION@", acceptableSaveVersions = "")
public class Eln {
    // Mod information (override from 'mcmod.info' file)
    public final static String MODID = "Eln";
    public final static String NAME = "Electrical Age";
    public final static String MODDESC = "Electricity in your base!";
    public final static String URL = "https://eln.ja13.org";
    public final static String UPDATE_URL = "https://github.com/jrddunbr/ElectricalAge/releases";
    public final static String SRC_URL = "https://github.com/jrddunbr/ElectricalAge";
    public final static String[] AUTHORS = {"Dolu1990", "lambdaShade", "cm0x4D", "metc", "Baughn", "jrddunbr", "Grissess", "OmegaHaxors"};

    // The instance of your mod that Forge uses.
    @Instance("Eln")
    public static Eln instance;

    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide = "mods.eln.client.ClientProxy", serverSide = "mods.eln.CommonProxy")
    public static CommonProxy proxy;

    /* Network Packet Constants */
    public static final byte PACKET_PLAYER_KEY = 14;
    public static final byte PACKET_NODE_SINGLE_SERIALIZED = 15;
    public static final byte PACKET_PUBLISH_FOR_NODE = 16;
    public static final byte PACKET_OPEN_LOCAL_GUI = 17;
    public static final byte PACKET_FOR_CLIENT_NODE = 18;
    public static final byte PACKET_PLAY_SOUND = 19;
    public static final byte PACKET_STOP_SOUND = 20;
    public static final byte PACKET_CLIENT_TO_SERVER_CONNECTION = 21;
    public static final byte PACKET_SERVER_TO_CLIENT_INFO = 22;
    public static final String NETWORK_CHANNEL_NAME = "miaouMod";

    /* Items */

    public static String dictTungstenOre, dictTungstenDust, dictTungstenIngot;
    public static String dictCheapChip, dictAdvancedChip;
    public static ArrayList<OreScannerConfigElement> oreScannerConfig = new ArrayList<>();

    public static Item swordCopper, hoeCopper, shovelCopper, pickaxeCopper, axeCopper;

    public static ItemArmor helmetCopper, plateCopper, legsCopper, bootsCopper;
    public static ItemArmor helmetECoal, plateECoal, legsECoal, bootsECoal;

    public static SharedItem sharedItem;
    public static SharedItem sharedItemStackOne;
    public static ItemStack wrenchItemStack;

    public static SixNodeItem sixNodeItem;
    public static TransparentNodeItem transparentNodeItem;
    public static OreItem oreItem;

    public static GraphiteDescriptor GraphiteDescriptor;

    /* Blocks */

    public static SixNodeBlock sixNodeBlock;
    public static TransparentNodeBlock transparentNodeBlock;
    public static OreBlock oreBlock;
    public static GhostBlock ghostBlock;
    public static LightBlock lightBlock;
    public static ArcClayBlock arcClayBlock;
    public static ArcMetalBlock arcMetalBlock;

    /* Cables */

    //public ElectricalCableDescriptor creativeCableDescriptor;
    /*public ElectricalCableDescriptor T2TransmissionCableDescriptor;
    public ElectricalCableDescriptor T1TransmissionCableDescriptor;*/
    public static ElectricalCableDescriptor veryHighVoltageCableDescriptor;
    public static ElectricalCableDescriptor highVoltageCableDescriptor;
    public static ElectricalCableDescriptor signalCableDescriptor;
    public static ElectricalCableDescriptor lowVoltageCableDescriptor;
    public static ElectricalCableDescriptor batteryCableDescriptor;
    public static ElectricalCableDescriptor meduimVoltageCableDescriptor;
    public static ElectricalCableDescriptor signalBusCableDescriptor;
    public static CopperCableDescriptor copperCableDescriptor;
    public static CurrentCableDescriptor lowCurrentCableDescriptor;
    public static CurrentCableDescriptor mediumCurrentCableDescriptor;
    public static CurrentCableDescriptor highCurrentCableDescriptor;
    public static CurrentCableDescriptor veryHighCurrentCableDescriptor;
    public static PortableNaNDescriptor portableNaNDescriptor;

    /* Configuration Options */

    public static boolean debugEnabled = false;  // Read from configuration file. Default is `false`.
    public static boolean versionCheckEnabled = true; // Read from configuration file. Default is `true`.
    public static boolean analyticsEnabled = true; // Read from configuration file. Default is `true`.
    public static String analyticsURL = "";
    public static boolean analyticsPlayerUUIDOptIn = false;
    public static String playerUUID = null; // Read from configuration file. Default is `null`.
    public static double heatTurbinePowerFactor = 1;
    public static double solarPanelPowerFactor = 1;
    public static double windTurbinePowerFactor = 1;
    public static double waterTurbinePowerFactor = 1;
    public static double fuelGeneratorPowerFactor = 1;
    public static double fuelHeatFurnacePowerFactor = 1;
    public static int autominerRange = 10;
    public static boolean wailaEasyMode = false;
    public static double shaftEnergyFactor = 0.05;
    public static double fuelHeatValueFactor = 0.0000675;
    public static int plateConversionRatio;
    public static boolean noSymbols = false;
    public static boolean noVoltageBackground = false;
    public static double maxSoundDistance = 16;
    public static double cableFactor;
    public static double stdBatteryHalfLife = 2 * Utils.minecraftDay;
    public static double batteryCapacityFactor = 1.;
    public static boolean killMonstersAroundLamps;
    public static int killMonstersAroundLampsRange;
    public static int maxReplicators = 100;
    public static float xRayScannerRange;
    public static boolean addOtherModOreToXRay;
    public static boolean replicatorPop;
    public static boolean xRayScannerCanBeCrafted = true;
    public static boolean forceOreRegen;
    public static boolean explosionEnable;
    public static boolean modbusEnable = false;
    public static int modbusPort;
    public static double electricalFrequency, thermalFrequency;
    public static int electricalInterSystemOverSampling;
    public static boolean oredictTungsten, oredictChips;
    public static boolean genCopper, genLead, genTungsten, genCinnabar;
    public static double solarPanelBasePower = 65.0;
    public static double incandescentLampLife;
    public static double economicLampLife;
    public static double carbonLampLife;
    public static double ledLampLife;
    public static boolean ledLampInfiniteLife = false;
    public static double cableResistanceMultiplier = 1.0;


    /* Other */

    public static ArrayList<IConfigSharing> configShared = new ArrayList<>();
    public static SimpleNetworkWrapper elnNetwork;
    public static PacketHandler packetHandler;
    private static NodeServer nodeServer;
    public static LiveDataManager clientLiveDataManager;
    public static ClientKeyHandler clientKeyHandler;
    public static SaveConfig saveConfig;
    public static GhostManager ghostManager;
    private static NodeManager nodeManager;
    public static PlayerManager playerManager;
    public static ModbusTcpServer modbusServer;
    public static Simulator simulator = null;
    public static DelayedTaskManager delayedTask;
    public static ItemEnergyInventoryProcess itemEnergyInventoryProcess;
    public static CreativeTabs creativeTab;

    private static OreRegenerate oreRegenerate;
    public static Obj3DFolder obj = new Obj3DFolder();
    public static ArrayList<DPType> debugTypes = new ArrayList<>();
    public static Logger logger;

    public static HashMap<String, Key> keyList;

    public static RecipesList magnetiserRecipes = new RecipesList();
    public static RecipesList compressorRecipes = new RecipesList();
    public static RecipesList plateMachineRecipes = new RecipesList();
    public static RecipesList arcFurnaceRecipes = new RecipesList();
    public static ElectricalFurnaceDescriptor electricalFurnace;
    public static RecipesList maceratorRecipes = new RecipesList();
    public static FunctionTable batteryVoltageFunctionTable;
    public static CableRenderDescriptor stdCableRenderSignal;
    public static CableRenderDescriptor stdCableRenderSignalBus;
    public static CableRenderDescriptor stdCableRender50V;
    public static CableRenderDescriptor stdCableRender200V;
    public static CableRenderDescriptor stdCableRender800V;
    public static CableRenderDescriptor stdCableRender3200V;

    public static CableRenderDescriptor stdCableRenderLowCurrent;
    public static CableRenderDescriptor stdCableRenderMediumCurrent;
    public static CableRenderDescriptor stdCableRenderHighCurrent;
    public static CableRenderDescriptor stdCableRenderVeryHighCurrent;

    public static CableRenderDescriptor stdPortableNaN;

    public static double gateOutputCurrent = 0.100;
    public static double SVU = 50, SVII = gateOutputCurrent / 50, SVUinv = 1.0 / SVU;
    public static double LVU = 50;
    public static double MVU = 200;
    public static double HVU = 800;
    public static double VVU = 3200;

    public static double SVP = gateOutputCurrent * SVU;

    public static double LVP() {
        return 1000 * cableFactor;
    }
    public static double MVP() {
        return 2000 * cableFactor;
    }
    public static double HVP() {
        return 5000 * cableFactor;
    }
    public static double VVP() {
        return 15000 * cableFactor;
    }

    public static double cableHeatingTime = 30;
    public static double cableWarmLimit = 130;
    public static double cableThermalConductionTao = 0.5;
    public static ThermalLoadInitializer cableThermalLoadInitializer = new ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, cableThermalConductionTao);
    public static ThermalLoadInitializer sixNodeThermalLoadInitializer = new ThermalLoadInitializer(
        cableWarmLimit, -100, cableHeatingTime, 1000);
    public static int wirelessTxRange = 32;
    public static WindProcess wind;
    public static ServerEventListener serverEventListener;
    public static FMLEventChannel eventChannel;
    public static boolean ComputerProbeEnable;
    public static boolean ElnToOtherEnergyConverterEnable;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockLvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockMvu;
    public static EnergyConverterElnToOtherBlock elnToOtherBlockHvu;
    public static ComputerProbeBlock computerProbeBlock;

    public static TreeResin treeResin;
    public static MiningPipeDescriptor miningPipeDescriptor;
    public static DataLogsPrintDescriptor dataLogsPrintDescriptor;
    public static GenericItemUsingDamageDescriptor multiMeterElement, thermometerElement, allMeterElement;
    public static GenericItemUsingDamageDescriptor configCopyToolElement;
    public static int replicatorRegistrationId = -1;
    public static double fuelGeneratorTankCapacity = 20 * 60;
    public static GenericItemUsingDamageDescriptorWithComment tinIngot, copperIngot, silverIngot, plumbIngot, tungstenIngot;
    public static GenericItemUsingDamageDescriptorWithComment dustTin, dustCopper, dustSilver;
    public static OreDescriptor oreTin, oreCopper, oreSilver;
    public static HashMap<String, ItemStack> dictionnaryOreFromMod = new HashMap<String, ItemStack>();
    public static ArrayList<ItemStack> furnaceList = new ArrayList<>();
    public static int energyMeterWebhookFrequency;

    @EventHandler
    @SuppressWarnings("unused")
    public void preInit(FMLPreInitializationEvent event) {

        // The configuration handler will load all of the configuration variables and set them in Eln.java for you.
        ConfigHandler.loadConfig(event);

        // load up the debug printer before anything else starts.
        DP.properInit(debugTypes, LogManager.getLogger(Eln.MODID));

        // register keys on the client and the server. The order of the registration is important.
        Eln.keyList = new HashMap<>();
        proxy.registerKey("Wrench", Keyboard.KEY_C);

        elnNetwork = NetworkRegistry.INSTANCE.newSimpleChannel("electrical-age");
        elnNetwork.registerMessage(AchievePacketHandler.class, AchievePacket.class, 0, Side.SERVER);
        elnNetwork.registerMessage(TransparentNodeRequestPacketHandler.class, TransparentNodeRequestPacket.class, 1, Side.SERVER);
        elnNetwork.registerMessage(TransparentNodeResponsePacketHandler.class, TransparentNodeResponsePacket.class, 2, Side.CLIENT);
        elnNetwork.registerMessage(GhostNodeWailaRequestPacketHandler.class, GhostNodeWailaRequestPacket.class, 3, Side.SERVER);
        elnNetwork.registerMessage(GhostNodeWailaResponsePacketHandler.class, GhostNodeWailaResponsePacket.class, 4, Side.CLIENT);
        elnNetwork.registerMessage(SixNodeWailaRequestPacketHandler.class, SixNodeWailaRequestPacket.class, 5, Side.SERVER);
        elnNetwork.registerMessage(SixNodeWailaResponsePacketHandler.class, SixNodeWailaResponsePacket.class, 6, Side.CLIENT);

        // ModContainer container = FMLCommonHandler.instance().findContainerFor(this);
        // LanguageRegistry.instance().loadLanguagesFor(container, Side.CLIENT);

        // Update ModInfo by code
        ModMetadata meta = event.getModMetadata();
        meta.modId = MODID;
        meta.version = Version.getVersionName();
        meta.name = NAME;
        meta.description = tr("mod.meta.desc");
        meta.url = URL;
        meta.updateUrl = UPDATE_URL;
        meta.authorList = asList(AUTHORS);
        meta.autogenerated = false; // Force to update from code

        DP.println(DPType.OTHER, Version.print());

        eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(NETWORK_CHANNEL_NAME);

        simulator = new Simulator(0.05, 1 / electricalFrequency, electricalInterSystemOverSampling, 1 / thermalFrequency);
        nodeManager = new NodeManager("caca");
        ghostManager = new GhostManager("caca2");
        delayedTask = new DelayedTaskManager();

        playerManager = new PlayerManager();
        //tileEntityDestructor = new TileEntityDestructor();

        oreRegenerate = new OreRegenerate();
        nodeServer = new NodeServer();
        clientLiveDataManager = new LiveDataManager();

        packetHandler = new PacketHandler();
        // ForgeDummyContainer
        instance = this;

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        Item itemCreativeTab = new Item()
            .setUnlocalizedName("eln:elncreativetab")
            .setTextureName("eln:elncreativetab");
        GameRegistry.registerItem(itemCreativeTab, "eln.itemCreativeTab");
        creativeTab = new GenericCreativeTab("Eln", itemCreativeTab);

        oreBlock = (OreBlock) new OreBlock().setCreativeTab(creativeTab).setBlockName("OreEln");

        arcClayBlock = new ArcClayBlock();
        arcMetalBlock = new ArcMetalBlock();

        sharedItem = (SharedItem) new SharedItem()
            .setCreativeTab(creativeTab).setMaxStackSize(64)
            .setUnlocalizedName("sharedItem");

        sharedItemStackOne = (SharedItem) new SharedItem()
            .setCreativeTab(creativeTab).setMaxStackSize(1)
            .setUnlocalizedName("sharedItemStackOne");

        transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(
            Material.iron,
            TransparentNodeEntity.class)
            .setCreativeTab(creativeTab)
            .setBlockTextureName("iron_block");
        sixNodeBlock = (SixNodeBlock) new SixNodeBlock(
            Material.plants, SixNodeEntity.class)
            .setCreativeTab(creativeTab)
            .setBlockTextureName("iron_block");

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
        // TileEntity.addMapping(TransparentNodeEntityWithSiededInv.class, "TransparentNodeEntityWSI");
        TileEntity.addMapping(SixNodeEntity.class, "SixNodeEntity");
        TileEntity.addMapping(LightBlockEntity.class, "LightBlockEntity");

        NodeManager.registerUuid(sixNodeBlock.getNodeUuid(), SixNode.class);
        NodeManager.registerUuid(transparentNodeBlock.getNodeUuid(), TransparentNode.class);

        sixNodeItem = (SixNodeItem) Item.getItemFromBlock(sixNodeBlock);
        transparentNodeItem = (TransparentNodeItem) Item.getItemFromBlock(transparentNodeBlock);

        oreItem = (OreItem) Item.getItemFromBlock(oreBlock);

        SixNode.sixNodeCacheList.add(new SixNodeCacheStd());

        // Register all items and blocks
        SixNodeRegistry.thingRegistration();
        TransparentNodeRegistry.thingRegistration();
        ItemRegistry.thingRegistration();
        MscRegistry.thingRegistration();

        if (isDevelopmentRun()) {
            SixNodeRegistry.registerPortableNaN();
        }

        OreDictionary.registerOre("blockAluminum", arcClayBlock);
        OreDictionary.registerOre("blockSteel", arcMetalBlock);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void modsLoaded(FMLPostInitializationEvent event) {
        Other.check();
        if (Other.ccLoaded) {
            PeripheralHandler.register();
        }
        recipeMaceratorModOres();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void load(FMLInitializationEvent event) {

        // Register Recipes
        SixNodeRegistry.recipeRegistration();
        TransparentNodeRegistry.recipeRegistration();
        ItemRegistry.recipeRegistration();
        MscRegistry.recipeRegistration();

        // Register Entities
        MscRegistry.entityRegistration();

        proxy.registerRenderers();
        TR("itemGroup.Eln");
        RegistryUtils.checkRecipe();
        Achievements.init();
        MinecraftForge.EVENT_BUS.register(new ElnForgeEventsHandler());
        FMLCommonHandler.instance().bus().register(new ElnFMLEventsHandler());
        FMLInterModComms.sendMessage("Waila", "register", "mods.eln.integration.waila.WailaIntegration.callbackRegister");
        DP.println(DPType.OTHER, "Electrical age init done");
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void postInit(FMLPostInitializationEvent event) {
        serverEventListener = new ServerEventListener();
    }

    @EventHandler
    @SuppressWarnings("unused")
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
        playerManager.clear();

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
    @SuppressWarnings("unused")
    public void onServerStart(FMLServerAboutToStartEvent ev) {
        modbusServer = new ModbusTcpServer(modbusPort);
        TeleporterElement.teleporterList.clear();
        //tileEntityDestructor.clear();
        LightBlockEntity.observers.clear();
        WirelessSignalTxElement.channelMap.clear();
        LampSupplyElement.channelMap.clear();
        playerManager.clear();
        clientLiveDataManager.start();
        simulator.init();
        simulator.addProcess(ProcessType.SlowProcess, wind = new WindProcess());

        if (replicatorPop)
            simulator.addProcess(ProcessType.SlowProcess, new ReplicatorPopProcess());
        simulator.addProcess(ProcessType.SlowProcess, itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onServerStarting(FMLServerStartingEvent ev) {

        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        WorldServer worldServer = server.worldServers[0];


        GhostManagerNbt ghostManagerNbt = (GhostManagerNbt) worldServer.mapStorage.loadData(
            GhostManagerNbt.class, "GhostManager");
        if (ghostManagerNbt == null) {
            ghostManagerNbt = new GhostManagerNbt("GhostManager");
            worldServer.mapStorage.setData("GhostManager", ghostManagerNbt);
        }

        saveConfig = (SaveConfig) worldServer.mapStorage.loadData(
            SaveConfig.class, "SaveConfig");
        if (saveConfig == null) {
            saveConfig = new SaveConfig("SaveConfig");
            worldServer.mapStorage.setData("SaveConfig", saveConfig);
        }

        NodeManagerNbt nodeManagerNbt = (NodeManagerNbt) worldServer.mapStorage.loadData(
            NodeManagerNbt.class, "NodeManager");
        if (nodeManagerNbt == null) {
            nodeManagerNbt = new NodeManagerNbt("NodeManager");
            worldServer.mapStorage.setData("NodeManager", nodeManagerNbt);
        }

        nodeServer.init();

        ServerCommandManager manager = (ServerCommandManager) MinecraftServer.getServer().getCommandManager();
        manager.registerCommand(new ConsoleListener());

        regenOreScannerFactors();
    }

    private void recipeMaceratorModOres() {
        float f = 4000;
        // AE2:
        RegistryUtils.recipeMaceratorModOre(f * 3f, "oreCertusQuartz", "dustCertusQuartz", 3);
        RegistryUtils.recipeMaceratorModOre(f * 1.5f, "crystalCertusQuartz", "dustCertusQuartz", 1);
        RegistryUtils.recipeMaceratorModOre(f * 3f, "oreNetherQuartz", "dustNetherQuartz", 3);
        RegistryUtils.recipeMaceratorModOre(f * 1.5f, "crystalNetherQuartz", "dustNetherQuartz", 1);
        RegistryUtils.recipeMaceratorModOre(f * 1.5f, "crystalFluix", "dustFluix", 1);
    }

    public void regenOreScannerFactors() {
        PortableOreScannerItem.RenderStorage.blockKeyFactor = null;
        oreScannerConfig.clear();
        if (addOtherModOreToXRay) {
            for (String name : OreDictionary.getOreNames()) {
                if (name == null)
                    continue;
                if (name.startsWith("ore")) {
                    for (ItemStack stack : OreDictionary.getOres(name)) {
                        int id = Utils.getItemId(stack) + 4096 * stack.getItem().getMetadata(stack.getItemDamage());
                        // Utils.println(OreDictionary.getOreID(name));
                        boolean find = false;
                        for (OreScannerConfigElement c : oreScannerConfig) {
                            if (c.blockKey == id) {
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            DP.println(DPType.OTHER, id + " added to xRay (other mod)");
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

    public static double getSmallRs() {
        return lowVoltageCableDescriptor.electricalRs;
    }

    public static void applySmallRs(NbtElectricalLoad aLoad) {
         lowVoltageCableDescriptor.applyTo(aLoad);
    }

    public static void applySmallRs(Resistor r) {
        lowVoltageCableDescriptor.applyTo(r);
    }

    @SuppressWarnings("unused")
    private boolean isDevelopmentRun() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }
}

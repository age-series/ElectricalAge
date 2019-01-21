package mods.eln;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import mods.eln.block.ArcClayBlock;
import mods.eln.block.ArcClayItemBlock;
import mods.eln.block.ArcMetalBlock;
import mods.eln.block.ArcMetalItemBlock;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientKeyHandler;
import mods.eln.client.SoundLoader;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorPopProcess;
import mods.eln.eventhandlers.ElnFMLEventsHandler;
import mods.eln.eventhandlers.ElnForgeEventsHandler;
import mods.eln.generic.*;
import mods.eln.generic.genericArmorItem.ArmourType;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostGroup;
import mods.eln.ghost.GhostManager;
import mods.eln.ghost.GhostManagerNbt;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.gridnode.transformer.GridTransformerDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.*;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.*;
import mods.eln.item.electricalitem.PortableOreScannerItem.RenderStorage.OreScannerConfigElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.mechanical.*;
import mods.eln.misc.*;
import mods.eln.misc.series.SerieEE;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.node.NodeManagerNbt;
import mods.eln.node.NodeServer;
import mods.eln.node.simple.SimpleNodeItem;
import mods.eln.node.six.*;
import mods.eln.node.transparent.*;
import mods.eln.ore.OreBlock;
import mods.eln.ore.OreDescriptor;
import mods.eln.ore.OreItem;
import mods.eln.packets.*;
import mods.eln.registration.*;
import mods.eln.server.*;
import mods.eln.signalinductor.SignalInductorDescriptor;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.simplenode.computerprobe.ComputerProbeBlock;
import mods.eln.simplenode.computerprobe.ComputerProbeEntity;
import mods.eln.simplenode.computerprobe.ComputerProbeNode;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.ElnDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.Ic2Descriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor.OcDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import mods.eln.simplenode.test.TestBlock;
import mods.eln.sixnode.*;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.batterycharger.BatteryChargerDescriptor;
import mods.eln.sixnode.diode.DiodeDescriptor;
import mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor;
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor;
import mods.eln.sixnode.electricaldigitaldisplay.ElectricalDigitalDisplayDescriptor;
import mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor;
import mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor;
import mods.eln.sixnode.electricalmath.ElectricalMathDescriptor;
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor;
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor;
import mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor;
import mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor;
import mods.eln.sixnode.electricalswitch.ElectricalSwitchDescriptor;
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor;
import mods.eln.sixnode.electricalvumeter.ElectricalVuMeterDescriptor;
import mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor;
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor;
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor;
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor;
import mods.eln.sixnode.groundcable.GroundCableDescriptor;
import mods.eln.sixnode.hub.HubDescriptor;
import mods.eln.sixnode.lampsocket.*;
import mods.eln.sixnode.lampsupply.LampSupplyDescriptor;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import mods.eln.sixnode.logicgate.*;
import mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor;
import mods.eln.sixnode.modbusrtu.ModbusTcpServer;
import mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor;
import mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor;
import mods.eln.sixnode.powersocket.PowerSocketDescriptor;
import mods.eln.sixnode.powersocket.PowerSocketElement;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignElement;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor;
import mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterDescriptor;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor;
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import mods.eln.sound.SoundCommand;
import mods.eln.transparentnode.FuelGeneratorDescriptor;
import mods.eln.transparentnode.FuelHeatFurnaceDescriptor;
import mods.eln.transparentnode.LargeRheostatDescriptor;
import mods.eln.transparentnode.NixieTubeDescriptor;
import mods.eln.transparentnode.autominer.AutoMinerDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.computercraftio.PeripheralHandler;
import mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor;
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.ArcFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.CompressorDescriptor;
import mods.eln.transparentnode.electricalmachine.MaceratorDescriptor;
import mods.eln.transparentnode.electricalmachine.MagnetizerDescriptor;
import mods.eln.transparentnode.electricalmachine.PlateMachineDescriptor;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor;
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor;
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor;
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterElement;
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.transparentnode.transformer.TransformerDescriptor;
import mods.eln.transparentnode.turbine.TurbineDescriptor;
import mods.eln.transparentnode.turret.TurretDescriptor;
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor;
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.*;

import static mods.eln.i18n.I18N.*;

@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression"})
@Mod(modid = Eln.MODID, name = Eln.NAME, version = "@VERSION@")
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



    // Stuff that is needed in Eln.java for things to work
    public static final ConfigHandler CONFIG_HANDLER = new ConfigHandler();

    public static final SixNodeRegistration SIX_NODE_REGISTRATION = new SixNodeRegistration();
    public static final TransparentNodeRegistration TRANSPARENT_NODE_REGISTRATION = new TransparentNodeRegistration();
    public static final BlockRegistration BLOCK_REGISTRATION = new BlockRegistration();
    public static final ItemRegistration ITEM_REGISTRATION = new ItemRegistration();
    public static final EntityRegistration ENTITY_REGISTRATION = new EntityRegistration();
    public static final RecipeRegistration RECIPE_REGISTRATION = new RecipeRegistration();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        // Update ModInfo by code
        ModMetadata meta = event.getModMetadata();
        meta.modId = MODID;
        meta.version = Version.getVersionName();
        meta.name = NAME;
        meta.description = tr("mod.meta.desc");
        meta.url = URL;
        meta.updateUrl = UPDATE_URL;
        meta.authorList = Arrays.asList(AUTHORS);
        meta.autogenerated = false; // Force to update from code

        CONFIG_HANDLER.readConfig(event);

        Vars.elnNetwork = NetworkRegistry.INSTANCE.newSimpleChannel("electrical-age");
        Vars.elnNetwork.registerMessage(AchievePacketHandler.class, AchievePacket.class, 0, Side.SERVER);
        Vars.elnNetwork.registerMessage(TransparentNodeRequestPacketHandler.class, TransparentNodeRequestPacket.class, 1, Side.SERVER);
        Vars.elnNetwork.registerMessage(TransparentNodeResponsePacketHandler.class, TransparentNodeResponsePacket.class, 2, Side.CLIENT);
        Vars.elnNetwork.registerMessage(GhostNodeWailaRequestPacketHandler.class, GhostNodeWailaRequestPacket.class, 3, Side.SERVER);
        Vars.elnNetwork.registerMessage(GhostNodeWailaResponsePacketHandler.class, GhostNodeWailaResponsePacket.class, 4, Side.CLIENT);
        Vars.elnNetwork.registerMessage(SixNodeWailaRequestPacketHandler.class, SixNodeWailaRequestPacket.class, 5, Side.SERVER);
        Vars.elnNetwork.registerMessage(SixNodeWailaResponsePacketHandler.class, SixNodeWailaResponsePacket.class, 6, Side.CLIENT);

        ModContainer container = FMLCommonHandler.instance().findContainerFor(this);
        // LanguageRegistry.instance().loadLanguagesFor(container, Side.CLIENT);

        Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (side == Side.CLIENT)
            MinecraftForge.EVENT_BUS.register(new SoundLoader());

        Vars.eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(Vars.channelName);

        Vars.simulator = new Simulator(0.05, 1 / Vars.electricalFrequency, Vars.electricalInterSystemOverSampling, 1 / Vars.thermalFrequency);
        Vars.nodeManager = new NodeManager("caca");
        Vars.ghostManager = new GhostManager("caca2");
        Vars.delayedTask = new DelayedTaskManager();

        Vars.playerManager = new PlayerManager();

        Vars.oreRegenerate = new OreRegenerate();
        Vars.nodeServer = new NodeServer();
        Vars.clientLiveDataManager = new LiveDataManager();

        Vars.packetHandler = new PacketHandler();
        // ForgeDummyContainer
        instance = this;

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        Item itemCreativeTab = new Item()
            .setUnlocalizedName("eln:elncreativetab")
            .setTextureName("eln:elncreativetab");
        GameRegistry.registerItem(itemCreativeTab, "eln.itemCreativeTab");
        Vars.creativeTab = new GenericCreativeTab("Eln", itemCreativeTab);

        Vars.oreBlock = (OreBlock) new OreBlock().setCreativeTab(Vars.creativeTab).setBlockName("OreEln");

        Vars.arcClayBlock = new ArcClayBlock();
        Vars.arcMetalBlock = new ArcMetalBlock();

        Vars.sharedItem = (SharedItem) new SharedItem()
            .setCreativeTab(Vars.creativeTab).setMaxStackSize(64)
            .setUnlocalizedName("sharedItem");

        Vars.sharedItemStackOne = (SharedItem) new SharedItem()
            .setCreativeTab(Vars.creativeTab).setMaxStackSize(1)
            .setUnlocalizedName("sharedItemStackOne");

        Vars.transparentNodeBlock = (TransparentNodeBlock) new TransparentNodeBlock(
            Material.iron,
            TransparentNodeEntity.class)
            .setCreativeTab(Vars.creativeTab)
            .setBlockTextureName("iron_block");
        Vars.sixNodeBlock = (SixNodeBlock) new SixNodeBlock(
            Material.plants, SixNodeEntity.class)
            .setCreativeTab(Vars.creativeTab)
            .setBlockTextureName("iron_block");

        Vars.ghostBlock = (GhostBlock) new GhostBlock().setBlockTextureName("iron_block");
        Vars.lightBlock = new LightBlock();

        Vars.obj.loadAllElnModels();

        NodeManager.registerUuid(Vars.sixNodeBlock.getNodeUuid(), SixNode.class);
        NodeManager.registerUuid(Vars.transparentNodeBlock.getNodeUuid(), TransparentNode.class);

        Vars.sixNodeItem = (SixNodeItem) Item.getItemFromBlock(Vars.sixNodeBlock);
        Vars.transparentNodeItem = (TransparentNodeItem) Item.getItemFromBlock(Vars.transparentNodeBlock);
        Vars.oreItem = (OreItem) Item.getItemFromBlock(Vars.oreBlock);

        SixNode.sixNodeCacheList.add(new SixNodeCacheStd());

        // TODO: REGISTER ALL BLOCKS/ITEMS/SHIT HERE

        OreDictionary.registerOre("blockAluminum", Vars.arcClayBlock);
        OreDictionary.registerOre("blockSteel", Vars.arcMetalBlock);

        GameRegistry.registerItem(Vars.sharedItem, "Eln.sharedItem");
        GameRegistry.registerItem(Vars.sharedItemStackOne, "Eln.sharedItemStackOne");
        GameRegistry.registerBlock(Vars.ghostBlock, "Eln.ghostBlock");
        GameRegistry.registerBlock(Vars.lightBlock, "Eln.lightBlock");
        GameRegistry.registerBlock(Vars.sixNodeBlock, SixNodeItem.class, "Eln.SixNode");
        GameRegistry.registerBlock(Vars.transparentNodeBlock, TransparentNodeItem.class, "Eln.TransparentNode");
        GameRegistry.registerBlock(Vars.oreBlock, OreItem.class, "Eln.Ore");
        GameRegistry.registerBlock(Vars.arcClayBlock, ArcClayItemBlock.class, "Eln.arc_clay_block");
        GameRegistry.registerBlock(Vars.arcMetalBlock, ArcMetalItemBlock.class, "Eln.arc_metal_block");
        TileEntity.addMapping(TransparentNodeEntity.class, "TransparentNodeEntity");
        TileEntity.addMapping(TransparentNodeEntityWithFluid.class, "TransparentNodeEntityWF");
        // TileEntity.addMapping(TransparentNodeEntityWithSiededInv.class, "TransparentNodeEntityWSI");
        TileEntity.addMapping(SixNodeEntity.class, "SixNodeEntity");
        TileEntity.addMapping(LightBlockEntity.class, "LightBlockEntity");
    }

    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        Vars.check();
        if (Vars.ccLoaded) {
            PeripheralHandler.register();
        }
        Eln.RECIPE_REGISTRATION.recipeMaceratorModOres();
    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        Vars.oreNames = new HashSet<String>();
        {
            final String[] names = OreDictionary.getOreNames();
            Collections.addAll(Vars.oreNames, names);
        }
        ENTITY_REGISTRATION.registerEntities();
        proxy.registerRenderers();
        TR("itemGroup.Eln");
        RECIPE_REGISTRATION.checkRecipe();
        if (isDevelopmentRun()) {
            Achievements.init();
        }
        MinecraftForge.EVENT_BUS.register(new ElnForgeEventsHandler());
        FMLCommonHandler.instance().bus().register(new ElnFMLEventsHandler());
        FMLInterModComms.sendMessage("Waila", "register", "mods.eln.integration.waila.WailaIntegration.callbackRegister");
        Utils.println("Electrical age " + Version.getVersionName() + " init done");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Vars.serverEventListener = new ServerEventListener();
    }

    @EventHandler
    /* Remember to use the right event! */
    public void onServerStopped(FMLServerStoppedEvent ev) {
        TutorialSignElement.resetBalise();
        if (Vars.modbusServer != null) {
            Vars.modbusServer.destroy();
            Vars.modbusServer = null;
        }
        LightBlockEntity.observers.clear();
        NodeBlockEntity.clientList.clear();
        TeleporterElement.teleporterList.clear();
        IWirelessSignalSpot.spots.clear();
        Vars.playerManager.clear();
        Vars.clientLiveDataManager.stop();
        Vars.nodeManager.clear();
        Vars.ghostManager.clear();
        Vars.saveConfig = null;
        Vars.modbusServer = null;
        Vars.oreRegenerate.clear();
        Vars.delayedTask.clear();
        DelayedBlockRemove.clear();
        Vars.serverEventListener.clear();
        Vars.nodeServer.stop();
        Vars.simulator.stop();
        LampSupplyElement.channelMap.clear();
        WirelessSignalTxElement.channelMap.clear();

    }

    public static WindProcess wind;

    @EventHandler
    public void onServerStart(FMLServerAboutToStartEvent ev) {
        Vars.modbusServer = new ModbusTcpServer(Vars.modbusPort);
        TeleporterElement.teleporterList.clear();
        //tileEntityDestructor.clear();
        LightBlockEntity.observers.clear();
        WirelessSignalTxElement.channelMap.clear();
        LampSupplyElement.channelMap.clear();
        Vars.playerManager.clear();
        Vars.clientLiveDataManager.start();
        Vars.simulator.init();
        Vars.simulator.addSlowProcess(wind = new WindProcess());

        if (Vars.replicatorPop)
            Vars.simulator.addSlowProcess(new ReplicatorPopProcess());
        Vars.simulator.addSlowProcess(Vars.itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @EventHandler
    /* Remember to use the right event! */
    public void onServerStarting(FMLServerStartingEvent ev) {

        {
            MinecraftServer server = FMLCommonHandler.instance()
                .getMinecraftServerInstance();
            WorldServer worldServer = server.worldServers[0];


            Vars.ghostManagerNbt = (GhostManagerNbt) worldServer.mapStorage.loadData(
                GhostManagerNbt.class, "GhostManager");
            if (Vars.ghostManagerNbt == null) {
                Vars.ghostManagerNbt = new GhostManagerNbt("GhostManager");
                worldServer.mapStorage.setData("GhostManager", Vars.ghostManagerNbt);
            }

            Vars.saveConfig = (SaveConfig) worldServer.mapStorage.loadData(
                SaveConfig.class, "SaveConfig");
            if (Vars.saveConfig == null) {
                Vars.saveConfig = new SaveConfig("SaveConfig");
                worldServer.mapStorage.setData("SaveConfig", Vars.saveConfig);
            }
            // saveConfig.init();

            Vars.nodeManagerNbt = (NodeManagerNbt) worldServer.mapStorage.loadData(
                NodeManagerNbt.class, "NodeManager");
            if (Vars.nodeManagerNbt == null) {
                Vars.nodeManagerNbt = new NodeManagerNbt("NodeManager");
                worldServer.mapStorage.setData("NodeManager", Vars.nodeManagerNbt);
            }

            Vars.nodeServer.init();
        }

        {
            MinecraftServer s = MinecraftServer.getServer();
            ICommandManager command = s.getCommandManager();
            ServerCommandManager manager = (ServerCommandManager) command;
            manager.registerCommand(new ConsoleListener());
        }

        regenOreScannerFactors();
    }

    public void regenOreScannerFactors() {
        PortableOreScannerItem.RenderStorage.blockKeyFactor = null;

        Vars.oreScannerConfig.clear();

        if (Vars.addOtherModOreToXRay) {
            for (String name : OreDictionary.getOreNames()) {
                if (name == null)
                    continue;
                // Utils.println(name + " " +
                // OreDictionary.getOreID(name));
                if (name.startsWith("ore")) {
                    for (ItemStack stack : OreDictionary.getOres(name)) {
                        int id = Utils.getItemId(stack) + 4096 * stack.getItem().getMetadata(stack.getItemDamage());
                        // Utils.println(OreDictionary.getOreID(name));
                        boolean find = false;
                        for (OreScannerConfigElement c : Vars.oreScannerConfig) {
                            if (c.blockKey == id) {
                                find = true;
                                break;
                            }
                        }

                        if (!find) {
                            Utils.println(id + " added to xRay (other mod)");
                            Vars.oreScannerConfig.add(new OreScannerConfigElement(id, 0.15f));
                        }
                    }
                }
            }
        }

        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.coal_ore), 5 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.iron_ore), 15 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.gold_ore), 40 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.lapis_ore), 40 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.redstone_ore), 40 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.diamond_ore), 100 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Blocks.emerald_ore), 40 / 100f));

        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Vars.oreBlock) + (1 << 12), 10 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Vars.oreBlock) + (4 << 12), 20 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Vars.oreBlock) + (5 << 12), 20 / 100f));
        Vars.oreScannerConfig.add(new OreScannerConfigElement(Block.getIdFromBlock(Vars.oreBlock) + (6 << 12), 20 / 100f));
    }

    public static double getSmallRs() {
        return Vars.lowVoltageCableDescriptor.electricalRs;
    }

    public static void applySmallRs(NbtElectricalLoad aLoad) {
        Vars.lowVoltageCableDescriptor.applyTo(aLoad);
    }

    public static void applySmallRs(Resistor r) {
        Vars.lowVoltageCableDescriptor.applyTo(r);
    }

    private boolean isDevelopmentRun() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }
}

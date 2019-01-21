package mods.eln.node;

import cpw.mods.fml.common.FMLCommonHandler;
import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.ghost.GhostBlock;
import mods.eln.misc.*;
import mods.eln.node.six.SixNode;
import mods.eln.sim.*;
import mods.eln.sound.SoundCommand;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class NodeBase {

    public static final int maskElectricalPower = 1 << 0;
    public static final int maskThermal = 1 << 1;

    public static final int maskElectricalGate = (1 << 2);
    public static final int maskElectricalAll = maskElectricalPower | maskElectricalGate;

    public static final int maskElectricalInputGate = maskElectricalGate;
    public static final int maskElectricalOutputGate = maskElectricalGate;

    public static final int maskWire = 0;
    public static final int maskElectricalWire = (1 << 3);
    public static final int maskThermalWire = maskWire + maskThermal;

    public static final int maskSignal = (1 << 9);
    public static final int maskRs485 = (1 << 10);

    public static final int maskSignalBus = (1 << 11);

    public static final int maskColorData = 0xF << 16;
    public static final int maskColorShift = 16;
    public static final int maskColorCareShift = 20;
    public static final int maskColorCareData = 1 << 20;

    public static final double networkSerializeUFactor = 10.0;
    public static final double networkSerializeIFactor = 100.0;
    public static final double networkSerializeTFactor = 10.0;

    public byte neighborOpaque;
    public byte neighborWrapable;

    public static int teststatic;

    public Coordonate coordonate;

    public ArrayList<NodeConnection> nodeConnectionList = new ArrayList<NodeConnection>(4);

    private boolean initialized = false;

    private boolean isAdded = false;

    private boolean needPublish = false;

    // public static boolean canBePlacedOn(ItemStack itemStack,Direction side)

    public boolean mustBeSaved() {
        return true;
    }

    public int getBlockMetadata() {
        return 0;
    }

    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {

    }

    public void notifyNeighbor() {
        coordonate.world().notifyBlockChange(coordonate.x, coordonate.y, coordonate.z, coordonate.getBlock());
    }

    //public abstract Block getBlock();
    public abstract String getNodeUuid();

    public LRDUCubeMask lrduCubeMask = new LRDUCubeMask();

    public void neighborBlockRead() {
        int[] vector = new int[3];
        World world = coordonate.world();

        neighborOpaque = 0;
        neighborWrapable = 0;
        for (Direction direction : Direction.values()) {
            vector[0] = coordonate.x;
            vector[1] = coordonate.y;
            vector[2] = coordonate.z;

            direction.applyTo(vector, 1);

            Block b = world.getBlock(vector[0], vector[1], vector[2]);
            if (b.isOpaqueCube())
                ;
            neighborOpaque |= 1 << direction.getInt();
            if (isBlockWrappable(b, world, coordonate.x, coordonate.y, coordonate.z))
                neighborWrapable |= 1 << direction.getInt();
        }
    }

    public boolean hasGui(Direction side) {
        return false;
    }

    public void onNeighborBlockChange() {
        neighborBlockRead();
        if (isAdded) {
            reconnect();
        }
    }

    public boolean isBlockWrappable(Direction direction) {
        return ((neighborWrapable >> direction.getInt()) & 1) != 0;
    }

    public boolean isBlockOpaque(Direction direction) {
        return ((neighborOpaque >> direction.getInt()) & 1) != 0;
    }

    public static boolean isBlockWrappable(Block block, World w, int x, int y, int z) {
        if (block.isReplaceable(w, x, y, z)) return true;
        if (block == Blocks.air) return true;
        if (block == Eln.sixNodeBlock) return true;
        if (block instanceof GhostBlock) return true;
        if (block == Blocks.torch) return true;
        if (block == Blocks.redstone_torch) return true;
        if (block == Blocks.unlit_redstone_torch) return true;
        if (block == Blocks.redstone_wire) return true;

        return false;
    }

    public NodeBase() {
        coordonate = new Coordonate();
    }

    boolean destructed = false;

    public boolean isDestructing() {
        return destructed;
    }

    public void physicalSelfDestruction(float explosionStrength) {
        if (destructed == true) return;
        destructed = true;
        if (Eln.instance.explosionEnable == false) explosionStrength = 0;
        disconnect();
        coordonate.world().setBlockToAir(coordonate.x, coordonate.y, coordonate.z);
        NodeManager.instance.removeNode(this);
        if (explosionStrength != 0) {
            coordonate.world().createExplosion((Entity) null, coordonate.x, coordonate.y, coordonate.z, explosionStrength, true);
        }
    }

    // NodeBaseTodo
    public void onBlockPlacedBy(Coordonate coordonate, Direction front, EntityLivingBase entityLiving, ItemStack itemStack) {
        // this.entity = entity;
        this.coordonate = coordonate;
        neighborBlockRead();
        NodeManager.instance.addNode(this);

        initializeFromThat(front, entityLiving, itemStack);

        if (itemStack != null)
            Utils.println("Node::constructor( meta = " + itemStack.getItemDamage() + ")");
    }

    abstract public void initializeFromThat(Direction front,
                                            EntityLivingBase entityLiving, ItemStack itemStack);

    public NodeBase getNeighbor(Direction direction) {
        int[] position = new int[3];
        position[0] = coordonate.x;
        position[1] = coordonate.y;
        position[2] = coordonate.z;
        direction.applyTo(position, 1);
        Coordonate nodeCoordonate = new Coordonate(position[0], position[1], position[2], coordonate.dimention);
        return NodeManager.instance.getNodeFromCoordonate(nodeCoordonate);
    }

    // leaf
    public void onBreakBlock() {
        destructed = true;
        disconnect();
        NodeManager.instance.removeNode(this);
        Utils.println("Node::onBreakBlock()");
    }

    public static SoundCommand beepUploaded = new SoundCommand("eln:beep_accept_2").smallRange();
    public static SoundCommand beepDownloaded = new SoundCommand("eln:beep_accept").smallRange();
    public static SoundCommand beepError = new SoundCommand("eln:beep_error").smallRange();

    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (!entityPlayer.worldObj.isRemote && entityPlayer.getCurrentEquippedItem() != null) {
            ItemStack equipped = entityPlayer.getCurrentEquippedItem();
            if (Eln.multiMeterElement.checkSameItemStack(equipped)) {
                String str = multiMeterString(side);
                if (str != null)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (Eln.thermometerElement.checkSameItemStack(equipped)) {
                String str = thermoMeterString(side);
                if (str != null)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (Eln.allMeterElement.checkSameItemStack(equipped)) {
                String str1 = multiMeterString(side);
                String str2 = thermoMeterString(side);
                String str = "";
                if (str1 != null)
                    str += str1;
                if (str2 != null)
                    str += str2;
                if (str.equals("") == false)
                    Utils.addChatMessage(entityPlayer, str);
                return true;
            }
            if (Eln.configCopyToolElement.checkSameItemStack(equipped)) {
                if(!equipped.hasTagCompound()) {
                    equipped.setTagCompound(new NBTTagCompound());
                }
                String act;
                SoundCommand snd = beepError;
                if(entityPlayer.isSneaking() || Eln.playerManager.get(entityPlayer).getInteractEnable()) {
                    if(writeConfigTool(side, equipped.getTagCompound(), entityPlayer))
                        snd = beepDownloaded;
                    act = "write";
                } else {
                    if(readConfigTool(side, equipped.getTagCompound(), entityPlayer))
                        snd = beepUploaded;
                    act = "read";
                }
                snd.set(
                    entityPlayer.posX,
                    entityPlayer.posY,
                    entityPlayer.posZ,
                    entityPlayer.worldObj
                ).play();
                Utils.println(String.format("NB.oBA: act %s data %s", act, equipped.getTagCompound().toString()));
                return true;
            }
        }
        if (hasGui(side)) {
            entityPlayer.openGui(Eln.instance, GuiHandler.nodeBaseOpen + side.getInt(), coordonate.world(), coordonate.x, coordonate.y, coordonate.z);
            return true;
        }

        return false;
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    public static void tryConnectTwoNode(NodeBase nodeA, Direction directionA, LRDU lrduA, NodeBase nodeB, Direction directionB, LRDU lrduB) {
        int mskA = nodeA.getSideConnectionMask(directionA, lrduA);
        int mskB = nodeB.getSideConnectionMask(directionB, lrduB);
        if (compareConnectionMask(mskA, mskB)) {
            ElectricalConnection eCon = null;
            ThermalConnection tCon = null;

            NodeConnection nodeConnection = new NodeConnection(nodeA, directionA, lrduA, nodeB, directionB, lrduB);

            nodeA.nodeConnectionList.add(nodeConnection);
            nodeB.nodeConnectionList.add(nodeConnection);

            nodeA.setNeedPublish(true);
            nodeB.setNeedPublish(true);

            nodeA.lrduCubeMask.set(directionA, lrduA, true);
            nodeB.lrduCubeMask.set(directionB, lrduB, true);

            nodeA.newConnectionAt(nodeConnection, true);
            nodeB.newConnectionAt(nodeConnection, false);

            ElectricalLoad eLoad;
            if ((eLoad = nodeA.getElectricalLoad(directionA, lrduA, mskB)) != null) {

                ElectricalLoad otherELoad = nodeB.getElectricalLoad(directionB, lrduB, mskA);
                if (otherELoad != null) {
                    eCon = new ElectricalConnection(eLoad, otherELoad);

                    Eln.simulator.addElectricalComponent(eCon);
                    nodeConnection.addConnection(eCon);
                }
            }
            ThermalLoad tLoad;
            if ((tLoad = nodeA.getThermalLoad(directionA, lrduA, mskB)) != null) {

                ThermalLoad otherTLoad = nodeB.getThermalLoad(directionB, lrduB, mskA);
                if (otherTLoad != null) {
                    tCon = new ThermalConnection(tLoad, otherTLoad);

                    Eln.simulator.addThermalConnection(tCon);
                    nodeConnection.addConnection(tCon);
                }

            }
        }
    }

    public abstract int getSideConnectionMask(Direction directionA, LRDU lrduA);

    public abstract ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA, int mask);

    public abstract ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB, int mask);

    public void checkCanStay(boolean onCreate) {

    }

    public void connectJob() {
        // EXTERNAL OTHERS SIXNODE
        {
            int[] emptyBlockCoord = new int[3];
            int[] otherBlockCoord = new int[3];
            for (Direction direction : Direction.values()) {
                if (isBlockWrappable(direction)) {
                    emptyBlockCoord[0] = coordonate.x;
                    emptyBlockCoord[1] = coordonate.y;
                    emptyBlockCoord[2] = coordonate.z;
                    direction.applyTo(emptyBlockCoord, 1);
                    for (LRDU lrdu : LRDU.values()) {
                        Direction elementSide = direction.applyLRDU(lrdu);
                        otherBlockCoord[0] = emptyBlockCoord[0];
                        otherBlockCoord[1] = emptyBlockCoord[1];
                        otherBlockCoord[2] = emptyBlockCoord[2];
                        elementSide.applyTo(otherBlockCoord, 1);
                        NodeBase otherNode = NodeManager.instance.getNodeFromCoordonate(new Coordonate(otherBlockCoord[0], otherBlockCoord[1], otherBlockCoord[2], coordonate.dimention));
                        if (otherNode == null) continue;
                        Direction otherDirection = elementSide.getInverse();
                        LRDU otherLRDU = otherDirection.getLRDUGoingTo(direction).inverse();
                        if (this instanceof SixNode || otherNode instanceof SixNode) {
                            tryConnectTwoNode(this, direction, lrdu, otherNode, otherDirection, otherLRDU);
                        }
                    }
                }
            }
        }

        {
            for (Direction dir : Direction.values()) {
                NodeBase otherNode = getNeighbor(dir);
                if (otherNode != null && otherNode.isAdded) {
                    for (LRDU lrdu : LRDU.values()) {
                        tryConnectTwoNode(this, dir, lrdu, otherNode, dir.getInverse(), lrdu.inverseIfLR());
                    }
                }

            }
        }

    }

    public void disconnectJob() {

        for (NodeConnection c : nodeConnectionList) {

            if (c.N1 != this) {
                c.N1.nodeConnectionList.remove(c);
                c.N1.setNeedPublish(true);
                c.N1.lrduCubeMask.set(c.dir1, c.lrdu1, false);
            }
            if(c.N2 != this) {
                c.N2.nodeConnectionList.remove(c);
                c.N2.setNeedPublish(true);
                c.N2.lrduCubeMask.set(c.dir2, c.lrdu2, false);
            }
            c.destroy();
        }

        lrduCubeMask.clear();

        nodeConnectionList.clear();
    }

    public static boolean compareConnectionMask(int mask1, int mask2) {
        if (((mask1 & 0xFFFF) & (mask2 & 0xFFFF)) == 0) return false;
        if (((mask1 & maskColorCareData) & (mask2 & maskColorCareData)) == 0) return true;
        if ((mask1 & maskColorData) == (mask2 & maskColorData)) return true;
        return false;
    }

    public void externalDisconnect(Direction side, LRDU lrdu) {
    }

    public void newConnectionAt(NodeConnection connection, boolean isA) {
    }

    public void connectInit() {
        lrduCubeMask.clear();
        nodeConnectionList.clear();
    }

    public void connect() {

        if (isAdded) {
            disconnect();
        }

        connectInit();
        connectJob();

        isAdded = true;

        setNeedPublish(true);

    }

    public void disconnect() {
        if (!isAdded) {
            Utils.println("Node destroy error already destroy");
            return;
        }

        disconnectJob();

        isAdded = false;
    }

    public boolean nodeAutoSave() {
        return true;
    }

    public void readFromNBT(NBTTagCompound nbt) {

        coordonate.readFromNBT(nbt, "c");

        neighborOpaque = nbt.getByte("NBOpaque");
        neighborWrapable = nbt.getByte("NBWrap");

        initialized = true;
    }

    public void writeToNBT(NBTTagCompound nbt) {

        coordonate.writeToNBT(nbt, "c");

        int idx;

        nbt.setByte("NBOpaque", neighborOpaque);
        nbt.setByte("NBWrap", neighborWrapable);

    }

    public String multiMeterString(Direction side) {
        return "";
    }

    public String thermoMeterString(Direction side) {
        return "";
    }

    public boolean readConfigTool(Direction side, NBTTagCompound tag, EntityPlayer invoker) { return false; }

    public boolean writeConfigTool(Direction side, NBTTagCompound tag, EntityPlayer invoker) { return false; }

    public void setNeedPublish(boolean needPublish) {
        this.needPublish = needPublish;
    }

    public boolean getNeedPublish() {
        return needPublish;
    }

    private boolean isINodeProcess(IProcess process) {
        for (Class c : process.getClass().getInterfaces()) {
            if (c == INBTTReady.class) return true;
        }
        return false;
    }

    boolean needNotify = false;

    public void publishSerialize(DataOutputStream stream) {

    }

    public void preparePacketForClient(DataOutputStream stream) {
        try {
            stream.writeByte(Eln.packetForClientNode);

            stream.writeInt(coordonate.x);
            stream.writeInt(coordonate.y);
            stream.writeInt(coordonate.z);

            stream.writeByte(coordonate.dimention);

            stream.writeUTF(getNodeUuid());

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void sendPacketToClient(ByteArrayOutputStream bos, EntityPlayerMP player) {
        Utils.sendPacketToClient(bos, player);
    }


    public void sendPacketToAllClient(ByteArrayOutputStream bos) {
        sendPacketToAllClient(bos, 100000);
    }

    public void sendPacketToAllClient(ByteArrayOutputStream bos, double range) {
        //Profiler p = new Profiler();

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        for (Object obj : server.getConfigurationManager().playerEntityList) {

            EntityPlayerMP player = (EntityPlayerMP) obj;
            WorldServer worldServer = (WorldServer) MinecraftServer.getServer().worldServerForDimension(player.dimension);
            PlayerManager playerManager = worldServer.getPlayerManager();
            if (player.dimension != this.coordonate.dimention) continue;
            if (!playerManager.isPlayerWatchingChunk(player, coordonate.x / 16, coordonate.z / 16)) continue;
            if (coordonate.distanceTo(player) > range) continue;

            Utils.sendPacketToClient(bos, player);
        }

    }

    public ByteArrayOutputStream getPublishPacket() {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {

            stream.writeByte(Eln.packetNodeSingleSerialized);

            stream.writeInt(coordonate.x);
            stream.writeInt(coordonate.y);
            stream.writeInt(coordonate.z);
            stream.writeByte(coordonate.dimention);

            stream.writeUTF(getNodeUuid());

            publishSerialize(stream);

            return bos;
        } catch (IOException e) {

            e.printStackTrace();

        }
        return null;
    }

    public void publishToAllPlayer() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        for (Object obj : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            WorldServer worldServer = (WorldServer) MinecraftServer.getServer().worldServerForDimension(player.dimension);
            PlayerManager playerManager = worldServer.getPlayerManager();
            if (player.dimension != this.coordonate.dimention) continue;
            if (!playerManager.isPlayerWatchingChunk(player, coordonate.x / 16, coordonate.z / 16)) continue;

            Utils.sendPacketToClient(getPublishPacket(), player);
        }
        if (needNotify) {
            needNotify = false;
            notifyNeighbor();
        }
        needPublish = false;
    }

    public void publishToPlayer(EntityPlayerMP player) {
        Utils.sendPacketToClient(getPublishPacket(), player);
    }

    public void dropItem(ItemStack itemStack) {
        if (itemStack == null) return;
        if (coordonate.world().getGameRules().getGameRuleBooleanValue("doTileDrops")) {
            float var6 = 0.7F;
            double var7 = (double) (coordonate.world().rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var9 = (double) (coordonate.world().rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            double var11 = (double) (coordonate.world().rand.nextFloat() * var6) + (double) (1.0F - var6) * 0.5D;
            EntityItem var13 = new EntityItem(coordonate.world(), (double) coordonate.x + var7, (double) coordonate.y + var9, (double) coordonate.z + var11, itemStack);
            var13.delayBeforeCanPickup = 10;
            coordonate.world().spawnEntityInWorld(var13);
        }
    }

    public void dropInventory(IInventory inventory) {
        if (inventory == null) return;
        for (int idx = 0; idx < inventory.getSizeInventory(); idx++) {
            dropItem(inventory.getStackInSlot(idx));
        }
    }

    public abstract void initializeFromNBT();

    public void globalBoot() {

    }

    public void needPublish() {
        setNeedPublish(true);
    }

    public void unload() {
        disconnect();
    }
}

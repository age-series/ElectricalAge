package mods.eln.integration.fmp;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.misc.Coordinate;
import mods.eln.node.NodeManager;
import mods.eln.partnode.FmpCreativeResistorElement;
import mods.eln.partnode.FmpMultipartNode;
import mods.eln.sixnode.CreativePowerResistorDescriptor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

/**
 * A floor-mounted ForgeMultipart rendition of Electrical Age's creative resistor.
 * Its electrical implementation is intentionally kept with the multipart rather
 * than creating a hidden SixNode tile entity.
 */
public class CreativeResistorFmpPart extends PartNodeFmpPart {

    private static CreativePowerResistorDescriptor descriptor;
    private double resistance = 100.0;
    private byte mountSide = 1;
    private byte rotation;
    private transient FmpMultipartNode node;
    private transient FmpCreativeResistorElement element;

    public CreativeResistorFmpPart() {
    }

    public CreativeResistorFmpPart(byte mountSide, byte rotation) {
        this.mountSide = mountSide;
        this.rotation = rotation;
    }

    @Override
    public String getType() {
        return CREATIVE_RESISTOR_TYPE;
    }

    @Override
    public Cuboid6 getBounds() {
        switch (mountSide) {
            case 0: return horizontalBounds(0.75, 1.0);
            case 1: return horizontalBounds(0.0, 0.25);
            case 2: return wallBoundsZ(0.0, 0.25);
            case 3: return wallBoundsZ(0.75, 1.0);
            case 4: return wallBoundsX(0.0, 0.25);
            default: return wallBoundsX(0.75, 1.0);
        }
    }

    @Override
    public boolean renderStatic(Vector3 pos, int pass) {
        // Obj3D draws through immediate-mode OpenGL. Calling it while FMP is
        // compiling a chunk display list corrupts the surrounding render state.
        return false;
    }

    @Override
    public void renderDynamic(Vector3 pos, float frame, int pass) {
        if (pass != 0) {
            return;
        }
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(pos.x, pos.y, pos.z);
            translateToMountFace();
            applyMountRotation();
            if (mountSide == 0 || mountSide == 1) {
                GL11.glRotatef(rotation * 90.0f - 90.0f, 0.0f, 1.0f, 0.0f);
            } else {
                // Local Y is the mounting-face normal after applyMountRotation.
                // North/south faces need one extra quarter turn for their
                // vertical default orientation.
                final float wallRotation = rotation * 90.0f + ((mountSide == 2 || mountSide == 3) ? 90.0f : 0.0f);
                GL11.glRotatef(wallRotation, 0.0f, 1.0f, 0.0f);
            }
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            resistorDescriptor().draw();
        } finally {
            GL11.glPopMatrix();
        }
    }

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        tag.setDouble("resistance", resistance);
        tag.setByte("rotation", rotation);
        tag.setByte("mountSide", mountSide);
    }

    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        resistance = tag.getDouble("resistance");
        if (resistance <= 0.0) {
            resistance = 100.0;
        }
        rotation = tag.getByte("rotation");
        mountSide = tag.getByte("mountSide");
    }

    @Override
    public void writeDesc(MCDataOutput packet) {
        super.writeDesc(packet);
        packet.writeByte(mountSide);
        packet.writeByte(rotation);
    }

    @Override
    public void readDesc(MCDataInput packet) {
        super.readDesc(packet);
        mountSide = packet.readByte();
        rotation = packet.readByte();
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(double resistance) {
        this.resistance = Math.max(1.0e-9, resistance);
    }

    @Override
    public void onAdded() {
        super.onAdded();
        createNode();
    }

    @Override
    public void onRemoved() {
        destroyNode();
        super.onRemoved();
    }

    @Override
    public void onNeighborChanged() {
        super.onNeighborChanged();
        if (node != null) {
            node.reconnect();
        }
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack heldItem) {
        if (!Utils.isPlayerUsingWrench(player)) {
            return false;
        }
        if (!world().isRemote) {
            rotation = (byte) ((rotation + 1) & 3);
            destroyNode();
            createNode();
            sendDescUpdate();
        }
        return true;
    }

    private static CreativePowerResistorDescriptor resistorDescriptor() {
        if (descriptor == null) {
            descriptor = new CreativePowerResistorDescriptor("Creative Power Resistor", Eln.obj.getObj("PowerElectricPrimitives"));
        }
        return descriptor;
    }

    private void createNode() {
        if (world() == null || world().isRemote || node != null || NodeManager.instance == null) return;
        final Coordinate coordinate = new Coordinate(x(), y(), z(), world().provider.dimensionId);
        final Object existing = NodeManager.instance.getNodeFromCoordonate(coordinate);
        if (existing instanceof FmpMultipartNode) {
            node = (FmpMultipartNode) existing;
        } else {
            node = new FmpMultipartNode();
            node.coordinate = coordinate;
            NodeManager.instance.addNode(node);
        }
        element = new FmpCreativeResistorElement(mountSide, rotation, resistance);
        node.addElement(element);
    }

    private void destroyNode() {
        if (node == null) return;
        if (element != null) node.removeElement(element);
        if (node.isEmpty()) {
            node.disconnect();
            if (NodeManager.instance != null) NodeManager.instance.removeNode(node);
        }
        element = null;
        node = null;
    }

    private Cuboid6 horizontalBounds(double minY, double maxY) {
        return (rotation & 1) == 0
            ? new Cuboid6(0.25, minY, 0.0, 0.75, maxY, 1.0)
            : new Cuboid6(0.0, minY, 0.25, 1.0, maxY, 0.75);
    }

    private Cuboid6 wallBoundsZ(double minZ, double maxZ) {
        return (rotation & 1) == 0
            ? new Cuboid6(0.25, 0.0, minZ, 0.75, 1.0, maxZ)
            : new Cuboid6(0.0, 0.25, minZ, 1.0, 0.75, maxZ);
    }

    private Cuboid6 wallBoundsX(double minX, double maxX) {
        return (rotation & 1) == 0
            ? new Cuboid6(minX, 0.0, 0.25, maxX, 1.0, 0.75)
            : new Cuboid6(minX, 0.25, 0.0, maxX, 0.75, 1.0);
    }

    private void applyMountRotation() {
        switch (mountSide) {
            case 0: GL11.glRotatef(180.0f, 1.0f, 0.0f, 0.0f); break;
            case 2: GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f); break;
            case 3: GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f); break;
            case 4: GL11.glRotatef(-90.0f, 0.0f, 0.0f, 1.0f); break;
            case 5: GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f); break;
            default: break;
        }
    }

    private void translateToMountFace() {
        switch (mountSide) {
            case 0: GL11.glTranslated(0.5, 1.0, 0.5); break;
            case 1: GL11.glTranslated(0.5, 0.0, 0.5); break;
            case 2: GL11.glTranslated(0.5, 0.5, 0.0); break;
            case 3: GL11.glTranslated(0.5, 0.5, 1.0); break;
            case 4: GL11.glTranslated(0.0, 0.5, 0.5); break;
            default: GL11.glTranslated(1.0, 0.5, 0.5); break;
        }
    }
}

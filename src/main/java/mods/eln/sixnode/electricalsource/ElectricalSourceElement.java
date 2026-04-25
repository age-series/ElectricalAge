package mods.eln.sixnode.electricalsource;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.misc.Coordinate;
import mods.eln.node.Node;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeConnection;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalcable.UtilityCableElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalSourceElement extends SixNodeElement implements IConfigurable {

    NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    NbtElectricalLoad electricalLoadRed = new NbtElectricalLoad("electricalLoadRed");
    NbtElectricalLoad electricalLoadWhite = new NbtElectricalLoad("electricalLoadWhite");
    NbtElectricalLoad electricalLoadGround = new NbtElectricalLoad("electricalLoadGround");
    VoltageSource voltageSource = new VoltageSource("voltSrc", electricalLoad, null);
    VoltageSource voltageSourceRed = new VoltageSource("voltSrcRed", electricalLoadRed, null);
    VoltageSource voltageSourceWhite = new VoltageSource("voltSrcWhite", electricalLoadWhite, null);
    VoltageSource voltageSourceGround = new VoltageSource("voltSrcGround", electricalLoadGround, null);

    public static final int setVoltageId = 1;

    public ElectricalSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(electricalLoad);
        electricalLoadList.add(electricalLoadRed);
        electricalLoadList.add(electricalLoadWhite);
        electricalLoadList.add(electricalLoadGround);
        electricalComponentList.add(voltageSource);
        electricalComponentList.add(voltageSourceRed);
        electricalComponentList.add(voltageSourceWhite);
        electricalComponentList.add(voltageSourceGround);
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        setConfiguredVoltage(nbt.getDouble("voltage"));
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("voltage", voltageSource.getVoltage());
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return getLoadForColor(lrdu, extractRequestedColor(mask, lrdu));
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (((ElectricalSourceDescriptor) sixNodeElementDescriptor).isSignalSource()) {
            return NodeBase.maskElectricalOutputGate;
        } else {
            int color = getPrimaryHotColor(lrdu);
            return NodeBase.maskElectricalPower + (color << NodeBase.maskColorShift) + (1 << NodeBase.maskColorCareShift);
        }
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(Math.max(electricalLoad.getVoltage(), electricalLoadRed.getVoltage()), getDisplayedCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", Math.max(electricalLoad.getVoltage(), electricalLoadRed.getVoltage())));
        info.put(I18N.tr("Current"), Utils.plotAmpere("", getDisplayedCurrent()));
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info.put(I18N.tr("Power"), Utils.plotPower("", Math.max(Math.abs(electricalLoad.getVoltage() * voltageSource.getCurrent()), Math.abs(electricalLoadRed.getVoltage() * voltageSourceRed.getCurrent()))));
        }
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) voltageSource.getVoltage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Eln.applySmallRs(electricalLoad);
        Eln.applySmallRs(electricalLoadRed);
        Eln.applySmallRs(electricalLoadWhite);
        Eln.applySmallRs(electricalLoadGround);
        setConfiguredVoltage(voltageSource.getVoltage());
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        return onBlockActivatedRotate(entityPlayer);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setVoltageId:
                    setConfiguredVoltage(stream.readFloat());
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("voltage")) {
            setConfiguredVoltage(compound.getDouble("voltage"));
            needPublish();
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        compound.setDouble("voltage", voltageSource.getVoltage());
    }

    @Override
    public void newConnectionAt(NodeConnection connection, boolean isA) {
        super.newConnectionAt(connection, isA);

        if (((ElectricalSourceDescriptor) sixNodeElementDescriptor).isSignalSource()) return;

        Direction localDirection = isA ? connection.getDir1() : connection.getDir2();
        LRDU localLrdu = side.getLRDUGoingTo(localDirection);
        if (localLrdu == null) return;
        Direction remoteDirection = isA ? connection.getDir2() : connection.getDir1();
        Object remoteNodeObject = isA ? connection.getN2() : connection.getN1();
        if (!(remoteNodeObject instanceof SixNode)) return;

        SixNode remoteNode = (SixNode) remoteNodeObject;
        Direction remoteElementSide = remoteDirection.applyLRDU(isA ? connection.getLrdu2() : connection.getLrdu1());
        if (!(remoteNode.getElement(remoteElementSide) instanceof UtilityCableElement)) return;

        UtilityCableElement utilityCable = (UtilityCableElement) remoteNode.getElement(remoteElementSide);
        LRDU remoteLrdu = utilityCable.side.getLRDUGoingTo(remoteDirection);
        if (remoteLrdu == null) return;
        Utils.println(
            "Source connect %s side=%s localDir=%s localLrdu=%s remoteDir=%s remoteCable=%s remoteSide=%s remoteLrdu=%s",
            getCoordinate(),
            side,
            localDirection,
            localLrdu,
            remoteDirection,
            utilityCable.getCoordinate(),
            utilityCable.side,
            remoteLrdu
        );
        int secondaryHot = getSecondaryHotColor(utilityCable);
        if (secondaryHot >= 0) {
            connectExtraConductor(connection, utilityCable, localLrdu, remoteLrdu, secondaryHot);
        }
        int neutral = getNeutralColor(utilityCable);
        if (neutral >= 0) {
            connectExtraConductor(connection, utilityCable, localLrdu, remoteLrdu, neutral);
        }
        int ground = getGroundColor(utilityCable);
        if (ground >= 0) {
            connectExtraConductor(connection, utilityCable, localLrdu, remoteLrdu, ground);
        }
    }

    private void connectExtraConductor(NodeConnection connection, UtilityCableElement utilityCable, LRDU localLrdu, LRDU remoteLrdu, int color) {
        ElectricalLoad localLoad = getLoadForColor(localLrdu, color);
        ElectricalLoad remoteLoad = utilityCable.getElectricalLoad(remoteLrdu, utilityCable.maskForConductorColor(color));
        if (localLoad == null || remoteLoad == null) return;
        Utils.println(
            "Source extra conductor %s side=%s localLrdu=%s color=%d remoteCable=%s remoteLrdu=%s",
            getCoordinate(),
            side,
            localLrdu,
            color,
            utilityCable.getCoordinate(),
            remoteLrdu
        );

        ElectricalConnection extraConnection = new ElectricalConnection(localLoad, remoteLoad);
        Eln.simulator.addElectricalComponent(extraConnection);
        connection.addConnection(extraConnection);
    }

    private void setConfiguredVoltage(double voltage) {
        voltageSource.setVoltage(voltage);
        voltageSourceRed.setVoltage(voltage);
        voltageSourceWhite.setVoltage(0.0);
        voltageSourceGround.setVoltage(0.0);
    }

    private double getDisplayedCurrent() {
        return Math.max(Math.abs(voltageSource.getCurrent()), Math.abs(voltageSourceRed.getCurrent()));
    }

    private ElectricalLoad getLoadForColor(LRDU lrdu, int color) {
        if (UtilityCableDescriptor.isGroundColorCode(color)) return electricalLoadGround;
        if (UtilityCableDescriptor.isNeutralColorCode(color)) return electricalLoadWhite;

        UtilityCableElement utilityCable = getAdjacentUtilityCable(lrdu);
        if (utilityCable != null) {
            int[] hots = utilityCable.activeHotConductorColors();
            if (hots.length > 1 && color == hots[1]) return electricalLoadRed;
        }
        return electricalLoad;
    }

    private int extractRequestedColor(int mask, LRDU lrdu) {
        if ((mask & NodeBase.maskColorCareData) == 0) {
            return getPrimaryHotColor(lrdu);
        }
        return (mask >> NodeBase.maskColorShift) & 0xF;
    }

    private int getPrimaryHotColor(LRDU lrdu) {
        UtilityCableElement utilityCable = getAdjacentUtilityCable(lrdu);
        if (utilityCable != null) {
            int[] hots = utilityCable.activeHotConductorColors();
            if (hots.length > 0) return hots[0];
        }
        return 0;
    }

    private int getSecondaryHotColor(UtilityCableElement utilityCable) {
        int[] hots = utilityCable.activeHotConductorColors();
        return hots.length > 1 ? hots[1] : -1;
    }

    private int getNeutralColor(UtilityCableElement utilityCable) {
        return utilityCable.activeNeutralConductorColor();
    }

    private int getGroundColor(UtilityCableElement utilityCable) {
        return utilityCable.activeGroundConductorColor();
    }

    private UtilityCableElement getAdjacentUtilityCable(LRDU lrdu) {
        SixNodeElement neighborElement = resolveAdjacentElement(lrdu);
        return neighborElement instanceof UtilityCableElement ? (UtilityCableElement) neighborElement : null;
    }

    private SixNodeElement resolveAdjacentElement(LRDU lrdu) {
        Coordinate base = getCoordinate();
        if (base == null || !base.getWorldExist()) return null;

        Direction worldDirection = side.applyLRDU(lrdu);
        Coordinate neighborCoordinate = base.moved(worldDirection);
        if (!neighborCoordinate.getBlockExist()) return null;
        if (!(neighborCoordinate.world().getTileEntity(neighborCoordinate.x, neighborCoordinate.y, neighborCoordinate.z) instanceof NodeBlockEntity)) {
            return null;
        }

        Node node = ((NodeBlockEntity) neighborCoordinate.world().getTileEntity(neighborCoordinate.x, neighborCoordinate.y, neighborCoordinate.z)).getNode();
        if (!(node instanceof SixNode)) return null;

        return ((SixNode) node).getElement(worldDirection.getInverse());
    }
}

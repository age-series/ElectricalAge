package mods.eln.sixnode.electricalbreaker;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeConnection;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.misc.Coordinate;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import mods.eln.sixnode.electricalcable.UtilityCableElement;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.tileentity.TileEntity;

public class ElectricalBreakerElement extends SixNodeElement {
    private static final String BREAKER_CLOSE_SOUND = "eln:circuit_breaker_close";
    private static final String BREAKER_OPEN_SOUND = "eln:circuit_breaker_open";
    private static final String BREAKER_TRIP_SOUND = "eln:circuit_breaker_trip";

    public ElectricalBreakerDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public NbtElectricalLoad aLoadRed = new NbtElectricalLoad("aLoadRed");
    public NbtElectricalLoad bLoadRed = new NbtElectricalLoad("bLoadRed");
    public NbtElectricalLoad aLoadWhite = new NbtElectricalLoad("aLoadWhite");
    public NbtElectricalLoad bLoadWhite = new NbtElectricalLoad("bLoadWhite");
    public NbtElectricalLoad aLoadGround = new NbtElectricalLoad("aLoadGround");
    public NbtElectricalLoad bLoadGround = new NbtElectricalLoad("bLoadGround");
    public Resistor switchResistor = new Resistor(aLoad, bLoad);
    public Resistor switchResistorRed = new Resistor(aLoadRed, bLoadRed);
    public Resistor whitePassThroughResistor = new Resistor(aLoadWhite, bLoadWhite);
    public Resistor groundPassThroughResistor = new Resistor(aLoadGround, bLoadGround);
    public ElectricalBreakerCutProcess cutProcess = new ElectricalBreakerCutProcess(this);

    SixNodeElementInventory inventory = new SixNodeElementInventory(0, 64, this);

    public float voltageMax = 600.0f, voltageMin = 0;

    boolean switchState = false;
    double currantMax = 0;
    boolean nbtBoot = false;

    public ElectricalCableDescriptor cableDescriptor = null;

    public static final byte setVoltageMaxId = 1;
    public static final byte setVoltageMinId = 2;
    public static final byte toogleSwitchId = 3;

    public ElectricalBreakerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(aLoad);
        electricalLoadList.add(bLoad);
        electricalLoadList.add(aLoadRed);
        electricalLoadList.add(bLoadRed);
        electricalLoadList.add(aLoadWhite);
        electricalLoadList.add(bLoadWhite);
        electricalLoadList.add(aLoadGround);
        electricalLoadList.add(bLoadGround);
        electricalComponentList.add(switchResistor);
        electricalComponentList.add(switchResistorRed);
        electricalComponentList.add(whitePassThroughResistor);
        electricalComponentList.add(groundPassThroughResistor);
        electricalComponentList.add(new Resistor(bLoad, null).pullDown());
        electricalComponentList.add(new Resistor(aLoad, null).pullDown());
        electricalComponentList.add(new Resistor(bLoadRed, null).pullDown());
        electricalComponentList.add(new Resistor(aLoadRed, null).pullDown());
        electricalComponentList.add(new Resistor(bLoadWhite, null).pullDown());
        electricalComponentList.add(new Resistor(aLoadWhite, null).pullDown());
        electricalComponentList.add(new Resistor(bLoadGround, null).pullDown());
        electricalComponentList.add(new Resistor(aLoadGround, null).pullDown());

        electricalProcessList.add(cutProcess);

        this.descriptor = (ElectricalBreakerDescriptor) descriptor;
    }

    public SixNodeElementInventory getInventory() {
        return inventory;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        switchState = nbt.getBoolean("switchState");
        voltageMax = nbt.getFloat("voltageMax");
        voltageMin = nbt.getFloat("voltageMin");
        nbtBoot = true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setBoolean("switchState", switchState);
        nbt.setFloat("voltageMax", voltageMax);
        nbt.setFloat("voltageMin", voltageMin);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        int color = extractRequestedColor(mask, lrdu);
        if (front == lrdu) return getLoadForColor(true, color);
        if (front.inverse() == lrdu) return getLoadForColor(false, color);
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return getPrimaryConnectionMask(lrdu);
        if (front.inverse() == lrdu) return getPrimaryConnectionMask(lrdu);

        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("Ua:", aLoad.getVoltage()) + Utils.plotVolt("Ub:", bLoad.getVoltage()) + Utils.plotAmpere("I:", getTripCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Contact"), switchState ? I18N.tr("Closed") : I18N.tr("Open"));
        info.put(I18N.tr("Current"), Utils.plotAmpere("", getTripCurrent()));
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info.put(I18N.tr("Voltages"), Utils.plotVolt("", aLoad.getVoltage()) + Utils.plotVolt(" ", bLoad.getVoltage()));
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
            stream.writeBoolean(switchState);
            stream.writeFloat(voltageMax);
            stream.writeFloat(voltageMin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSwitchState(boolean state) {
        if (state == switchState) return;
        if (state) {
            cutProcess.resetTripAccumulator();
        }
        playBreakerSound(state ? BREAKER_CLOSE_SOUND : BREAKER_OPEN_SOUND);
        switchState = state;
        refreshSwitchResistor();
        needPublish();
    }

    public void tripSwitch() {
        if (!switchState) return;
        playBreakerSound(BREAKER_TRIP_SOUND);
        switchState = false;
        refreshSwitchResistor();
        needPublish();
    }

    private void playBreakerSound(String soundName) {
        play(new SoundCommand(soundName).mulVolume(0.3F, 1.0f).smallRange());
    }

    public void refreshSwitchResistor() {
        ElectricalCableDescriptor cableDescriptor = resolveConnectedCableDescriptor();
        if (!switchState) {
            switchResistor.highImpedance();
            switchResistorRed.highImpedance();
        } else {
            cableDescriptor.applyTo(switchResistor);
            cableDescriptor.applyTo(switchResistorRed);
        }
        cableDescriptor.applyTo(whitePassThroughResistor);
        cableDescriptor.applyTo(groundPassThroughResistor);
    }

    public boolean getSwitchState() {
        return switchState;
    }

    @Override
    public void initialize() {
        computeElectricalLoad();
        setSwitchState(switchState);
    }

    @Override
    public void inventoryChanged() {
        computeElectricalLoad();
        reconnect();
    }

    public void computeElectricalLoad() {
        if (!nbtBoot) setSwitchState(false);
        nbtBoot = false;

        cableDescriptor = resolveConnectedCableDescriptor();
        cableDescriptor.applyTo(aLoad);
        cableDescriptor.applyTo(bLoad);
        cableDescriptor.applyTo(aLoadRed);
        cableDescriptor.applyTo(bLoadRed);
        cableDescriptor.applyTo(aLoadWhite);
        cableDescriptor.applyTo(bLoadWhite);
        cableDescriptor.applyTo(aLoadGround);
        cableDescriptor.applyTo(bLoadGround);
        currantMax = this.descriptor.currentLimit;
        refreshSwitchResistor();
    }

    private ElectricalCableDescriptor resolveConnectedCableDescriptor() {
        ElectricalCableDescriptor cable = resolveAdjacentCableDescriptor(front);
        if (cable != null) return cable;
        cable = resolveAdjacentCableDescriptor(front.inverse());
        if (cable != null) return cable;
        return Eln.instance.veryHighVoltageCableDescriptor;
    }

    private ElectricalCableDescriptor resolveAdjacentCableDescriptor(LRDU lrdu) {
        SixNodeElement neighborElement = resolveAdjacentElement(lrdu);
        if (neighborElement == null) return null;
        SixNodeDescriptor neighborDescriptor = neighborElement.sixNodeElementDescriptor;
        return neighborDescriptor instanceof ElectricalCableDescriptor ? (ElectricalCableDescriptor) neighborDescriptor : null;
    }

    private SixNodeElement resolveAdjacentElement(LRDU lrdu) {
        Coordinate base = getCoordinate();
        if (base == null || !base.getWorldExist()) return null;

        Direction worldDirection = side.applyLRDU(lrdu);
        Coordinate neighborCoordinate = base.moved(worldDirection);
        TileEntity tileEntity = neighborCoordinate.world().getTileEntity(neighborCoordinate.x, neighborCoordinate.y, neighborCoordinate.z);
        if (!(tileEntity instanceof NodeBlockEntity)) return null;

        Node node = ((NodeBlockEntity) tileEntity).getNode();
        if (!(node instanceof SixNode)) return null;

        return ((SixNode) node).getElement(worldDirection.getInverse());
    }

    @Override
    public void newConnectionAt(NodeConnection connection, boolean isA) {
        super.newConnectionAt(connection, isA);

        Direction localDirection = isA ? connection.getDir1() : connection.getDir2();
        LRDU localLrdu = side.getLRDUGoingTo(localDirection);
        if (localLrdu == null) return;
        Direction remoteDirection = isA ? connection.getDir2() : connection.getDir1();
        Node remoteNode = isA ? (Node) connection.getN2() : (Node) connection.getN1();
        if (!(remoteNode instanceof SixNode)) return;

        Direction remoteElementSide = remoteDirection.applyLRDU(isA ? connection.getLrdu2() : connection.getLrdu1());
        SixNodeElement remoteElement = ((SixNode) remoteNode).getElement(remoteElementSide);
        if (!(remoteElement instanceof UtilityCableElement)) return;

        UtilityCableElement utilityCable = (UtilityCableElement) remoteElement;
        LRDU remoteLrdu = utilityCable.side.getLRDUGoingTo(remoteDirection);
        if (remoteLrdu == null) return;
        Utils.println(
            "Breaker connect %s side=%s front=%s localDir=%s localLrdu=%s remoteDir=%s remoteCable=%s remoteSide=%s remoteLrdu=%s",
            getCoordinate(),
            side,
            front,
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
        ElectricalLoad localLoad = getLoadForColor(localLrdu == front, color);
        ElectricalLoad remoteLoad = utilityCable.getElectricalLoad(remoteLrdu, utilityCable.maskForConductorColor(color));
        if (localLoad == null || remoteLoad == null) return;
        Utils.println(
            "Breaker extra conductor %s side=%s localLrdu=%s frontSide=%s color=%d remoteCable=%s remoteLrdu=%s",
            getCoordinate(),
            side,
            localLrdu,
            localLrdu == front,
            color,
            utilityCable.getCoordinate(),
            remoteLrdu
        );

        ElectricalConnection extraConnection = new ElectricalConnection(localLoad, remoteLoad);
        Eln.simulator.addElectricalComponent(extraConnection);
        connection.addConnection(extraConnection);
    }

    public double getTripCurrent() {
        return Math.max(Math.abs(switchResistor.getCurrent()), Math.abs(switchResistorRed.getCurrent()));
    }

    public double getMonitoredVoltage() {
        return Math.max(Math.abs(aLoad.getVoltage()), Math.abs(aLoadRed.getVoltage()));
    }

    private ElectricalLoad getLoadForColor(boolean frontSide, int color) {
        if (UtilityCableDescriptor.isGroundColorCode(color)) return frontSide ? aLoadGround : bLoadGround;
        if (UtilityCableDescriptor.isNeutralColorCode(color)) return frontSide ? aLoadWhite : bLoadWhite;

        UtilityCableElement utilityCable = getAdjacentUtilityCable(frontSide ? front : front.inverse());
        if (utilityCable != null) {
            int[] hots = utilityCable.activeHotConductorColors();
            if (hots.length > 1 && color == hots[1]) return frontSide ? aLoadRed : bLoadRed;
        }
        return frontSide ? aLoad : bLoad;
    }

    private int extractRequestedColor(int mask, LRDU lrdu) {
        if ((mask & NodeBase.maskColorCareData) == 0) {
            return getPrimaryHotColor(lrdu);
        }
        return (mask >> NodeBase.maskColorShift) & 0xF;
    }

    private int getPrimaryConnectionMask(LRDU lrdu) {
        int color = getPrimaryHotColor(lrdu);
        return NodeBase.maskElectricalAll + (color << NodeBase.maskColorShift) + (1 << NodeBase.maskColorCareShift);
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

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setVoltageMaxId:
                    voltageMax = stream.readFloat();
                    needPublish();
                    break;
                case setVoltageMinId:
                    voltageMin = stream.readFloat();
                    needPublish();
                    break;
                case toogleSwitchId:
                    setSwitchState(!getSwitchState());
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

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalBreakerContainer(player, inventory);
    }
}

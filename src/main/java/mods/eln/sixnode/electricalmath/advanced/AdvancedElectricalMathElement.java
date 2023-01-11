package mods.eln.sixnode.electricalmath.advanced;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.node.NodeConnection;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement;
import mods.eln.solver.ISymbole;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class AdvancedElectricalMathElement extends SixNodeElement implements IConfigurable {
    static class GateInputSymbol implements ISymbole {
        private final String name;
        private final NbtElectricalGateInput gate;

        public GateInputSymbol(String name, NbtElectricalGateInput gate) {
            this.name = name;
            this.gate = gate;
        }

        @Override
        public double getValue() {
            return gate.getNormalized();
        }

        @Override
        public String getName() {
            return name;
        }
    }
    class DayTime implements ISymbole {

        @Override
        public double getValue() {
            return sixNode.coordinate.world().getWorldTime() / (24000.0 - 1.0);
        }

        @Override
        public String getName() {
            return "daytime";
        }
    }
    class APLC_GateProcess implements IProcess {
        @Override
        public void process(double time) {
            double vDelta = lastU - NOMINAL_V;
            lastU = powerLoad.getU();
            boolean lastPowered = isPowered;
            short lastStatus = isOverORUnderVoltage;

            if (lastU <= MIN_V){
                isPowered = false;
                setGlobalOutput(0);
            } else {
                isPowered = true;
                double mult = 1;
                if (Math.abs(vDelta) >= 10) {
                    if (vDelta > 0) {
                        mult = 1 + (Math.pow(Math.abs(lastU - NOMINAL_V), 1.28)) / NOMINAL_V;
                        isOverORUnderVoltage = 1;
                    } else {
                        mult = 1 - (Math.pow(Math.abs(lastU - NOMINAL_V), 1.2)) / NOMINAL_V;
                        isOverORUnderVoltage = -1;
                    }
                } else {
                    isOverORUnderVoltage = 0;
                }

                for (Gate gate : gates) {
                    gate.updateValue(mult);
                }
            }

            updatePowerUse();
            pushOutputs();
            if (lastPowered != isPowered || lastStatus != isOverORUnderVoltage)
                sendPackageAllClients();

            if (firstBoot){
                firstBoot = false;
                sendPackageAllClients();
            }
        }

        public void setGlobalOutput(double value){
            for (Gate gate : gates) {
                gate.value = value;
            }
        }

        public void pushOutputs(){
            for (Gate gate : gates) {
                gate.pushOutput();
            }
        }
    }

    //power
    public static final double wattsStandBy = 10f;
    public static final double wattsPerRedstone = 5f;
    public static final double wattsPerVoltageOutPut = 3.125f;

    //sim and connections
    /**
     * index: 0 to A gate, 1 to B gate, 2 to Output Gate
     * Use an integer to represent if the side is connected and where is connected.
     * <Pre>
     * 0 is the unconnected.
     * Positive connected.
     * <Pre>
     *     The positive value is a int value that contains all colors connected,
     *     each 1 bit is the color connected to it;
     *
     *     Ex: The int value 21 represents the junction of Black (0), Dark_Green(2), Dark_Red(4) connected at same time.
     *      so, 21 = (1 << 0) + (1 << 2) + (1 << 4).
     *     Also: The int value 0 is used to represent the Black in a signalBusCable, or a signalCable.
     * </Pre>
     */
    int[] sideConnectionMask = new int[3];
    final NbtElectricalGateInput[][] gateInput = new NbtElectricalGateInput[2][16];
    final NbtElectricalGateOutput[] gateOutput = new NbtElectricalGateOutput[16];
    final NbtElectricalGateOutputProcess[] gateOutputProcesses = new NbtElectricalGateOutputProcess[16];
    public final APLC_GateProcess process = new APLC_GateProcess();

    //equations
    private List<Gate> gates = new ArrayList<>(16);
    ArrayList<ISymbole> symboles = new ArrayList<>();

    //powerControl
    final NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    final Resistor powerResistor = new Resistor(powerLoad, null);

    //status and control var
    double lastU;
    double powerNeeded = wattsStandBy;
    boolean isPowered = false;
    boolean firstBoot = true;
    /**
     * 1 to over voltage
     * 0 to normal voltage
     * -1 to under voltage
     */
    short isOverORUnderVoltage = 0;
    private static final double NOMINAL_V = Eln.instance.lowVoltageCableDescriptor.electricalNominalVoltage;
    private static final double MIN_V = Eln.instance.lowVoltageCableDescriptor.electricalNominalVoltage * 0.8f;

    public AdvancedElectricalMathElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(powerLoad);
        electricalComponentList.add(powerResistor);

        powerLoad.setRs(MnaConst.noImpedance);
        powerResistor.setR(MnaConst.highImpedance);

        slowProcessList.add(new VoltageStateWatchDog().set(powerLoad).setUNominal(NOMINAL_V).setUMaxMin(70).set(new WorldExplosion(this).cableExplosion()));
        electricalProcessList.add(process);

        initOutput();
        initInputSymbolAndGates();
        updatePowerUse();
    }

    Gate preProcessEquation(String expression, int index){
        Gate selectedGate = null;
        for (Gate gate : gates) {
            if (gate.index == index) {
                selectedGate = gate;
                break;
            }
        }

        if (expression.equals("")) {
            if (selectedGate != null) {
                gates.remove(selectedGate);
                selectedGate.updateValue(0);
                selectedGate.pushOutput();
                flushConnectionMask();
            }
        } else {
            if (selectedGate == null){
                selectedGate = new Gate(index, this, 0);
                gates.add(selectedGate);
                flushConnectionMask();
            }
            selectedGate.updateExpression(expression);
        }

        flushPowerNeeded();
        updatePowerUse();
        sendPackageAllClients();
        return selectedGate;
    }

    private void flushConnectionMask(){
        sideConnectionMask = new int[]{0,0,0};
        for (Gate gate : gates) {
            gate.addColorMask();
            sideConnectionMask[2] |= (1 << gate.index);
        }
    }

    private void flushPowerNeeded(){
        powerNeeded = wattsStandBy;
        for (Gate gate : gates) {
            powerNeeded += wattsPerVoltageOutPut;
            powerNeeded += gate.powerToOperate;
        }
    }

    private void updatePowerUse(){
        if (isPowered) {
            powerResistor.setR((lastU * lastU) / powerNeeded);
        } else {
            powerResistor.setR(MnaConst.highImpedance);
        }
    }

    @Override
    public void newConnectionAt(@Nullable NodeConnection connection, boolean isA) {
        SixNodeElement e1 = ((SixNode) connection.getN1()).getElement(connection.getDir1().applyLRDU(connection.getLrdu1()));
        SixNodeElement e2 = ((SixNode) connection.getN2()).getElement(connection.getDir2().applyLRDU(connection.getLrdu2()));

        ElectricalSignalBusCableElement cable = null;
        LRDU lrdu = null;

        if (e1 instanceof ElectricalSignalBusCableElement) {
            cable = (ElectricalSignalBusCableElement) e1;
            lrdu = this.side.getLRDUGoingTo(connection.getDir2());
        }
        if (e2 instanceof ElectricalSignalBusCableElement) {
            cable = (ElectricalSignalBusCableElement) e2;
            lrdu = this.side.getLRDUGoingTo(connection.getDir1());
        }

        if (cable != null){
            int gateSide;
            boolean isOutput = false;

            if (lrdu == front) {isOutput = true; gateSide = 2;}
            else if (lrdu == front.left()) gateSide = 1;
            else if (lrdu == front.right() ) gateSide = 0;
            else return;

            NbtElectricalLoad[] nbtLoad = isOutput ? gateOutput : gateInput[gateSide];
            int mask = sideConnectionMask[gateSide];

            for (int i = 0; i <= 0xF; i++) {
                if (mask == 0) break;

                if ((mask | (1 << i)) != 0){
                    ElectricalConnection c1 = new ElectricalConnection(cable.getColoredElectricalLoads()[i], nbtLoad[i]);
                    Eln.simulator.addElectricalComponent(c1);
                    connection.addConnection(c1);
                }
            }
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (lrdu == front.inverse()) return powerLoad;
        if (lrdu == front && sideConnectionMask[2] == 1) return gateOutput[0];
        if (lrdu == front.left() && sideConnectionMask[1] == 1) return gateInput[1][0];
        if (lrdu == front.right() && sideConnectionMask[0] == 1) return gateInput[0][0];
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front.inverse()) return Node.maskElectricalPower;
        if (lrdu == front && sideConnectionMask[2] != 0) return Node.maskElectricalOutputGate;
        if (lrdu == front.left() && sideConnectionMask[1] != 0) return Node.maskElectricalInputGate;
        if (lrdu == front.right() && sideConnectionMask[0] != 0) return Node.maskElectricalInputGate;
        return 0;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    private void initOutput(){
        for (int idx = 0; idx <= 15; idx++) {
            NbtElectricalGateOutput output = new NbtElectricalGateOutput("gateOut" + idx);
            NbtElectricalGateOutputProcess process  = new NbtElectricalGateOutputProcess("gateOProcess" + idx, output);
            gateOutput[idx] = output;
            gateOutputProcesses[idx] = process;
            electricalLoadList.add(output);
            electricalComponentList.add(process);
        }
    }

    private void initInputSymbolAndGates(){
        symboles.clear();

        for (int j = 0; j < gateInput.length; j++) {
            NbtElectricalGateInput[] busSide = gateInput[j];

            for (int i = 0; i <= 0xF; i++) {
                String signature = String.valueOf((char) (65 + j)) + i;

                NbtElectricalGateInput nbtBustInput = new NbtElectricalGateInput(("gate" + signature));

                symboles.add(new GateInputSymbol(signature, nbtBustInput));
                electricalLoadList.add(nbtBustInput);
                busSide[i] = nbtBustInput;
            }
        }

        symboles.addAll(0, Arrays.asList(
            new GateInputSymbol("A", gateInput[0][0]),
            new GateInputSymbol("B", gateInput[1][0]),
            new DayTime())
        );
    }

    public String[] formatConnectionsVoltage(){
        String[] s = new String[2];
        for (int idx = 0; idx<2; idx++) {
            int mask = sideConnectionMask[idx];
            StringBuilder st = new StringBuilder();

            for (int i = 0; i <= 0xF; i++) {
                if (mask == 0) break;

                if ((mask & (1 << i)) != 0){
                    st.append(ElectricalSignalBusCableElement.Companion.getWool_to_chat()[15 - i].toString())
                        .append(Utils.plotVolt(gateInput[idx][i].getU()));
                }
            }

            if (st.length() != 0)
                s[idx] = st.toString();
            else
                s[idx] = "";
        }

        return s;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();

        String[] connections = formatConnectionsVoltage();
        for (int idx = 0; idx<2; idx++) {
            info.put(String.valueOf(((char) (65 + idx))), connections[idx]);
        }

        info.put(I18N.tr("Input voltage"), Utils.plotVolt("", gateOutput[0].getU()));
        info.put(I18N.tr("Input power"), Utils.plotPower("", powerResistor.getP()));
        return info;
    }

    @NotNull
    @Override
    public String multiMeterString() {
        StringBuilder builder = new StringBuilder(String.valueOf(EnumChatFormatting.WHITE));
        int idx = 0;
        for (String s : formatConnectionsVoltage()) {
            if (s != null) {
                if (s.length() != 0)
                    builder
                        .append(((char) (65 + idx)))
                        .append(": ")
                        .append(s);
                idx++;
            }
            builder.append(EnumChatFormatting.WHITE);
        }

        builder
            .append("Input: ")
            .append(Utils.plotVolt("U:", powerLoad.getU()))
            .append(Utils.plotPower("P:", powerResistor.getP()));

        return builder.toString();
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return "";
    }


    //network and nbt
    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        isPowered = nbt.getBoolean("isPowered");
        isOverORUnderVoltage = nbt.getShort("underOrOver");
        lastU = nbt.getDouble("lastU");

        for (int i = 0; i <= 15; i++) {
            String expression = nbt.getString("expression" + i);
            if (!expression.equals("")) {
                Gate gate = preProcessEquation(expression, i);
                gate.equation.readFromNBT(nbt, "equation");
            }
        }

    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("isPowered", isPowered);
        nbt.setShort("underOrOver", isOverORUnderVoltage);
        nbt.setDouble("lastU", lastU);

        for (Gate gate : gates) {
            nbt.setString("expression" + gate.index, gate.expression);
            gate.equation.writeToNBT(nbt,"equation");
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeInt(sideConnectionMask[0]);
            stream.writeInt(sideConnectionMask[1]);
            stream.writeInt(sideConnectionMask[2]);

            stream.writeInt(gates.size());
            for (Gate gate : gates) {
                new Gate.GateInfo(gate).serialize(stream);
            }

            stream.writeBoolean(isPowered);
            stream.writeShort(isOverORUnderVoltage);
            stream.writeDouble(powerNeeded);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(@NotNull DataInputStream stream, EntityPlayerMP player) {
        super.networkUnserialize(stream, player);
        try {
            byte id = stream.readByte();
            preProcessEquation(stream.readUTF(), id);
            reconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPackageAllClients(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(outputStream);

        this.preparePacketForClient(stream);
        try {
            stream.writeBoolean(isPowered);
            stream.writeShort(isOverORUnderVoltage);
            stream.writeDouble(powerNeeded);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.sendPacketToAllClient(outputStream);
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        for (Gate gate : gates) {
            compound.setString("expression" + gate.index, gate.expression);
        }
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        for (int i = 0; i <= 15; i++) {
            if (compound.hasKey("expression"+i)){
                preProcessEquation(compound.getString("expression"+i), i);
            }
        }
        reconnect();
    }
}

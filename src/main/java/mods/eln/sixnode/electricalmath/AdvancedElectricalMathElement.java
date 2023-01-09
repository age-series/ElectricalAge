package mods.eln.sixnode.electricalmath;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
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
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement;
import mods.eln.solver.ISymbole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdvancedElectricalMathElement extends ElectricalMathElement {

    class APLCProcess implements IProcess {

        @Override
        public void process(double time) {
            lastU = powerLoad.getU();
            double vDelta = lastU - NOMINAL_V;
            boolean oldPower = isPowered;

            if (lastU <= MIN_V){
                gateOutputProcess.setU(0);
                isPowered = false;
            }
            else if (Math.abs(vDelta) >= 10){
                if (vDelta > 0){
                    gateOutputProcess.setU(gateOutputProcess.getU() * (1+ (Math.pow(Math.abs(lastU - NOMINAL_V), 1.1))/lastU));
                } else {
                    gateOutputProcess.setU(gateOutputProcess.getU() * (1- (Math.pow(Math.abs(lastU - NOMINAL_V), 1.1))/lastU));
                }
                isPowered = true;
            }

            if (oldPower != isPowered) {
                redstoneReady = isPowered;
                needPublish();
            }
            updatePowerUse();
        }
    }

    private static final double wattsStandBy = 10f;
    private static final double wattsPerRedstone = 5f;
    private static final double wattsPerVoltageOutPut = 3.125f;

    final NbtElectricalGateInput[][] gateInput = new NbtElectricalGateInput[2][16];
    final NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    final Resistor powerResistor = new Resistor(powerLoad, null);

    boolean isPowered = true;
    double lastU;
    double powerNeeded = 0;

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();
    APLCProcess process = new APLCProcess();

    //todo TRANSFER TO THE DESCRIPTORCLASS;
    private static final double NOMINAL_V = 50f;
    private static final double MIN_V = 30f;

    /**
     *
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
    int[] sideConnectionMask = new int[2];

    public AdvancedElectricalMathElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(powerLoad);
        electricalComponentList.add(powerResistor);
        electricalProcessList.add(process);

        powerLoad.setRs(MnaConst.noImpedance);

        VoltageStateWatchDog watchDog = new VoltageStateWatchDog();
        watchDog.set(powerLoad);
        watchDog.setUNominal(NOMINAL_V);
        watchDog.setUMaxMin(70);
        watchDog.set(new WorldExplosion(this).cableExplosion());
        slowProcessList.add(watchDog);

        clearAndAddSymbols();
    }

    @Override
    void preProcessEquation(String expression) {
        super.preProcessEquation(expression);

        for (int idx = 0; idx < 2; idx++) {
            int colorCode = 0;

            //Default A,B Symbols
            if (equation.isSymboleUsed(symboleList.get(idx)))
                colorCode = 1;

            //SignalBust Symbols, A0, A1, B2, B4,...
            for (int i = 0; i <= 0xF; i++) {
                if (equation.isSymboleUsed(symboleList.get(i + (idx*16) + 3))) {
                    colorCode |= (1 << i);
                }
            }

            sideConnectionMask[idx] = colorCode;
        }

        powerNeeded = 0;
        powerNeeded += wattsStandBy;
        powerNeeded += redstoneRequired * wattsPerRedstone;
        redstoneReady = true;
        isPowered = true;
    }

    void updatePowerUse(){
        if (isPowered) {
            powerResistor.setR((lastU * lastU) / powerNeeded);
        } else {
            powerResistor.setR(MnaConst.highImpedance);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(outputStream);

        this.preparePacketForClient(stream);
        try {
            stream.writeBoolean(isPowered);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.sendPacketToAllClient(outputStream);
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

            //if (lrdu == front) nbtElectricalGateInputs = this.gateOutput;
            if (lrdu == front.left()) gateSide = 1;
            else if (lrdu == front.right() ) gateSide = 0;
            else return;

            NbtElectricalGateInput[] nbtElectricalGateInputs = gateInput[gateSide];
            int mask = sideConnectionMask[gateSide];
            for (int i = 0; i <= 0xF; i++) {
                if (mask == 0) break;

                if ((mask | (1 << i)) != 0){
                    ElectricalConnection c1 = new ElectricalConnection(cable.getColoredElectricalLoads()[i], nbtElectricalGateInputs[i]);
                    Eln.simulator.addElectricalComponent(c1);
                    connection.addConnection(c1);
                }
            }

        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (lrdu == front) return gateOutput;
        if (lrdu == front.inverse()) return powerLoad;
        if (lrdu == front.left() && sideConnectionMask[1] == 1) return gateInput[1][0];
        if (lrdu == front.right() && sideConnectionMask[0] == 1) return gateInput[0][0];
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front) return Node.maskElectricalOutputGate;
        if (lrdu == front.inverse()) return Node.maskElectricalPower;
        if (lrdu == front.left() && sideConnectionMask[1] != 0) return Node.maskElectricalInputGate;
        if (lrdu == front.right() && sideConnectionMask[0] != 0) return Node.maskElectricalInputGate;
        return 0;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeInt(sideConnectionMask[0]);
            stream.writeInt(sideConnectionMask[1]);
            stream.writeBoolean(isPowered);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void clearAndAddSymbols(){
        symboleList.clear();

        for (int j = 0; j < gateInput.length; j++) {
            NbtElectricalGateInput[] busSide = gateInput[j];

            for (int i = 0; i <= 0xF; i++) {
                String signature = String.valueOf((char) (65 + j)) + i;

                NbtElectricalGateInput nbtBustInput = new NbtElectricalGateInput(("gate" + signature));

                symboleList.add(new GateInputSymbol(signature, nbtBustInput));
                electricalLoadList.add(nbtBustInput);
                busSide[i] = nbtBustInput;
            }
        }

        symboleList.addAll(0, Arrays.asList(
            new GateInputSymbol("A", gateInput[0][0]),
            new GateInputSymbol("B", gateInput[1][0]),
            new DayTime())
        );
    }


    public String[] getConnectionsVoltage(){
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

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Equation"), expression);

        String[] connections = getConnectionsVoltage();
        for (int idx = 0; idx<2; idx++) {
            info.put(String.valueOf(((char) (65 + idx))), connections[idx]);
        }

        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", gateOutput.getU()));
        return info;
    }

    @NotNull
    @Override
    public String multiMeterString() {
        StringBuilder builder = new StringBuilder();
        int idx = 0;
        for (String s : getConnectionsVoltage()) {
            if (s != null) {
                if (s.length() != 0)
                    builder.append(((char) (65 + idx))).append(": ").append(s);

                idx++;
            }
        }

        builder.append(Utils.plotVolt("PowerIn:", powerLoad.getU()))
            .append(Utils.plotOhm("PowerRs:", powerResistor.getR()))
            .append(Utils.plotPower("PowerIn:", powerResistor.getP()));

        return builder.toString();
    }
}

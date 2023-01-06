package mods.eln.sixnode.electricalmath;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.ConfigCopyToolDescriptor;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.node.NodeConnection;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement;
import mods.eln.solver.Equation;
import mods.eln.solver.ISymbole;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElectricalMathElement extends SixNodeElement implements IConfigurable {

    NbtElectricalGateOutput gateOutput = new NbtElectricalGateOutput("gateOutput");
    final NbtElectricalGateInput[][] gateInput = new NbtElectricalGateInput[3][16];

    NbtElectricalGateOutputProcess gateOutputProcess = new NbtElectricalGateOutputProcess("gateOutputProcess", gateOutput);

    ArrayList<ISymbole> symboleList = new ArrayList<ISymbole>();

    ElectricalMathElectricalProcess electricalProcess = new ElectricalMathElectricalProcess(this);

    boolean firstBoot = true;

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
    int sideConnectionEnable[] = new int[3];
    String expression = "";
    Equation equation;
    boolean equationIsValid;

    int redstoneRequired;
    boolean redstoneReady = false;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    static final byte setExpressionId = 1;

    public ElectricalMathElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

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

        electricalLoadList.add(gateOutput);
        electricalLoadList.add(gateInput[0][0]);
        electricalLoadList.add(gateInput[1][0]);
        electricalLoadList.add(gateInput[2][0]);

        electricalComponentList.add(gateOutputProcess);

        electricalProcessList.add(electricalProcess);

        symboleList.add(new GateInputSymbol("A", gateInput[0][0]));
        symboleList.add(new GateInputSymbol("B", gateInput[1][0]));
        symboleList.add(new GateInputSymbol("C", gateInput[2][0]));

        symboleList.add(new DayTime());
    }

    class GateInputSymbol implements ISymbole {
        private String name;
        private NbtElectricalGateInput gate;

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

    class ElectricalMathElectricalProcess implements IProcess {
        private ElectricalMathElement e;

        ElectricalMathElectricalProcess(ElectricalMathElement e) {
            this.e = e;
        }

        @Override
        public void process(double time) {
            if (e.redstoneReady)
                e.gateOutputProcess.setOutputNormalizedSafe(e.equation.getValue(time));
            else
                e.gateOutputProcess.setOutputNormalized(0.0);
        }
    }

    void preProcessEquation(String expression) {
        this.expression = expression;
        equation = new Equation(); //expression, symboleList, 100);
        equation.setUpDefaultOperatorAndMapper();
        equation.setIterationLimit(100);
        equation.addSymbol(symboleList);
        equation.preProcess(expression);

        for (int idx = 0; idx < 3; idx++) {
            int colorCode = 0;

            //Default A,B,C Symbols
            if (equation.isSymboleUsed(symboleList.get(idx + 48)))
                colorCode = 1;

            //SignalBust Symbols, A0, A1, A2, B4, C12...
            for (int i = 0; i <= 0xF; i++) {
                if (equation.isSymboleUsed(symboleList.get(i + (idx*16) ))) {
                    colorCode |= (1 << i);
                }
            }

            sideConnectionEnable[idx] = colorCode;
        }

        this.expression = expression;

        redstoneRequired = 0;
        if (equationIsValid = equation.isValid()) {
            redstoneRequired = equation.getOperatorCount();
        }
        checkRedstone();
    }

    public class DayTime implements ISymbole {

        @Override
        public double getValue() {
            return sixNode.coordinate.world().getWorldTime() / (24000.0 - 1.0);
        }

        @Override
        public String getName() {
            return "daytime";
        }
    }

    //todo rewrite that to be more clean, and don't do irrelevant connection between cable and processor.
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
            NbtElectricalGateInput[] nbtElectricalGateInputs = null;

            //if (lrdu == front) nbtElectricalGateInputs = this.gateOutput;
            if (lrdu == front.left()) nbtElectricalGateInputs = this.gateInput[2];
            else if (lrdu == front.inverse() ) nbtElectricalGateInputs = this.gateInput[1];
            else if (lrdu == front.right() ) nbtElectricalGateInputs = this.gateInput[0];
            else return;

            for (int i = 0; i < nbtElectricalGateInputs.length; i++) {
                ElectricalConnection c1 = new ElectricalConnection(cable.getColoredElectricalLoads()[i], nbtElectricalGateInputs[i]);
                Eln.simulator.addElectricalComponent(c1);
                connection.addConnection(c1);
            }

        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (lrdu == front) return gateOutput;
        if (lrdu == front.left() && sideConnectionEnable[2] == 1) return gateInput[2][0];
        if (lrdu == front.inverse() && sideConnectionEnable[1] == 1) return gateInput[1][0];
        if (lrdu == front.right() && sideConnectionEnable[0] == 1) return gateInput[0][0];
        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front) return Node.maskElectricalOutputGate;
        if (lrdu == front.left() && sideConnectionEnable[2] != 0) return Node.maskElectricalInputGate;
        if (lrdu == front.inverse() && sideConnectionEnable[1] != 0) return Node.maskElectricalInputGate;
        if (lrdu == front.right() && sideConnectionEnable[0] != 0) return Node.maskElectricalInputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("Uout:", gateOutput.getU()) + Utils.plotAmpere("Iout:", gateOutput.getCurrent());
    }


    //todo rewrite that to show all inputs connected.
    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Equation"), expression);
        info.put(I18N.tr("Input voltages"),
            Utils.plotVolt("\u00A7c", gateInput[0][0].getU()) +
                Utils.plotVolt("\u00A7a", gateInput[1][0].getU()) +
                Utils.plotVolt("\u00A79", gateInput[2][0].getU()));
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", gateOutput.getU()));
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        if (firstBoot) preProcessEquation(expression);
    }

    @Override
    public void inventoryChanged() {
        super.inventoryChanged();
        checkRedstone();
    }

    void checkRedstone() {
        int redstoneInStack = 0;

        ItemStack stack = inventory.getStackInSlot(ElectricalMathContainer.restoneSlotId);
        if (stack != null) redstoneInStack = stack.stackSize;

        redstoneReady = redstoneRequired <= redstoneInStack;
        needPublish();
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalMathContainer(sixNode, player, inventory);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("expression", expression);
        equation.writeToNBT(nbt, "equation");
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        expression = nbt.getString("expression");
        preProcessEquation(expression);
        equation.readFromNBT(nbt, "equation");

        firstBoot = false;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(expression);
            stream.writeInt(redstoneRequired);
            stream.writeBoolean(equationIsValid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
        super.networkUnserialize(stream, player);
        try {
            switch (stream.readByte()) {
                case setExpressionId:
                    preProcessEquation(stream.readUTF());
                    reconnect();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("expression")) {
            preProcessEquation(compound.getString("expression"));
            reconnect();
        }
        if(ConfigCopyToolDescriptor.readVanillaStack(compound, "redstone", inventory, 0, invoker)) {
            checkRedstone();
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        compound.setString("expression", expression);
        ConfigCopyToolDescriptor.writeVanillaStack(compound, "redstone", inventory.getStackInSlot(0));
    }
}

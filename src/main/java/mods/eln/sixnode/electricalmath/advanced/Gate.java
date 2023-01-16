package mods.eln.sixnode.electricalmath.advanced;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.solver.Equation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class Gate {
    static class GateInfo{
        final int index;
        final String expression;
        final boolean isValid;

        public GateInfo(Gate gate) {
            index = gate.index;
            expression = gate.expression;
            isValid = gate.equationIsValid;
        }

        private GateInfo(int index, String expression, boolean isValid) {
            this.index = index;
            this.expression = expression;
            this.isValid = isValid;
        }

        public void serialize(DataOutputStream stream) throws IOException {
            stream.writeInt(index);
            stream.writeUTF(expression);
            stream.writeBoolean(isValid);
        }

        static public GateInfo unSerialize(DataInputStream stream) throws IOException{
            return new GateInfo(stream.readInt(), stream.readUTF(), stream.readBoolean());
        }
    }

    final int index;
    AdvancedElectricalMathElement element;

    NbtElectricalGateOutputProcess process;
    ElectricalLoad load;

    Equation equation;
    String expression;
    boolean equationIsValid;
    double powerToOperate = 0;

    double value;

    public Gate(int index, AdvancedElectricalMathElement element, double initialValue) {
        this.index = index;
        this.element = element;
        this.process = element.gateOutputProcesses[index];
        this.equation = new Equation();
        this.load = element.gateOutput[index];
        this.value = initialValue;
    }

    public Gate(int index){
        this.index = index;
    }

    public void updateExpression(String expression){
        this.expression = expression;
        powerToOperate = 0;
        this.equation = new Equation();
        equation.setUpDefaultOperatorAndMapper();
        equation.setIterationLimit(100);
        equation.addSymbol(element.symboles);
        equation.preProcess(expression);
        equationIsValid = equation.isValid();

        if (equationIsValid) {
            powerToOperate += equation.getOperatorCount() * AdvancedElectricalMathElement.wattsPerRedstone;
        }

        addColorMask();
    }

    public void addColorMask(){
        for (int idx = 0; idx < 2; idx++) {
            int colorCode = element.sideConnectionMask[idx];

            //Default A,B Symbols
            if (equation.isSymboleUsed(element.symboles.get(idx)))
                colorCode |= 0b1;

            //SignalBust Symbols, A0, A1, B2, B4,...
            for (int i = 0; i <= 0xF; i++) {
                if (equation.isSymboleUsed(element.symboles.get(i + (idx*16) + 3))) {
                    colorCode |= (1 << i);
                }
            }

            element.sideConnectionMask[idx] = colorCode;
        }
    }

    public void pushOutput(){
        this.process.setOutputNormalized(value);
    }

    public void updateValue(double mult,double dt){
        value = equation.getValue(dt) * mult;
    }
}

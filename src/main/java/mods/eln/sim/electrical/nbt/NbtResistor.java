package mods.eln.sim.electrical.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.electrical.mna.component.Resistor;
import mods.eln.sim.electrical.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class NbtResistor extends Resistor implements INBTTReady {

    String name;

    public NbtResistor(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        name += str;
        setR(nbt.getDouble(str + "R"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        name += str;
        nbt.setDouble(str + "R", getR());
    }
}

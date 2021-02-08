package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.thermal.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.item.EntityItem;
import org.lwjgl.opengl.GL11;

public class ArcFurnaceDescriptor extends ElectricalMachineDescriptor {

    private Obj3DPart main;

    public ArcFurnaceDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);

        if (obj != null) {
            main = obj.getPart("ArcFurnace");
        }
    }

    class ArcFurnaceDescriptorHandle {
    }

    @Override
    Object newDrawHandle() {
        return new ArcFurnaceDescriptorHandle();
    }

    @Override
    public float volumeForRunningSound(float processState, float powerFactor) {
        return super.volumeForRunningSound(processState, powerFactor);
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity,
              float powerFactor, float processState) {
        if (main != null) {
            GL11.glRotatef(-90f, 0f, 1f, 0f);
            GL11.glTranslatef(-0.5f, -0.5f, 0.5f);
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            main.draw();
        }
    }

    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity, float powerFactor, float processState) {
        ArcFurnaceDescriptorHandle handle = (ArcFurnaceDescriptorHandle) handleO;
    }

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}

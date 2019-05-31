package mods.eln.transparentnode.powerinductor;

import mods.eln.Eln;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.mna.misc.MnaConst;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class PowerInductorDescriptor extends TransparentNodeDescriptor {

    private Obj3D obj;

    public PowerInductorDescriptor(String name, Obj3D obj, ISerie serie) {
        super(name, PowerInductorElement.class, PowerInductorRender.class);
        this.serie = serie;
        this.obj = obj;
    }

    ISerie serie;

    public double getlValue(int cableCount) {
        if (cableCount == 0) return 0;
        return serie.getValue(cableCount - 1);
    }

    public double getlValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(PowerInductorContainer.cableId);
        if (core == null)
            return getlValue(0);
        else
            return getlValue(core.stackSize);
    }

    public double getRsValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(PowerInductorContainer.coreId);

        if (core == null) return MnaConst.highImpedance;
        FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

        double coreFactor = coreDescriptor.cableMultiplier;

        return Eln.instance.lowVoltageCableDescriptor.electricalRs * coreFactor;
    }

    void draw() {}

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        draw();
    }
}

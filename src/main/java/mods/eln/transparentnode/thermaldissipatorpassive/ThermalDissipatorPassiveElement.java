package mods.eln.transparentnode.thermaldissipatorpassive;


import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ThermalDissipatorPassiveElement extends TransparentNodeElement {
    ThermalDissipatorPassiveDescriptor descriptor;
    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");


    public ThermalDissipatorPassiveElement(TransparentNode transparentNode,
                                           TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);

        thermalLoadList.add(thermalLoad);
        this.descriptor = (ThermalDissipatorPassiveDescriptor) descriptor;

        slowProcessList.add(thermalWatchdog);

        thermalWatchdog
            .setMaximumTemperature(this.descriptor.warmLimit)
            .setDestroys(new WorldExplosion(this).machineExplosion());
    }


    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog(thermalLoad);

    @Nullable
    @Override
    public ElectricalLoad getElectricalLoad(@NotNull Direction side, @NotNull LRDU lrdu) {

        return null;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull Direction side, @NotNull LRDU lrdu) {

        if (side == Direction.YN || side == Direction.YP || lrdu != lrdu.Down) return null;
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(@NotNull Direction side, @NotNull LRDU lrdu) {

        if (side == Direction.YN || side == Direction.YP || lrdu != lrdu.Down) return 0;
        return node.maskThermal;
    }

    @NotNull
    @Override
    public String multiMeterString(@NotNull Direction side) {

        return "";
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {

        return Utils.plotCelsius("T : ", thermalLoad.temperatureCelsius) + Utils.plotPower("P : ", thermalLoad.getPower());
    }

    @Override
    public void initialize() {
        descriptor.applyTo(thermalLoad);
        connect();
    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, Direction side,
                                    float vx, float vy, float vz) {
        ItemStack stack = player.getCurrentEquippedItem();
        if (stack == null) return false;
        if (stack.getItem() == Items.water_bucket) {
            thermalLoad.temperatureCelsius *= 0.5;

            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.bucket));
            return true;
        }
        if (stack.getItem() == Item.getItemFromBlock(Blocks.ice)) {
            thermalLoad.temperatureCelsius *= 0.2;
            if (stack.stackSize != 0)
                stack.stackSize--;
            else
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.temperatureCelsius));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Thermal power"), Utils.plotPower("", thermalLoad.getPower()));
        }
        return info;
    }

}

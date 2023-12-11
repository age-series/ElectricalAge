package mods.eln.transparentnode.eggincubator;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EggIncubatorElement extends TransparentNodeElement {

    public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    public Resistor powerResistor = new Resistor(powerLoad, null);
    TransparentNodeElementInventory inventory = new EggIncubatorInventory(1, 64, this);
    EggIncubatorProcess slowProcess = new EggIncubatorProcess();
    EggIncubatorDescriptor descriptor;

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog(powerLoad);

    double lastVoltagePublish;

    public EggIncubatorElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        electricalLoadList.add(powerLoad);
        electricalComponentList.add(powerResistor);
        slowProcessList.add(slowProcess);

        this.descriptor = (EggIncubatorDescriptor) descriptor;

        WorldExplosion exp = new WorldExplosion(this).machineExplosion();
        slowProcessList.add(voltageWatchdog.setNominalVoltage(this.descriptor.nominalVoltage).setDestroys(exp));
    }

    class EggIncubatorProcess implements IProcess, INBTTReady {

        double energy = 5000;

        public EggIncubatorProcess() {
            resetEnergy();
        }

        void resetEnergy() {
            energy = 10000 + Math.random() * 10000;
        }

        @Override
        public void process(double time) {
            energy -= powerResistor.getPower() * time;
            if (inventory.getStackInSlot(EggIncubatorContainer.EggSlotId) != null) {
                descriptor.setState(powerResistor, true);
                if (energy <= 0) {
                    inventory.decrStackSize(EggIncubatorContainer.EggSlotId, 1);
                    EntityChicken chicken = new EntityChicken(node.coordinate.world());
                    chicken.setGrowingAge(-24000);
                    EntityLiving entityliving = (EntityLiving) chicken;
                    entityliving.setLocationAndAngles(node.coordinate.x + 0.5, node.coordinate.y + 0.5, node.coordinate.z + 0.5, MathHelper.wrapAngleTo180_float(node.coordinate.world().rand.nextFloat() * 360.0F), 0.0F);
                    entityliving.rotationYawHead = entityliving.rotationYaw;
                    entityliving.renderYawOffset = entityliving.rotationYaw;
                    //entityliving.func_110161_a((EntityLivingData)null); 1.6.4
                    node.coordinate.world().spawnEntityInWorld(entityliving);
                    entityliving.playLivingSound();
                    //node.coordonate.world().spawnEntityInWorld());
                    resetEnergy();

                    needPublish();
                }
            } else {
                descriptor.setState(powerResistor, false);
                resetEnergy();
            }
            if (Math.abs(powerLoad.getVoltage() - lastVoltagePublish) / descriptor.nominalVoltage > 0.1) needPublish();
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt, String str) {
            energy = nbt.getDouble(str + "energyCounter");
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt, String str) {
            nbt.setDouble(str + "energyCounter", energy);
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        return powerLoad;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull Direction side, @NotNull LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu == lrdu.Down) {
            return NodeBase.maskElectricalPower;
        }
        return 0;
    }

    @NotNull
    @Override
    public String multiMeterString(@NotNull Direction side) {
        return Utils.plotUIP(powerLoad.getVoltage(), powerLoad.getCurrent());
    }

    @NotNull
    @Override
    public String thermoMeterString(@NotNull Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        descriptor.applyTo(powerLoad);
        connect();
    }

    public void inventoryChange(IInventory inventory) {
        needPublish();
    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Nullable
    @Override
    public Container newContainer(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new EggIncubatorContainer(player, inventory, node);
    }

    public float getLightOpacity() {
        return 1.0f;
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            if (inventory.getStackInSlot(EggIncubatorContainer.EggSlotId) == null) stream.writeByte(0);
            else stream.writeByte(inventory.getStackInSlot(EggIncubatorContainer.EggSlotId).stackSize);

            node.lrduCubeMask.getTranslate(front.down()).serialize(stream);

            stream.writeFloat((float) powerLoad.getVoltage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastVoltagePublish = powerLoad.getVoltage();
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Has egg"), inventory.getStackInSlot(EggIncubatorContainer.EggSlotId) != null ?
            I18N.tr("Yes") : I18N.tr("No"));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Power consumption"), Utils.plotPower("", powerResistor.getPower()));
        }
        return info;
    }
}

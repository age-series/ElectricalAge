package mods.eln.item.electricalitem;

import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalLampItem extends LampItem implements IItemEnergyBattery {

    int lightMin, rangeMin;
    int lightMax, rangeMax;
    double energyStorage, dischargeMin, dischargeMax, chargePower;

    ResourceLocation on, off, boosted;

    public ElectricalLampItem(String name, int lightMin, int rangeMin, double dischargeMin, int lightMax,
                              int rangeMax, double dischargeMax, double energyStorage, double chargePower) {
        super(name);
        this.lightMin = lightMin;
        this.rangeMin = rangeMin;
        this.lightMax = lightMax;
        this.rangeMax = rangeMax + 1; //adding 1 is a hack. Since the value is locked at 1 anyway, I would rather not change a ton of code to make this work, and just double its range by adding 1 here
        this.chargePower = chargePower;
        this.dischargeMin = dischargeMin;
        this.dischargeMax = dischargeMax;
        this.energyStorage = energyStorage;
        setDefaultIcon(name + "off");
		boosted = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "boosted.png");
        on = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "on.png");
        off = new ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "off.png");
        //	off = new ResourceLocation("eln", "/model/StoneFurnace/all.png");
    }

    @Override
    int getRange(ItemStack stack) {
        return getLightState(stack) == 1 ? rangeMin : rangeMax;
    }

    @Override
    int getLight(ItemStack stack) {
        double energy = getEnergy(stack);
        int state = getLightState(stack);
        double power = 0;

        switch (state) {
            case 1:
                power = dischargeMin;
                break;
            case 2:
                power = dischargeMax;
                break;
        }

        if (energy > power) {
            //setEnergy(stack, energy - power);
            return getLightLevel(stack);
        } else {
            //setEnergy(stack,0);
            return 0;
        }
    }

    @Override
    public NBTTagCompound getDefaultNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setDouble("energy", 0);
        nbt.setBoolean("powerOn", false);
        nbt.setInteger("rand", (int) (Math.random() * 0xFFFFFFF));
        return nbt;
    }

    int getLightState(ItemStack stack) {
        return getNbt(stack).getInteger("LightState");
    }

    void setLightState(ItemStack stack, int value) {
        getNbt(stack).setInteger("LightState", value);
    }

    int getLightLevel(ItemStack stack) {
        return getLightState(stack) == 1 ? lightMin : lightMax;
    }
    /*
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4,
			boolean par5) {
		
		if(world.isRemote == false && entity instanceof EntityPlayer && ((EntityPlayer) entity).inventory.getCurrentItem() == stack && Eln.playerManager.get((EntityPlayer) entity).getInteractRise()){
			int lightState = getLightState(stack) + 1;
			if(lightState > 1) lightState = 0;
			//((EntityPlayer) entity).addChatMessage("Flashlight !!!");
			switch (lightState) {
				case 0:
					Utils.addChatMessage((EntityPlayerMP)entity,"Flashlight OFF");
					break;
				case 1:
					Utils.addChatMessage((EntityPlayerMP)entity,"Flashlight ON");
					break;
				case 2:
					Utils.addChatMessage((EntityPlayerMP)entity,"Flashlight ON-2");
					break;
				default:
					break;
			}
			setLightState(stack, lightState);
		}
		super.onUpdate(stack, world, entity, par4, par5);
	}*/

    @Override
    public ItemStack onItemRightClick(ItemStack s, World w, EntityPlayer p) {
        if (!w.isRemote) {
            int lightState = getLightState(s) + 1;
            if (lightState > 2) lightState = 0;
            //((EntityPlayer) entity).addChatMessage("Flashlight !!!");
            switch (lightState) {
                case 0:
                    Utils.addChatMessage((EntityPlayerMP) p, "flashlight OFF");
                    break;
                case 1:
                    Utils.addChatMessage((EntityPlayerMP) p, "flashlight ON");
                    break;
                case 2:
                    Utils.addChatMessage((EntityPlayerMP) p, "flashlight BOOSTED");
                    break;
                default:
                    break;
            }
            setLightState(s, lightState);
        }
        return s;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Discharge power: %1$W", Utils.plotValue(dischargeMin)));
        if (itemStack != null) {
            list.add(tr("Stored Energy: %1$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (int) (getEnergy(itemStack) / energyStorage * 100)));
            list.add(tr("State:") + " " + (getLightState(itemStack) != 0 ? tr("On") : tr("Off")));
        }
    }
/*
	@Override
	public double putEnergy(ItemStack stack, double energy, double time) {
		double hit = Math.min(energy,Math.min(energyStorage - getEnergy(stack), chargePower * time));
		setEnergy(stack, getEnergy(stack) + hit);
		return energy - hit;
	}

	@Override
	public boolean isFull(ItemStack stack) {
		return getEnergy(stack) == energyStorage;
	}
*/

    public double getEnergy(ItemStack stack) {
        return getNbt(stack).getDouble("energy");
    }

    public void setEnergy(ItemStack stack, double value) {
        getNbt(stack).setDouble("energy", value);
    }

    @Override
    public double getEnergyMax(ItemStack stack) {
        return energyStorage;
    }

    @Override
    public double getChargePower(ItemStack stack) {
        return chargePower;
    }

    @Override
    public double getDischagePower(ItemStack stack) {
        return 0;
    }

    @Override
    public int getPriority(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (type == ItemRenderType.INVENTORY)
            return false;
        return true;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		ResourceLocation drawlightstate = off;
			switch (getLightState(item)) {  //just a heads up this could be done way better
			case 0:
				drawlightstate = off;
				break;
			case 1:
				drawlightstate = on;
				break;
			case 2:
				drawlightstate = boosted;
				break;
			}
		UtilsClient.drawIcon(type,drawlightstate);
        //UtilsClient.drawIcon(type, (getLight(item) != 0 && getLightState(item) != 0 ? on : off));		
        if (type == ItemRenderType.INVENTORY) {
            UtilsClient.drawEnergyBare(type, (float) (getEnergy(item) / getEnergyMax(item)));
        }
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
        double energy = getEnergy(stack);
        int state = getLightState(stack);
        double power = 0;

        switch (state) {
            case 1:
                power = dischargeMin * time;
                break;
            case 2:
                power = dischargeMax * time;
                break;
        }

        if (energy > power)
            setEnergy(stack, energy - power);
        else
            setEnergy(stack, 0);
    }
}

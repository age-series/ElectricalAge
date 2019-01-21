package mods.eln.sixnode.lampsocket;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;

public class LightBlockEntity extends TileEntity {

    ArrayList<LightHandle> lightList = new ArrayList<LightHandle>();

    public static final ArrayList<LightBlockObserver> observers = new ArrayList<LightBlockObserver>();

    static void addObserver(LightBlockObserver observer) {
        observers.add(observer);
    }

    static void removeObserver(LightBlockObserver observer) {
        observers.remove(observer);
    }


    public interface LightBlockObserver {
        void lightBlockDestructor(Coordonate coord);
    }

    static class LightHandle implements INBTTReady {
        byte value;
        int timeout;

        public LightHandle() {
            value = 0;
            timeout = 0;
        }

        public LightHandle(byte value, int timeout) {
            this.value = value;
            this.timeout = timeout;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt, String str) {
            value = nbt.getByte(str + "value");
            timeout = nbt.getInteger(str + "timeout");
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt, String str) {
            nbt.setByte(str + "value", value);
            nbt.setInteger(str + "timeout", timeout);
        }
    }

    void addLight(int light, int timeout) {
        lightList.add(new LightHandle((byte) light, timeout));
        lightManager();
    }

	/*void removeLight(int light) {
        //int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		for (int idx = 0; idx < lightList.size(); idx++) {
			if (lightList.get(idx) == light) {
				lightList.remove(idx);
				lightManager();
				return;
			}
		}
		Utils.println("Assert void removeLight(int light)");
	}*/
	/*
	void replaceLight(int oldLight, int newLight) {
		for (int idx = 0; idx < lightList.size(); idx++) {
			if (lightList.get(idx) == oldLight) {
				lightList.set(idx, newLight);
				lightManager();
				return;
			}
		}	
		Utils.println("Assert void replaceLight(int oldLight, int newLight)");
	}*/

	/*
	int getLight() {
		int light = 0;
		for (LightHandle l : lightList) {
			if (light < l.value) light = l.value;
		}
		return light;
	}*/

    void lightManager() {
		/*if (lightList.size() == 0) {
			worldObj.setBlock(xCoord, yCoord, zCoord, 0);
		} else {
			int light = getLight();
			if (light != worldObj.getBlockMetadata(xCoord, yCoord, zCoord)) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, light, 2);
				worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
			}
		}*/
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;

        if (lightList.isEmpty()) {
            //	worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 2);
            worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            //worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
            //Eln.instance.tileEntityDestructor.add(this);
            Utils.println("Destroy light at " + xCoord + " " + yCoord + " " + zCoord + " ");
            return;
        }

        int light = 0;
        Iterator<LightHandle> iterator = lightList.iterator();

        while (iterator.hasNext()) {
            LightHandle l = iterator.next();
            if (light < l.value) light = l.value;

            l.timeout--;
            if (l.timeout <= 0) {
                iterator.remove();
            }
        }

        if (light != worldObj.getBlockMetadata(xCoord, yCoord, zCoord)) {

            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, light, 2);
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
        }
    }

    public static void addLight(World w, int x, int y, int z, int light, int timeout) {
        Block block = w.getBlock(x, y, z);
        if (block != Vars.lightBlock) {
            if (block != Blocks.air) return;
            w.setBlock(x, y, z, Vars.lightBlock, light, 2);
        }

        TileEntity t = w.getTileEntity(x, y, z);
        if (t != null && t instanceof LightBlockEntity)
            ((LightBlockEntity) t).addLight(light, timeout);
        else
            Utils.println("ASSERT if(t != null && t instanceof LightBlockEntity)");
    }

    public static void addLight(Coordonate coord, int light, int timeout) {
        addLight(coord.world(), coord.x, coord.y, coord.z, light, timeout);
    }

	/*public static void removeLight(Coordonate coord, int light) {
		int blockId = coord.getBlockId();
		if (blockId != Eln.lightBlockId) return;
		((LightBlockEntity)coord.getTileEntity()).removeLight(light);
	}
	
	public static void replaceLight(Coordonate coord, int oldLight, int newLight) {
		int blockId = coord.getBlockId();
		if (blockId != Eln.lightBlockId) {
			//coord.setBlock(Eln.lightBlockId, newLight);
			Utils.println("ASSERT public static void replaceLight(Coordonate coord, int oldLight, int newLight) " + coord);
			return;
		}
		((LightBlockEntity)coord.getTileEntity()).replaceLight(oldLight,newLight);
	}*/

	/*public int getClientLight() {
		return clientLight;
	}
	
	int clientLight = 0;*/
}

package mods.eln.item;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.lampitem.BoilerplateLampData;
import mods.eln.item.lampitem.LampDescriptor;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlock;
import mods.eln.node.NodeManager;
import mods.eln.sixnode.currentcable.CurrentCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ConfigCopyToolDescriptor extends GenericItemUsingDamageDescriptor {
    public ConfigCopyToolDescriptor(String name) { super(name); }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float vx, float vy, float vz) {
        if(world.isRemote) return false;

        Block block = world.getBlock(x, y, z);

        if(block instanceof NodeBlock) {
            NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordinate(x, y, z, world));
            if(node != null) {
                node.onBlockActivated(player, Direction.fromIntMinecraftSide(side), vx, vy, vz);
            }
            return true;
        }
        return false;
    }

    public static boolean readCableType(NBTTagCompound compound, IInventory inv, int slot, EntityPlayer invoker, boolean acceptSignalCable) {
        String name = "cable";

        if (compound.hasKey(name + "Type")) {
            int type = compound.getInteger(name + "Type");
            GenericItemBlockUsingDamageDescriptor desc = Eln.sixNodeItem.getDescriptor(type);

            if (desc instanceof ElectricalCableDescriptor) {
                if (!(((ElectricalCableDescriptor) desc).signalWire && !acceptSignalCable)) {
                    return readCableType(compound, inv, slot, invoker);
                }
            } else if (desc instanceof CurrentCableDescriptor) {
                return readCableType(compound, inv, slot, invoker);
            }
        }

        return false;
    }

    public static boolean readCableType(NBTTagCompound compound, IInventory inv, int slot, EntityPlayer invoker) {
        return readCableType(compound, "cable", inv, slot, invoker);
    }

    public static boolean readCableType(NBTTagCompound compound, String name, IInventory inv, int slot, EntityPlayer invoker) {
        if(compound.hasKey(name + "Type")) {
            int amt = 1;
            if(compound.hasKey(name + "Amt")) {
                amt = compound.getInteger(name + "Amt");
            }
            int type = compound.getInteger(name + "Type");
            ItemStack stackInSlot = inv.getStackInSlot(slot);
            if (stackInSlot != null) {
                GenericItemBlockUsingDamageDescriptor thisCableDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(stackInSlot, ElectricalCableDescriptor.class);
                if (thisCableDesc == null) thisCableDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(stackInSlot, CurrentCableDescriptor.class);
                if(thisCableDesc != null) {
                    final GenericItemBlockUsingDamageDescriptor cableDesc = thisCableDesc;
                    (new ItemMovingHelper() {
                        @Override
                        public boolean acceptsStack(ItemStack stack) {
                            return cableDesc.checkSameItemStack(stack);
                        }

                        @Override
                        public ItemStack newStackOfSize(int items) {
                            return cableDesc.newItemStack(items);
                        }
                    }).move(invoker.inventory, inv, slot, 0);
                }
            }
            if(type != -1) {
                GenericItemBlockUsingDamageDescriptor cableDesc = Eln.sixNodeItem.getDescriptor(type);
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        if (cableDesc != null) {
                            return cableDesc.checkSameItemStack(stack);
                        } else return false;
                    }

                    @Override @Nullable
                    public ItemStack newStackOfSize(int items) {
                        if (cableDesc != null) {
                            return cableDesc.newItemStack(items);
                        } else return null; // This is okay because all usages of this function handle null values safely.
                    }
                }).move(invoker.inventory, inv, slot, amt);
            }
            return true;
        }
        return false;
    }

    public static void writeCableType(NBTTagCompound compound, ItemStack stack) {
        writeCableType(compound, "cable", stack);
    }

    public static void writeCableType(NBTTagCompound compound, String name, ItemStack stack) {
        if(stack != null) {
            Eln.logger.info("CCT Copy: " + name + "Amt: " + stack.stackSize);
            compound.setInteger(name + "Amt", stack.stackSize);
        }
        GenericItemBlockUsingDamageDescriptor desc = GenericItemBlockUsingDamageDescriptor.getDescriptor(stack);
        if(desc != null) {
            Eln.logger.info("CCT Copy: " + name + "Type: " + desc.parentItemDamage);
            compound.setInteger(name + "Type", desc.parentItemDamage);
        } else {
            Eln.logger.info("CCT Copy: " + name + "Type: -1");
            compound.setInteger(name + "Type", -1);
        }
    }

    public static boolean readLampDescriptor(NBTTagCompound compound, String name, IInventory inv, int slot, EntityPlayer invoker, BoilerplateLampData[] acceptedLampTypes) {
        if (compound.hasKey(name)) {
            String type = compound.getString(name);
            GenericItemUsingDamageDescriptor desc = GenericItemUsingDamageDescriptor.getByName(type);

            if (desc instanceof LampDescriptor) {
                for (BoilerplateLampData acceptedLampType : acceptedLampTypes) {
                    if (((LampDescriptor) desc).getLampData().getTechnology() == acceptedLampType) {
                        return readGenDescriptor(compound, name, inv, slot, invoker);
                    }
                }
            }
        }

        return false;
    }

    public static boolean readGenDescriptor(NBTTagCompound compound, String name, IInventory inv, int slot, EntityPlayer invoker) {
        if(compound.hasKey(name)) {
            int amt = 1;
            if(compound.hasKey(name + "Amt")) {
                amt = compound.getInteger(name + "Amt");
            }
            String type = compound.getString(name);
            GenericItemUsingDamageDescriptor desc = GenericItemUsingDamageDescriptor.getDescriptor(inv.getStackInSlot(slot));
            if(desc != null) {
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        return desc.checkSameItemStack(stack);
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return desc.newItemStack(items);
                    }
                }).move(invoker.inventory, inv, slot, 0);
            }
            if(!type.equals(GenericItemUsingDamageDescriptor.INVALID_NAME)) {
                GenericItemUsingDamageDescriptor newDesc = GenericItemUsingDamageDescriptor.getByName(type);
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        if (newDesc != null) {
                            return newDesc.checkSameItemStack(stack);
                        } else return false;
                    }

                    @Override @Nullable
                    public ItemStack newStackOfSize(int items) {
                        if (newDesc != null) {
                            return newDesc.newItemStack(items);
                        } else return null; // This is okay because all usages of this function handle null values safely.
                    }
                }).move(invoker.inventory, inv, slot, amt);
            }
            return true;
        }
        return false;
    }

    public static void writeGenDescriptor(NBTTagCompound compound, String name, ItemStack stack) {
        if(stack != null) {
            Eln.logger.info("CCT Copy: " + name + "Amt: " + stack.stackSize);
            compound.setInteger(name + "Amt", stack.stackSize);
        }
        GenericItemUsingDamageDescriptor desc = GenericItemUsingDamageDescriptor.getDescriptor(stack);
        if(desc != null) {
            Eln.logger.info("CCT Copy: " + name + " " + desc.name);
            compound.setString(name, desc.name);
        } else {
            Eln.logger.info("CCT Copy: " + name + " Invalid Descriptor");
            compound.setString(name, GenericItemUsingDamageDescriptor.INVALID_NAME);
        }
    }

    public static boolean readVanillaStack(NBTTagCompound compound, String name, IInventory inv, int slot, EntityPlayer invoker) {
        if(compound.hasKey(name)) {
            int amt = 1;
            if(compound.hasKey(name + "Amt")) {
                amt = compound.getInteger(name + "Amt");
            }
            int itemId = compound.getInteger(name);
            ItemStack current = inv.getStackInSlot(slot);
            if(current != null) {
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        return current.getItem() == stack.getItem();
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return new ItemStack(current.getItem(), items);
                    }
                }).move(invoker.inventory, inv, slot, 0);
            }
            if(itemId >= 0) {
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        return Item.getIdFromItem(stack.getItem()) == itemId;
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return new ItemStack(Item.getItemById(itemId), items);
                    }
                }).move(invoker.inventory, inv, slot, amt);
            }
            return true;
        }
        return false;
    }

    public static void writeVanillaStack(NBTTagCompound compound, String name, ItemStack stack) {
        if(stack == null) {
            Eln.logger.info("CCT Copy: " + name + "Amt: 0");
            compound.setInteger(name, -1);
            compound.setInteger(name + "Amt", 0);
        } else {
            Eln.logger.info("CCT Copy: " + name + " " + Item.getIdFromItem(stack.getItem()));
            Eln.logger.info("CCT Copy: " + name + "Amt: " + stack.stackSize);
            compound.setInteger(name, Item.getIdFromItem(stack.getItem()));
            compound.setInteger(name + "Amt", stack.stackSize);
        }
    }
}

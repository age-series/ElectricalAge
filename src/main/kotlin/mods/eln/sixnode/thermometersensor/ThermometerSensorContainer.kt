package mods.eln.sixnode.thermometersensor

import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

class ThermometerSensorContainer(player: EntityPlayer, inventory: IInventory) :
    BasicContainer(player, inventory, arrayOf<Slot>())

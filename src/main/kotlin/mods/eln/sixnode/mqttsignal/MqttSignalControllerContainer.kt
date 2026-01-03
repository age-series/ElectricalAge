package mods.eln.sixnode.mqttsignal

import mods.eln.misc.BasicContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class MqttSignalControllerContainer(player: EntityPlayer, inventory: IInventory) :
    BasicContainer(player, inventory, emptyArray())

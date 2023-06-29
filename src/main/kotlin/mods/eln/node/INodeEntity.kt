package mods.eln.node

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.misc.Direction
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import java.io.DataInputStream

interface INodeEntity {
    val nodeUuid: String
    fun serverPublishUnserialize(stream: DataInputStream)
    fun serverPacketUnserialize(stream: DataInputStream)

    @SideOnly(Side.CLIENT)
    fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen?
    fun newContainer(side: Direction, player: EntityPlayer): Container?
}

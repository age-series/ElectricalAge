package mods.eln.sixnode.lampsocket.objrender

import mods.eln.sixnode.lampsocket.LampSocketDescriptor
import mods.eln.sixnode.lampsocket.LampSocketRender
import net.minecraftforge.client.IItemRenderer.ItemRenderType

interface ILampSocketObjRender {

    fun draw(descriptor: LampSocketDescriptor, type: ItemRenderType, distanceToPlayer: Double)

    fun draw(render: LampSocketRender, distanceToPlayer: Double)

}
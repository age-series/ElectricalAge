package mods.eln.sixnode.lampsocket.objrender

import mods.eln.sixnode.lampsocket.LampSocketDescriptor
import mods.eln.sixnode.lampsocket.LampSocketRender
import net.minecraftforge.client.IItemRenderer.ItemRenderType

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
interface ILampSocketObjRender {

    fun draw(descriptor: LampSocketDescriptor, type: ItemRenderType, distanceToPlayer: Double)

    fun draw(render: LampSocketRender, distanceToPlayer: Double)

}
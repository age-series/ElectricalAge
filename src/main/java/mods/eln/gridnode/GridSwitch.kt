package mods.eln.gridnode

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11


class GridSwitchDescriptor(
    name: String
): TransparentNodeDescriptor(name, GridSwitchElement::class.java, GridSwitchRender::class.java) {

    val gridSwitchModel = Eln.obj.getObj("ElnGridSwitch")

    val objectList = listOf<String>(
        "Contact_M2_ContactMMesh_2.001",
        "Contact_F2_ContactFMesh_2",
        "Belt_pulley_2_BeltPulleyMesh_2",
        "Belt_pulley_1_BeltPulleyMesh_1",
        "Contact_M1_ContactMMesh_1",
        "Contact_F1_ContactFMesh_1",
        "Lid_1_LidMesh_1",
        "Lid_2_LidMesh_2",
        "SwitchBase_SwitchBaseMesh",
        "Belt_1_BeltMesh_1",
        "Belt_2_BeltMesh_2",
        "g0_Cube",
        "g1_Cube",
        "p0_Cube",
        "p1_Cube"
    ).map { gridSwitchModel.getPart(it)}


    fun draw() {
        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(2.5, -0.5, 1.5)
        objectList.forEach {
            it.draw()
        }
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?) = false

    override fun renderItem(type: IItemRenderer.ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        draw()
    }
}

class GridSwitchElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?) = null

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        return 0
    }

    override fun multiMeterString(side: Direction?) = ""

    override fun thermoMeterString(side: Direction?) = ""

    override fun initialize() {
        connect()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun hasGui() = false
}

class GridSwitchRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, descriptor) {
    init {
        this.transparentNodedescriptor = descriptor as GridSwitchDescriptor
    }

    override fun draw() {
        front.glRotateXnRef()
        (this.transparentNodedescriptor as GridSwitchDescriptor).draw()
    }
}


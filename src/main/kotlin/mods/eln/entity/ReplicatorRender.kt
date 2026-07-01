package mods.eln.entity

import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.entity.RenderLiving
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation

class ReplicatorRender(model: ModelBase, shadowSize: Float) : RenderLiving(model, shadowSize) {
    override fun getEntityTexture(entity: Entity): ResourceLocation {
        return RES
    }

    companion object {
        private val RES = ResourceLocation("eln:textures/entity/replicator.png")
    }
}

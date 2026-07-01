package mods.eln.entity

import net.minecraft.entity.EntityCreature
import net.minecraft.entity.ai.EntityAIBase
import net.minecraft.entity.ai.RandomPositionGenerator

class ConfigurableAiWander(
    private val entity: EntityCreature,
    private val speed: Double,
    private val randLimit: Int
) : EntityAIBase() {
    private var xPosition = 0.0
    private var yPosition = 0.0
    private var zPosition = 0.0

    init {
        mutexBits = 1
    }

    override fun shouldExecute(): Boolean {
        if (entity.age >= 100) return false
        if (entity.rng.nextInt(randLimit) != 0) return false

        val vec3 = RandomPositionGenerator.findRandomTarget(entity, 10, 7) ?: return false
        xPosition = vec3.xCoord
        yPosition = vec3.yCoord
        zPosition = vec3.zCoord
        return true
    }

    override fun continueExecuting(): Boolean {
        return !entity.navigator.noPath()
    }

    override fun startExecuting() {
        entity.navigator.tryMoveToXYZ(xPosition, yPosition, zPosition, speed)
    }
}

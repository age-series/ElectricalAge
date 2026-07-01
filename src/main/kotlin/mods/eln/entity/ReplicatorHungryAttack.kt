package mods.eln.entity

import net.minecraft.entity.ai.EntityAINearestAttackableTarget

class ReplicatorHungryAttack(
    private val replicator: ReplicatorEntity,
    targetClass: Class<*>,
    targetChance: Int,
    shouldCheckSight: Boolean
) : EntityAINearestAttackableTarget(replicator, targetClass, targetChance, shouldCheckSight) {
    override fun shouldExecute(): Boolean {
        if (replicator.hunger < replicator.hungerToCanibal) return false
        return super.shouldExecute()
    }
}

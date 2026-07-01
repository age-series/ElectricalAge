package mods.eln.entity

import mods.eln.misc.Utils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.entity.EnumCreatureAttribute
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.EntityAIAttackOnCollide
import net.minecraft.entity.ai.EntityAIHurtByTarget
import net.minecraft.entity.ai.EntityAILookIdle
import net.minecraft.entity.ai.EntityAIMoveThroughVillage
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction
import net.minecraft.entity.ai.EntityAINearestAttackableTarget
import net.minecraft.entity.ai.EntityAISwimming
import net.minecraft.entity.ai.EntityAIWatchClosest
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.DamageSource
import net.minecraft.world.World
import java.util.ArrayList
import java.util.Map
import java.util.Random

class ReplicatorEntity(world: World) : EntityMob(world) {
    var isSpawnedFromWeather = false
    var hungerTime = 10.0 * 60.0
    var hungerToEnergy = 10.0 * hungerTime
    var energyToDuplicate = 10000.0
    var hungerToDuplicate = -energyToDuplicate / hungerToEnergy
    var hungerToCanibal = 0.6
    var hunger = (Math.random() - 0.5) * 0.3

    init {
        func_110163_bv()
        setSize(0.3f, 0.7f)

        val replicatorAi = ReplicatorCableAI(this)
        var priority = 0
        tasks.addTask(priority++, EntityAISwimming(this))
        tasks.addTask(priority++, EntityAIAttackOnCollide(this, EntityPlayer::class.java, 1.0, false))
        tasks.addTask(priority++, EntityAIAttackOnCollide(this, EntityVillager::class.java, 1.0, true))
        tasks.addTask(priority++, EntityAIAttackOnCollide(this, ReplicatorEntity::class.java, 1.0, true))
        tasks.addTask(priority++, replicatorAi)
        tasks.addTask(priority++, EntityAIMoveTowardsRestriction(this, 1.0))
        tasks.addTask(priority++, EntityAIMoveThroughVillage(this, 1.0, false))
        tasks.addTask(priority++, ConfigurableAiWander(this, 1.0, 20))
        tasks.addTask(priority, EntityAIWatchClosest(this, EntityPlayer::class.java, 8.0f))
        tasks.addTask(priority++, EntityAILookIdle(this))

        priority = 1
        targetTasks.addTask(priority++, EntityAIHurtByTarget(this, true))
        targetTasks.addTask(priority, EntityAINearestAttackableTarget(this, EntityPlayer::class.java, 0, true))
        targetTasks.addTask(priority, EntityAINearestAttackableTarget(this, EntityVillager::class.java, 0, false))
        targetTasks.addTask(priority++, ReplicatorHungryAttack(this, ReplicatorEntity::class.java, 0, false))
    }

    override fun attackEntityAsMob(entity: Entity): Boolean {
        if (entity is ReplicatorEntity) {
            hunger -= 0.4
            entity.hunger += 0.4
        }
        return super.attackEntityAsMob(entity)
    }

    override fun updateAITick() {
        super.updateAITick()
        hunger += 0.05 / hungerTime

        if (hunger > 1 && Math.random() < 0.05 / 5) {
            attackEntityFrom(DamageSource.starve, 1.0f)
        }
        if (hunger < 0.5 && Math.random() * 10 < 0.05) {
            heal(1.0f)
        }
        if (hunger < hungerToDuplicate) {
            val entityLiving = ReplicatorEntity(worldObj)
            entityLiving.setLocationAndAngles(posX, posY, posZ, 0.0f, 0.0f)
            entityLiving.rotationYawHead = entityLiving.rotationYaw
            entityLiving.renderYawOffset = entityLiving.rotationYaw
            worldObj.spawnEntityInWorld(entityLiving)
            entityLiving.playLivingSound()
            hunger = 0.0
        }
    }

    fun eatElectricity(energy: Double) {
        hunger -= Math.min(0.001, energy / hungerToEnergy)
    }

    override fun applyEntityAttributes() {
        super.applyEntityAttributes()
        getEntityAttribute(SharedMonsterAttributes.followRange).baseValue = 8.0
        getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue = 8.0
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).baseValue = 0.23000000417232513
        getEntityAttribute(SharedMonsterAttributes.attackDamage).baseValue = 3.0
    }

    override fun isAIEnabled(): Boolean = true

    override fun getLivingSound(): String = "mob.silverfish.say"

    override fun getHurtSound(): String = "mob.silverfish.hit"

    override fun getDeathSound(): String = "mob.silverfish.kill"

    override fun dropFewItems(wasRecentlyHit: Boolean, lootingLevel: Int) {
        if (dropList.isNotEmpty()) {
            entityDropItem(dropList[Random().nextInt(dropList.size)].copy(), 0.5f)
        }

        if (isSpawnedFromWeather && Math.random() < 0.33) {
            for (entryObject in EntityList.IDtoClassMapping.entries) {
                val entry = entryObject as Map.Entry<*, *>
                if (entry.value == ReplicatorEntity::class.java) {
                    entityDropItem(
                        ItemStack(Item.itemRegistry.getObject("spawn_egg") as Item, 1, entry.key as Int),
                        0.5f
                    )
                    break
                }
            }
        }
    }

    override fun getCreatureAttribute(): EnumCreatureAttribute {
        return EnumCreatureAttribute.UNDEFINED
    }

    override fun writeEntityToNBT(nbt: NBTTagCompound) {
        super.writeEntityToNBT(nbt)
        nbt.setDouble("ElnHunger", hunger)
        nbt.setBoolean("isSpawnedFromWeather", isSpawnedFromWeather)
    }

    override fun readEntityFromNBT(nbt: NBTTagCompound) {
        super.readEntityFromNBT(nbt)
        hunger = nbt.getDouble("ElnHunger")
        isSpawnedFromWeather = nbt.getBoolean("isSpawnedFromWeather")
        Utils.println("[Replicator] $posX $posY $posZ ")
    }

    companion object {
        @JvmField
        val dropList = ArrayList<ItemStack>()
    }
}

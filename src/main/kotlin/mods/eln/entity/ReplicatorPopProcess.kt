package mods.eln.entity

import cpw.mods.fml.common.FMLCommonHandler
import mods.eln.Eln
import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.EnumSkyBlock

class ReplicatorPopProcess : IProcess {
    override fun process(time: Double) {
        val world = FMLCommonHandler.instance().minecraftServerInstance.worldServers[0]
        val maxReplicators = Eln.config.getIntOrElse("entities.replicator.maxCount", 100)
        val popPerSecondPerPlayer =
            Eln.config.getDoubleOrElse("entities.replicator.thunderSpawnPerSecondPerPlayer", 1.0 / 120.0)

        var replicatorCount = 0
        for (entity in world.loadedEntityList) {
            if (entity is ReplicatorEntity) {
                replicatorCount++
                if (replicatorCount > maxReplicators) {
                    entity.setDead()
                }
            }
        }

        if (world.difficultySetting == EnumDifficulty.PEACEFUL) return

        if (world.worldInfo.isThundering) {
            for (obj in world.playerEntities) {
                val player = obj as EntityPlayerMP
                if (Math.random() * world.playerEntities.size < time * popPerSecondPerPlayer && player.worldObj == world) {
                    val x = (player.posX + Utils.rand(-100.0, 100.0)).toInt()
                    val z = (player.posZ + Utils.rand(-100.0, 100.0)).toInt()
                    var y = 2
                    Utils.println("POP")

                    if (!world.blockExists(x, y, z)) break

                    while (world.getBlock(x, y, z) != Blocks.air || Utils.getLight(world, EnumSkyBlock.Block, x, y, z) > 6) {
                        y++
                    }

                    val entityLiving = ReplicatorEntity(world)
                    entityLiving.setLocationAndAngles(x + 0.5, y.toDouble(), z + 0.5, 0.0f, 0.0f)
                    entityLiving.rotationYawHead = entityLiving.rotationYaw
                    entityLiving.renderYawOffset = entityLiving.rotationYaw
                    world.spawnEntityInWorld(entityLiving)
                    entityLiving.playLivingSound()
                    entityLiving.isSpawnedFromWeather = true
                    Utils.println("Spawn Replicator at $x $y $z")
                }
            }
        }
    }
}

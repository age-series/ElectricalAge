package mods.eln.entity

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils.println
import mods.eln.misc.Utils.rand
import mods.eln.sim.IProcess
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityMob

/**
 * This class is used by lamps to kill mobs within a range (except for Enderman, Replicators, and Withers)
 *
 * @param coordinate Location where to center range of mob decimation
 * @param range the range in which to remove mobs from
 */
class EnemyRemover(private val coordinate: Coordinate, private val range: Int) : IProcess {
    var timerCounter = 0.0
    val timerPeriod = 0.212
    var oldList: List<*>? = null

    override fun process(time: Double) {
        //Monster killing must be active before continuing :
        if (!Eln.instance.killMonstersAroundLamps) return
        timerCounter += time
        if (timerCounter > timerPeriod) {
            timerCounter -= rand(1.0, 1.5) * timerPeriod
            val list = coordinate.world().getEntitiesWithinAABB(EntityMob::class.java, coordinate.getAxisAlignedBB(range + 8))
            for (o in list) {
                val mob = o as EntityMob
                if (oldList == null || !oldList!!.contains(o)) {
                    if (coordinate.distanceTo(mob) < range) {
                        if (o !is ReplicatorEntity && o !is EntityWither && o !is EntityEnderman) {
                            mob.setDead()
                            println("MonsterPopFreeProcess killed a ${o.javaClass.name}")
                        }
                    }
                }
            }
            oldList = list
        }
    }

    init {
        if (range < 1) {
            throw Exception("The range is too small")
        }
    }
}

package mods.eln.sim.process.destruct

import mods.eln.Eln
import mods.eln.mechanical.ShaftElement
import mods.eln.misc.Coordinate
import mods.eln.node.six.SixNodeElement
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import java.lang.RuntimeException

class WorldExplosion : IDestructible {
    private var origin: Any
    var coordinate: Coordinate
    private var strength = 0f
    var type: String = ""

    constructor(e: ShaftElement) {
        coordinate = e.coordonate()
        type = e.toString()
        origin = e
    }

    constructor(e: SixNodeElement) {
        coordinate = e.coordinate?: throw RuntimeException("WorldExplosion: Null SixNode Element received")
        type = e.toString()
        origin = e
    }

    constructor(e: TransparentNodeElement) {
        coordinate = e.coordinate()
        type = e.toString()
        origin = e
    }

    constructor(e: EnergyConverterElnToOtherNode) {
        coordinate = e.coordinate
        type = e.toString()
        origin = e
    }

    fun cableExplosion(): WorldExplosion {
        strength = 1.5f
        return this
    }

    fun machineExplosion(): WorldExplosion {
        strength = 3f
        return this
    }

    override fun destructImpl() {
        if (Eln.explosionEnable) coordinate.world().createExplosion(
            null as Entity?,
            coordinate.x.toDouble(),
            coordinate.y.toDouble(),
            coordinate.z.toDouble(),
            strength,
            true
        ) else coordinate.world().setBlock(
            coordinate.x, coordinate.y, coordinate.z, Blocks.air
        )
    }

    override fun describe(): String {
        return String.format("%s (%s)", type, coordinate.toString())
    }
}

package mods.eln.sim.destruct

import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.node.six.SixNodeElement
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode
import net.minecraft.init.Blocks

class WorldExplosion : IDestructable {

    internal var c: Coordonate
    internal var strength: Float = 0.toFloat()
    internal var type: String

    constructor(c: Coordonate) {
        this.c = c
        this.type = "Generic Explosion at $c"
    }

    constructor(e: SixNodeElement) {
        this.c = e.coordonate
        this.type = e.toString()
    }

    constructor(e: TransparentNodeElement) {
        this.c = e.coordonate()
        this.type = e.toString()
    }

    constructor(e: EnergyConverterElnToOtherNode) {
        this.c = e.coordonate
        this.type = e.toString()
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
        if (Eln.explosionEnable)
            c.world().createExplosion(null, c.x.toDouble(), c.y.toDouble(), c.z.toDouble(), strength, true)
        else
            c.world().setBlock(c.x, c.y, c.z, Blocks.air)
    }

    override fun describe(): String {
        return String.format("%s (%s)", this.type, this.c.toString())
    }
}

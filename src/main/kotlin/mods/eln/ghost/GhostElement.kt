package mods.eln.ghost

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class GhostElement : INBTTReady {
    @JvmField
    var elementCoordinate: Coordinate? = null
    var observatorCoordonate: Coordinate? = null
    var uUID = 0

    constructor() {}
    constructor(elementCoordinate: Coordinate, observatorCoordinate: Coordinate, UUID: Int) {
        this.elementCoordinate = elementCoordinate
        observatorCoordonate = observatorCoordinate
        uUID = UUID
    }

    fun breakBlock() {
        Eln.ghostManager.removeGhost(elementCoordinate)
        val observer = Eln.ghostManager.getObserver(observatorCoordonate)
        observer?.ghostDestroyed(uUID)
    }

    fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        val observer = Eln.ghostManager.getObserver(observatorCoordonate)
        return observer?.ghostBlockActivated(uUID, entityPlayer!!, side!!, vx, vy, vz) ?: false
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        elementCoordinate = Coordinate(nbt, str + "elemCoord")
        observatorCoordonate = Coordinate(nbt, str + "obserCoord")
        uUID = nbt.getInteger(str + "UUID")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        elementCoordinate!!.writeToNBT(nbt, str + "elemCoord")
        observatorCoordonate!!.writeToNBT(nbt, str + "obserCoord")
        nbt.setInteger(str + "UUID", uUID)
    }
}

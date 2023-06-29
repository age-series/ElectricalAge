package mods.eln.simplenode.energyconverter

import cofh.api.energy.IEnergyHandler
import mods.eln.Other
import mods.eln.misc.Direction

object EnergyConverterElnToOtherFireWallRf {

    fun updateEntity(e: EnergyConverterElnToOtherEntity) {
        if (e.worldObj.isRemote) return
        if (e.node == null) return
        val node = e.node as EnergyConverterElnToOtherNode

        val energySinkList: List<Pair<IEnergyHandler, Direction>> = Direction.all
            .mapNotNull { Pair(it.applyToTileEntity(e), it) }
            .filter{ it.first is IEnergyHandler }
            .map { Pair(it.first as IEnergyHandler, it.second) }
        if (energySinkList.isEmpty()) return
        val rfUsed = energySinkList.map {
            val rfAvailable = (node.availableEnergyInModUnits(Other.getWattsToRf()) / energySinkList.size)
            // receiveEnergy takes RF in, gives out RF
            val rfUsed = it.first.receiveEnergy(it.second.toForge(), rfAvailable.toInt(), false).toDouble()
            rfUsed
        }.sum()
        node.drawEnergy(rfUsed, Other.getWattsToRf())
    }
}

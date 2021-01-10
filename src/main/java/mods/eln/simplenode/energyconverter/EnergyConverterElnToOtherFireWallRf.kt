package mods.eln.simplenode.energyconverter

import cofh.api.energy.IEnergyHandler
import mods.eln.Other
import mods.eln.misc.Direction
import mods.eln.node.simple.SimpleNode

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
        val pMax = node.getOtherModEnergyBuffer(Other.getElnToTeConversionRatio())

        val energyUsed = energySinkList.map {
            it.first.receiveEnergy(it.second.toForge(), pMax.toInt() / energySinkList.size, false).toDouble()
        }.sum()
        node.drawEnergy(energyUsed, Other.getElnToTeConversionRatio())
    }
}

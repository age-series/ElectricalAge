package mods.eln.sixnode.electricalcable

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.plotVolt
import mods.eln.misc.Utils.println
import mods.eln.node.NodeBase
import mods.eln.node.NodeConnection
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.util.EnumChatFormatting
import java.util.*

class ElectricalSignalBusCableElement(sixNode: SixNode?, side: Direction?, descriptor: SixNodeDescriptor?) : ElectricalCableElement(sixNode, side, descriptor) {
    var coloredElectricalLoads: Array<NbtElectricalLoad>
    override fun initialize() {
        for (load in coloredElectricalLoads) {
            descriptor.applyTo(load)
        }
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        val color = mask shr NodeBase.maskColorShift and 0xF
        val load: ElectricalLoad = coloredElectricalLoads[color]
        println("ESBCE.gEL: mask $mask, color $color, load $load")
        return load
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = LinkedHashMap()
        val arry = arrayOfNulls<String>(coloredElectricalLoads.size / 4)
        var t = ""
        for (i in coloredElectricalLoads.indices) {
            t += wool_to_chat[15 - i].toString() + plotVolt("", coloredElectricalLoads[i].voltage).trim { it <= ' ' } + "\u00A7r, "
            if ((i + 1) % 4 == 0) {
                arry[(i - 3) / 4] = t.substring(0, t.length - 2)
                t = ""
            }
        }
        for (i in arry.indices) {
            info[String.format("%02d-%02d", 4 * i, 4 * i + 3)] = arry[i]!!
        }
        return info
    }

    override fun multiMeterString(): String {
        var t = ""
        for (i in 0..15) {
            t += wool_to_chat[15 - i].toString() + plotVolt("", coloredElectricalLoads[i].voltage).trim { it <= ' ' } + " "
        }
        return t
    }

    override fun newConnectionAt(connection: NodeConnection?, isA: Boolean) {
        if (!isA) return  // Only run for one of the connection attempts between two ESBCEs; choose A arbitrarily.
        println("ESBCE.nCA:")
        val other = connection!!.N2
        println("\tother is: $other")
        if (other is SixNode) {
            println("\tother is SixNode")
            val el = other.getElement(connection.dir2.applyLRDU(connection.lrdu2))
            println("\tel is: $el")
            if (el is ElectricalSignalBusCableElement) {
                println("\tel is ESBCE too")
                // Connect the other 15 colors, too
                for (i in 1..15) {
                    val econ = ElectricalConnection(
                        coloredElectricalLoads[i],
                        el.coloredElectricalLoads[i]
                    )
                    Eln.simulator.addElectricalComponent(econ)
                    connection.addConnection(econ)
                }
            }
        }
        println("ESBCE.nCA ends.")
    }

    companion object {
        var wool_to_chat = arrayOf(
            EnumChatFormatting.WHITE,
            EnumChatFormatting.GOLD,
            EnumChatFormatting.LIGHT_PURPLE,
            EnumChatFormatting.BLUE,
            EnumChatFormatting.YELLOW,
            EnumChatFormatting.GREEN,
            EnumChatFormatting.RED,
            EnumChatFormatting.DARK_GRAY,
            EnumChatFormatting.GRAY,
            EnumChatFormatting.DARK_AQUA,
            EnumChatFormatting.DARK_PURPLE,
            EnumChatFormatting.DARK_BLUE,
            EnumChatFormatting.AQUA,  // FIXME: supposed to be brown
            EnumChatFormatting.DARK_GREEN,
            EnumChatFormatting.DARK_RED,
            EnumChatFormatting.BLACK)
    }

    init {
        colorCare = 0
        electricalLoadList.remove(electricalLoad)
        electricalLoad = null

        coloredElectricalLoads = (0 .. 15).mapIndexed {
            idx, _ ->
            val load = NbtElectricalLoad("color$idx")
            load.setCanBeSimplifiedByLine(true)
            electricalLoadList.add(load)
            load
        }.toTypedArray()
    }
}

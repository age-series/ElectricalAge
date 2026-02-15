package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.IFluidHandler
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

/**
 * A comparator-alike. It doesn't "compare" anything, though.
 */
class ScannerDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, ScannerElement::class.java, ScannerRender::class.java) {

    val main = obj.getPart("main")!!
    val leds = arrayOf("LED_0", "LED_1").map { obj.getPart(it) }.requireNoNulls()

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    fun draw(mode: ScanMode) {
        main.draw()
        leds[mode.value.toInt()].draw()
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Scans blocks to produce signals."))
        list.add(tr("- For tanks, outputs fill percentage."))
        // This string sucks. I can't use the normal Java method to fix this problem. TODO: fix this so that it is readable on windowed games.
        list.add(tr("- For inventories, outputs either total fill or fraction of slots with any items."))
        list.add(tr("Right-click to change mode."))
        list.add(tr("Otherwise behaves as a vanilla comparator."))
    }
}

enum class ScanMode(val value: Byte) {
    SIMPLE(0), SLOTS(1);

    companion object {
        private val map = ScanMode.values().associateBy(ScanMode::value);
        fun fromByte(type: Byte) = map[type]
    }
}

class ScannerElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {
    val output = NbtElectricalGateOutput("signal")
    val outputProcess = NbtElectricalGateOutputProcess("signalP", output)

    var mode = ScanMode.SIMPLE

    val updater = IProcess {
        val appliedLRDU = side.applyLRDU(front)
        val scannedCoord = Coordinate(coordinate!!).apply {
            move(appliedLRDU)
        }
        val targetSide: ForgeDirection = appliedLRDU.inverse.toForge()
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        var out: Double? = null
        if (te != null) {
            out = scanTileEntity(te, targetSide)
        }
        if (out == null) {
            out = scanBlock(scannedCoord, targetSide)
        }
        outputProcess.outputNormalized = out
    }

    init {
        electricalLoadList.add(output)
        electricalComponentList.add(outputProcess)
        slowProcessList.add(updater)
    }

    private fun scanBlock(scannedCoord: Coordinate, targetSide: ForgeDirection): Double {
        val block = scannedCoord.block
        return when {
            block.hasComparatorInputOverride() ->
                block.getComparatorInputOverride(scannedCoord.world(), scannedCoord.x, scannedCoord.y, scannedCoord.z, targetSide.ordinal) / 15.0
            block.isOpaqueCube -> 1.0
            block.isAir(scannedCoord.world(), scannedCoord.x, scannedCoord.y, scannedCoord.z) -> 0.0
            else -> 1.0/3.0
        }
    }

    private fun scanTileEntity(te: TileEntity, targetSide: ForgeDirection): Double? {
        if (te is IFluidHandler) {
            val info = te.getTankInfo(targetSide)?.filter { it.capacity > 0 } ?: return 0.0
            if (info.isEmpty()) return 0.0
            return info.sumOf {
                (it.fluid?.amount ?: 0).toDouble() / it.capacity
            } / info.size
        } else if (hbmFluidUserClass?.isInstance(te) == true) {
            return scanHbmFluidUser(te)
        } else if (te is ISidedInventory) {
            var sum = 0
            var limit = 0
            val slots = te.getAccessibleSlotsFromSide(targetSide.ordinal)
            when (mode) {
                ScanMode.SIMPLE -> slots.forEach {
                        sum += te.getStackInSlot(it)?.stackSize ?: 0
                        limit += te.inventoryStackLimit
                    }

                ScanMode.SLOTS -> slots.forEach {
                    sum += if ((te.getStackInSlot(it)?.stackSize ?: 0) > 0) 1 else 0
                    limit += 1
                }
            }
            return sum.toDouble() / limit
        } else if (te is IInventory) {
            val sum = when (mode) {
                ScanMode.SIMPLE -> (0..te.sizeInventory - 1).sumOf {
                    te.getStackInSlot(it)?.stackSize ?: 0
                }.toDouble()

                ScanMode.SLOTS -> (0..te.sizeInventory - 1).count {
                    (te.getStackInSlot(it)?.stackSize ?: 0) > 0
                }.toDouble() * te.inventoryStackLimit
            }
            return sum / te.inventoryStackLimit / te.sizeInventory
        } else {
            return null
        }
    }

    private fun scanHbmFluidUser(te: TileEntity): Double? {
        val method = hbmGetAllTanksMethod ?: return null
        val tanks = try {
            method.invoke(te)
        } catch (_: Exception) {
            return null
        } ?: return 0.0

        val tankCount = Array.getLength(tanks)
        if (tankCount == 0) return 0.0

        var sum = 0.0
        var count = 0
        for (idx in 0 until tankCount) {
            val tank = Array.get(tanks, idx) ?: continue
            val (fillMethod, maxFillMethod) = hbmTankMethodCache.getOrPut(tank.javaClass) {
                val fill = tank.javaClass.getMethod("getFill")
                val max = tank.javaClass.getMethod("getMaxFill")
                fill to max
            }
            val maxFill = (maxFillMethod.invoke(tank) as? Number)?.toDouble() ?: continue
            if (maxFill <= 0.0) continue
            val fill = (fillMethod.invoke(tank) as? Number)?.toDouble() ?: 0.0
            sum += fill / maxFill
            count++
        }
        return if (count == 0) 0.0 else sum / count
    }

    companion object {
        private val hbmFluidUserClass: Class<*>? by lazy {
            runCatching { Class.forName("api.hbm.fluidmk2.IFluidUserMK2") }.getOrNull()
        }
        private val hbmGetAllTanksMethod: Method? by lazy {
            hbmFluidUserClass?.getMethod("getAllTanks")
        }
        private val hbmTankMethodCache = mutableMapOf<Class<*>, Pair<Method, Method>>()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (onBlockActivatedRotate(entityPlayer)) return true
        if (entityPlayer.isHoldingMeter()) return false
        mode = when (mode) {
            ScanMode.SIMPLE -> ScanMode.SLOTS
            ScanMode.SLOTS -> ScanMode.SIMPLE
        }
        needPublish()
        return true
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? = output
    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.inverse() -> NodeBase.maskElectricalOutputGate
        else -> 0
    }

    override fun multiMeterString(): String {
        return "Mode: ${tr(mode.name.lowercase()
            .replaceFirstChar { it.titlecase(Locale.getDefault()) })}, Value: ${Utils.plotPercent("", outputProcess.outputNormalized)}"
    }

    override fun thermoMeterString(): String = ""

    override fun initialize() {
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeByte(mode.value.toInt())
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("mode", mode.value)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        mode = ScanMode.fromByte(nbt.getByte("mode"))!!
    }
}


class ScannerRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(entity, side, descriptor) {
    val desc = descriptor as ScannerDescriptor
    var mode = ScanMode.SIMPLE

    override fun draw() {
        super.draw()
        front!!.glRotateOnX()
        desc.draw(mode)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        mode = ScanMode.fromByte(stream.readByte())!!
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? = Eln.instance.signalCableDescriptor.render
}

package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.RealisticEnum
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.CurrentSource
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class RegulatorChipMode {
    LDO,
    BOOST
}

class RegulatorChipDescriptor(
    name: String,
    obj: Obj3D?,
    modelPartName: String,
    iconName: String,
    val mode: RegulatorChipMode,
    val outputVoltage: Double,
    val currentLimit: Double,
    val dropoutVoltage: Double = 1.2,
    val minimumInputVoltage: Double = 0.0,
    val maximumInputVoltage: Double = outputVoltage * 2.0,
    val efficiency: Double = 0.9
) : SixNodeDescriptor(name, RegulatorChipElement::class.java, RegulatorChipRender::class.java, iconName) {
    private val case = obj?.getPart("Case")
    private val top = obj?.getPart(modelPartName)
    private val input = obj?.getPart("Input1")
    private val output = obj?.getPart("Output")
    val nominalMinimumInputVoltage = when (mode) {
        RegulatorChipMode.LDO -> outputVoltage + dropoutVoltage
        RegulatorChipMode.BOOST -> minimumInputVoltage
    }
    val quiescentInputResistance: Double = when (mode) {
        RegulatorChipMode.LDO -> nominalMinimumInputVoltage / QUIESCENT_INPUT_CURRENT
        RegulatorChipMode.BOOST -> nominalMinimumInputVoltage / QUIESCENT_INPUT_CURRENT
    }.takeIf { it.isFinite() && it > 0.0 } ?: Double.POSITIVE_INFINITY

    init {
        voltageLevelColor = VoltageLevelColor.fromVoltage(outputVoltage)
    }

    fun draw() {
        input?.draw()
        output?.draw()
        case?.draw()
        top?.draw()
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean = true

    override fun shouldUseRenderHelper(
        type: IItemRenderer.ItemRenderType,
        item: ItemStack,
        helper: IItemRenderer.ItemRendererHelper
    ): Boolean = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun shouldUseRenderHelperEln(
        type: IItemRenderer.ItemRenderType?,
        item: ItemStack?,
        helper: IItemRenderer.ItemRendererHelper?
    ): Boolean = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f)
            GL11.glScalef(1.25f, 1.25f, 1.25f)
            GL11.glRotatef(-90.0f, 0.0f, 1.0f, 0.0f)
            draw()
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU? =
        super.getFrontFromPlace(side, player)!!.left()

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    override fun addInformation(
        itemStack: ItemStack?,
        entityPlayer: EntityPlayer?,
        list: MutableList<String>?,
        par4: Boolean
    ) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (list == null) return

        list.add(
            tr(
                "Input range: %1$ to %2$.",
                Utils.plotVolt("", nominalMinimumInputVoltage),
                Utils.plotVolt("", maximumInputVoltage)
            )
        )
        list.add(tr("Output voltage: %1$", Utils.plotVolt("", outputVoltage)))
        list.add(tr("Current limit: %1$", Utils.plotAmpere("", currentLimit)))
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        list?.add(tr("Acts as an ideal regulated voltage source inside its input voltage range."))
        list?.add(
            tr(
                "Input draw and current limiting are simplified; operation outside the input range is not based on real device data."
            )
        )
        return RealisticEnum.IDEAL
    }

    companion object {
        const val QUIESCENT_INPUT_CURRENT = 0.005
    }
}

class RegulatorChipElement(node: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElement(node, side, sixNodeDescriptor) {
    private val descriptor = sixNodeDescriptor as RegulatorChipDescriptor

    private val inputLoad = NbtElectricalLoad("input")
    private val outputLoad = NbtElectricalLoad("output")
    private val inputSink = CurrentSource("inputSink", inputLoad, null)
    private val outputSource = VoltageSource("outputSource", outputLoad, null)
    private val quiescentLoad = if (descriptor.quiescentInputResistance.isFinite()) {
        Resistor(inputLoad, null).setResistance(descriptor.quiescentInputResistance)
    } else {
        null
    }
    private val regulator = RegulatorChipController(descriptor, inputLoad, outputLoad, inputSink, outputSource)

    init {
        electricalLoadList.add(inputLoad)
        electricalLoadList.add(outputLoad)
        electricalComponentList.add(inputSink)
        electricalComponentList.add(outputSource)
        if (quiescentLoad != null) electricalComponentList.add(quiescentLoad)
        electricalProcessList.add(regulator)
    }

    override fun initialize() {
        Eln.applySmallRs(inputLoad)
        Eln.applySmallRs(outputLoad)
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? = when (lrdu) {
        front -> outputLoad
        front.inverse() -> inputLoad
        else -> null
    }

    override fun getConnectionMask(lrdu: LRDU): Int = when (lrdu) {
        front, front.inverse() -> NodeBase.maskElectricalPower
        else -> 0
    }

    override fun multiMeterString(): String =
        Utils.plotVolt("In: ", inputLoad.voltage) + " " +
            Utils.plotVolt("Out: ", outputLoad.voltage) + " " +
            Utils.plotAmpere("Iout: ", regulator.outputCurrent)

    override fun getWaila(): Map<String, String> = mutableMapOf(
        tr("Input: ") to Utils.plotUIP(inputLoad.voltage, -inputSink.current),
        tr("Output: ") to Utils.plotUIP(outputLoad.voltage, regulator.outputCurrent)
    )
}

class RegulatorChipController(
    private val descriptor: RegulatorChipDescriptor,
    private val inputLoad: NbtElectricalLoad,
    private val outputLoad: NbtElectricalLoad,
    private val inputSink: CurrentSource,
    private val outputSource: VoltageSource
) : IProcess {
    var outputCurrent = 0.0
        private set

    override fun process(time: Double) {
        val inputVoltage = inputLoad.voltage
        val availableOutputVoltage = when (descriptor.mode) {
            RegulatorChipMode.LDO -> {
                if (inputVoltage > descriptor.maximumInputVoltage) {
                    0.0
                } else if (inputVoltage >= descriptor.nominalMinimumInputVoltage) {
                    descriptor.outputVoltage
                } else {
                    max(0.0, inputVoltage - descriptor.dropoutVoltage)
                }
            }
            RegulatorChipMode.BOOST -> {
                if (inputVoltage in descriptor.minimumInputVoltage..descriptor.maximumInputVoltage) {
                    descriptor.outputVoltage
                } else {
                    0.0
                }
            }
        }

        if (!availableOutputVoltage.isFinite() || availableOutputVoltage <= 0.0) {
            idle()
            return
        }

        val previousVoltage = outputSource.voltage.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val previousCurrent = abs(outputSource.current).takeIf { it.isFinite() } ?: 0.0
        val targetCurrent = if (previousVoltage > 0.0 && previousCurrent > 0.0) {
            availableOutputVoltage * previousCurrent / previousVoltage
        } else {
            0.0
        }
        outputCurrent = min(targetCurrent, descriptor.currentLimit)
        val sourceVoltage = if (targetCurrent > descriptor.currentLimit && targetCurrent > 0.0) {
            availableOutputVoltage * descriptor.currentLimit / targetCurrent
        } else {
            availableOutputVoltage
        }
        outputSource.setVoltage(sourceVoltage)

        val outputPower = (sourceVoltage * outputCurrent).coerceAtLeast(0.0)
        val inputCurrent = when (descriptor.mode) {
            RegulatorChipMode.LDO -> outputCurrent
            RegulatorChipMode.BOOST -> if (inputVoltage > 0.0) outputPower / (inputVoltage * descriptor.efficiency) else 0.0
        }
        inputSink.current = -if (inputCurrent.isFinite()) inputCurrent.coerceAtLeast(0.0) else 0.0
    }

    private fun idle() {
        outputCurrent = 0.0
        inputSink.current = 0.0
        outputSource.setVoltage(outputLoad.voltage.takeIf { it.isFinite() } ?: 0.0)
    }
}

class RegulatorChipRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(entity, side, descriptor) {
    private val descriptor = descriptor as RegulatorChipDescriptor

    override fun draw() {
        super.draw()
        front!!.glRotateOnX()
        descriptor.draw()
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? = when (lrdu) {
        front, front!!.inverse() -> Eln.instance.lowVoltageCableDescriptor.render
        else -> null
    }
}

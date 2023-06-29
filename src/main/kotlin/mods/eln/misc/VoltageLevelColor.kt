package mods.eln.misc

import mods.eln.Eln
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import org.lwjgl.opengl.GL11

enum class VoltageLevelColor(private val voltageLevel: String?) {
    None(null), Neutral("neutral"), SignalVoltage("signal"), LowVoltage("low"), MediumVoltage("medium"), HighVoltage("high"), VeryHighVoltage("veryhigh"), Grid("grid"), HighGrid("highgrid"), Thermal("thermal");

    fun drawIconBackground(type: ItemRenderType) {
        if (!Eln.noVoltageBackground && voltageLevel != null && type == ItemRenderType.INVENTORY || type == ItemRenderType.FIRST_PERSON_MAP) {
            UtilsClient.drawIcon(type, ResourceLocation("eln", "textures/voltages/$voltageLevel.png"))
        }
    }

    fun setGLColor() {
        when (this) {
            SignalVoltage -> GL11.glColor3f(.80f, .87f, .82f)
            LowVoltage -> GL11.glColor3f(.55f, .84f, .68f)
            MediumVoltage -> GL11.glColor3f(.55f, .74f, .85f)
            HighVoltage -> GL11.glColor3f(.96f, .80f, .56f)
            VeryHighVoltage -> GL11.glColor3f(.86f, .58f, .55f)
            None, Neutral -> {}
            else -> {}
        }
    }

    companion object {
        @JvmStatic
        fun fromVoltage(voltage: Double): VoltageLevelColor {
            return if (voltage < 0) {
                None
            } else if (voltage <= 2 * Eln.LVU) {
                LowVoltage
            } else if (voltage <= 2 * Eln.MVU) {
                MediumVoltage
            } else if (voltage <= 2 * Eln.HVU) {
                HighVoltage
            } else if (voltage <= 2 * Eln.VVU) {
                VeryHighVoltage
            } else {
                None
            }
        }

        @JvmStatic
        fun fromCable(descriptor: ElectricalCableDescriptor?): VoltageLevelColor {
            return if (descriptor != null) {
                if (descriptor.signalWire) {
                    SignalVoltage
                } else {
                    fromVoltage(descriptor.electricalNominalVoltage)
                }
            } else {
                None
            }
        }
    }
}

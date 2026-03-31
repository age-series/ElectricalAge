package mods.eln.registration

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.ghost.GhostGroup
import mods.eln.i18n.I18N
import mods.eln.item.ElectricalFuseDescriptor
import mods.eln.item.ElectricalFuseDescriptor.Companion.BlownFuse
import mods.eln.misc.Direction
import mods.eln.misc.FunctionTableYProtect
import mods.eln.misc.IFunction
import mods.eln.misc.SeriesFunction.Companion.newE12
import mods.eln.misc.SeriesFunction.Companion.newE6
import mods.eln.misc.NominalVoltage
import mods.eln.sixnode.signalinductor.SignalInductorDescriptor
import mods.eln.sixnode.*
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor
import mods.eln.sixnode.batterycharger.BatteryChargerDescriptor
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.currentrelay.CurrentRelayDescriptor
import mods.eln.sixnode.diode.DiodeDescriptor
import mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement
import mods.eln.sixnode.electricalcable.MoltenMetalPileDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableElement
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCablePalette
import mods.eln.sixnode.electricalcable.UtilityCableRender
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor
import mods.eln.sixnode.electricaldigitaldisplay.ElectricalDigitalDisplayDescriptor
import mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor
import mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj
import mods.eln.sixnode.electricalhumiditysensor.ElectricalHumiditySensorDescriptor
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor
import mods.eln.sixnode.electricalmath.ElectricalMathDescriptor
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor
import mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor
import mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor
import mods.eln.sixnode.electricalswitch.ElectricalSwitchDescriptor
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor
import mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor
import mods.eln.sixnode.mqttmeter.MqttEnergyMeterElement
import mods.eln.sixnode.mqttmeter.MqttEnergyMeterRender
import mods.eln.sixnode.mqttsignal.MqttSignalControllerDescriptor
import mods.eln.sixnode.groundcable.GroundCableDescriptor
import mods.eln.sixnode.hub.HubDescriptor
import mods.eln.sixnode.lampsocket.LampSocketDescriptor
import mods.eln.sixnode.lampsocket.objrender.LampSocketStandardObjRender
import mods.eln.sixnode.lampsocket.objrender.LampSocketSuspendedObjRender
import mods.eln.sixnode.lampsupply.LampSupplyDescriptor
import mods.eln.sixnode.logicgate.*
import mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor
import mods.eln.sixnode.powersocket.PowerSocketDescriptor
import mods.eln.sixnode.resistor.ResistorDescriptor
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor
import mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor
import mods.eln.sixnode.thermometersensor.ThermometerSensorDescriptor
import mods.eln.sixnode.tutorialsign.TutorialSignDescriptor
import mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterDescriptor
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor
import net.minecraft.creativetab.CreativeTabs

object SixNodeRegistration {

    private fun <T : GenericItemBlockUsingDamageDescriptor> T.inTab(tab: CreativeTabs) = apply {
        setCreativeTab(tab)
    }

    private fun <T : GenericItemBlockUsingDamageDescriptor> T.power() = inTab(Eln.creativeTabPowerElectronics)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.signal() = inTab(Eln.creativeTabSignalProcessing)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.lighting() = inTab(Eln.creativeTabLighting)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.cables() = inTab(Eln.creativeTabCables)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.tools() = inTab(Eln.creativeTabToolsArmor)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.materials() = inTab(Eln.creativeTabOresMaterials)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.machines() = inTab(Eln.creativeTabMachines)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.creative() = inTab(Eln.creativeTabCreative)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.other() = inTab(Eln.creativeTabOther)

    fun registerSix() {
        Eln.sixNodeItem.setCreativeTabForGroup(2, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(3, Eln.creativeTabCreative)
        Eln.sixNodeItem.setCreativeTabForGroup(32, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(33, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(34, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(35, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(36, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(37, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(48, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(126, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(127, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(64, Eln.creativeTabLighting)
        Eln.sixNodeItem.setCreativeTabForGroup(67, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(65, Eln.creativeTabLighting)
        Eln.sixNodeItem.setCreativeTabForGroup(92, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(93, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(94, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(95, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(96, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(97, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(98, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(100, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(101, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(102, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(103, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(104, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(108, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(109, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(117, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(118, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(124, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(66, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(116, Eln.creativeTabMachines)
        Eln.sixNodeItem.setCreativeTabForGroup(125, Eln.creativeTabToolsArmor)
        registerGround(2)
        registerElectricalSource(3)
        registerElectricalCable(32)
        registerCurrentCables(33)
        registerUtilityCables(34, 35, 36, 37)
        registerThermalCable(48)
        registerCurrentRelays(126)
        if (Eln.instance.isDevelopmentRun) {
            registerConduit(127)
        }
        registerLampSocket(64)
        registerPowerSocket(67)
        registerLampSupply(65)

        registerWirelessSignal(92)
        registerElectricalDataLogger(93)
        registerElectricalRelay(94)
        registerElectricalGateSource(95)
        registerPassiveComponent(96)
        registerSwitch(97)
        registerElectricalManager(98)
        registerElectricalSensor(100)
        registerThermalSensor(101)
        registerElectricalVuMeter(102)
        registerElectricalAlarm(103)
        registerElectricalEnvironmentalSensor(104)
        registerElectricalRedstone(108)
        registerElectricalGate(109)
        registerSixNodeMisc(117)
        registerLogicalGates(118)
        registerAnalogChips(124)
        registerBatteryCharger(66)

        registerTreeResinCollector(116)
        registerPortableNaN() // 125
    }

    private fun registerGround(id: Int) {
        var subId: Int

        run {
            subId = 0
            val name = I18N.TR_NAME(I18N.Type.NONE, "Ground Cable")
            val desc = GroundCableDescriptor(name, Eln.obj.getObj("groundcable")).creative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 8
            val name = I18N.TR_NAME(I18N.Type.NONE, "Hub")
            val desc = HubDescriptor(name, Eln.obj.getObj("hub")).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalSource(id: Int) {
        var subId: Int

        run {
            subId = 0
            val name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Source")
            val desc = ElectricalSourceDescriptor(name, Eln.obj.getObj("voltagesource"), false).creative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            val name = I18N.TR_NAME(I18N.Type.NONE, "Signal Source")
            val desc =
                ElectricalSourceDescriptor(name, Eln.obj.getObj("signalsource"), true).creative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            val name = I18N.TR_NAME(I18N.Type.NONE, "Current Source")
            val desc = CurrentSourceDescriptor(name, Eln.obj.getObj("currentsource")).creative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalCable(id: Int) {
        var subId: Int
        var name: String?

        var desc: ElectricalCableDescriptor
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Cable")
            Eln.instance.stdCableRenderSignal = CableRenderDescriptor("eln", "sprites/cable.png", 0.95f, 0.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRenderSignal, "For signal transmission.", true).signal()
            desc.hideFromCreative()
            Eln.instance.signalCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.SVU, Eln.SVP, 0.02 / 50 * Eln.SVU, Eln.SVU * 1.3,
                Eln.SVP * 1.2, 0.5, Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, 1.0
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Low Voltage Cable")
            Eln.instance.stdCableRender50V = CableRenderDescriptor("eln", "sprites/cable.png", 1.95f, 0.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender50V, "For low voltage with high current.", false).power()
            Eln.instance.lowVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.LVU, Eln.instance.LVP(), 0.2 / 20 / 100, Eln.LVU * 1.3, Eln.instance.LVP() * 1.2, 20.0, Eln.cableWarmLimit,
                -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender50V, "For low voltage with high current.", false)
            desc.setPhysicalConstantLikeNormalCable(
                Eln.LVU, Eln.instance.LVP() / 4, 0.2 / 20 / 100, Eln.LVU * 1.3, Eln.instance.LVP() * 1.2, 20.0,
                Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
        }

        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Medium Voltage Cable")
            Eln.instance.stdCableRender200V = CableRenderDescriptor("eln", "sprites/cable.png", 2.95f, 0.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender200V, "miaou", false).power()
            Eln.instance.meduimVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.MVU, Eln.instance.MVP(), 0.10 / 20 / 100, Eln.MVU * 1.3, Eln.instance.MVP() * 1.2, 30.0, Eln.cableWarmLimit,
                -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "High Voltage Cable")
            Eln.instance.stdCableRender800V = CableRenderDescriptor("eln", "sprites/cable.png", 3.95f, 1.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender800V, "miaou2", false).power()
            Eln.instance.highVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.HVU, Eln.instance.HVP(), 0.025 * 5 / 4 / 20 / 100, Eln.HVU * 1.3, Eln.instance.HVP() * 1.2, 40.0,
                Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }


        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Very High Voltage Cable")
            Eln.instance.stdCableRender3200V = CableRenderDescriptor("eln", "sprites/cableVHV.png", 3.95f, 1.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender3200V, "miaou2", false).power()
            Eln.instance.veryHighVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.VVU, Eln.instance.VVP(), 0.025 * 5 / 4 / 20 / 8 / 100, Eln.VVU * 1.3, Eln.instance.VVP() * 1.2, 40.0,
                Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 24
            name = I18N.TR_NAME(I18N.Type.NONE, "Creative Cable")
            Eln.instance.stdCableRenderCreative = CableRenderDescriptor("eln", "sprites/cablecreative.png", 8.0f, 4.0f)
            desc = ElectricalCableDescriptor(
                name, Eln.instance.stdCableRenderCreative, "Experience the power of " +
                        "Microresistance", false
            ).creative()
            Eln.instance.creativeCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.VVU * 16, Eln.VVU * 16 * Eln.instance.VVP(), 1e-9,  //what!?
                Eln.VVU * 16 * 1.3, Eln.VVU * 16 * Eln.instance.VVP() * 1.2, 40.0, Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime,
                Eln.cableThermalConductionTao
            )
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 20
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Bus Cable")
            Eln.instance.stdCableRenderSignalBus = CableRenderDescriptor("eln", "sprites/cable.png", 3.95f, 3.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRenderSignalBus, "For transmitting many signals.", true).signal()
            Eln.instance.signalBusCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.SVU, Eln.SVP, 0.02 / 50 * Eln.SVU, Eln.SVU * 1.3,
                Eln.SVP * 1.2, 0.5, Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, 1.0
            )
            desc.ElementClass = ElectricalSignalBusCableElement::class.java
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerCurrentCables(id: Int) {
        var subId: Int
        var name: String?
        var desc: CurrentCableDescriptor
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Low Current Cable")
            Eln.instance.lowCurrentCableRender = CableRenderDescriptor("eln", "sprites/currentcable.png", 1.9f, 0.9f)
            desc = CurrentCableDescriptor(name, Eln.instance.lowCurrentCableRender, "Current based electrical cable").power()
            desc.setPhysicalConstantLikeNormalCable(5.0)
            desc.hideFromCreative()
            Eln.instance.lowCurrentCableDescriptor = desc
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Medium Current Cable")
            Eln.instance.mediumCurrentCableRender = CableRenderDescriptor("eln", "sprites/currentcable.png", 2.9f, 1.9f)
            desc = CurrentCableDescriptor(name, Eln.instance.mediumCurrentCableRender, "Current based electrical cable").power()
            desc.setPhysicalConstantLikeNormalCable(20.0)
            desc.hideFromCreative()
            Eln.instance.mediumCurrentCableDescriptor = desc
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "High Current Cable")
            Eln.instance.highCurrentCableRender = CableRenderDescriptor("eln", "sprites/currentcable.png", 3.9f, 1.9f)
            desc = CurrentCableDescriptor(name, Eln.instance.highCurrentCableRender, "Current based electrical cable").power()
            desc.setPhysicalConstantLikeNormalCable(100.0)
            desc.hideFromCreative()
            Eln.instance.highCurrentCableDescriptor = desc
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private data class UtilitySingleSpec(
        val awgLabel: String,
        val metricLabel: String,
        val metricArea: Double,
        val ampacity: Double,
        val insulatedVoltage: Double,
        val insulatedTemperature: Double,
        val poleEligible: Boolean = false
    )

    private data class UtilityMultiSpec(
        val label: String,
        val metricLabel: String,
        val metricArea: Double,
        val conductorCount: Int,
        val ampacity: Double,
        val insulationVoltage: Double,
        val maxTemperature: Double,
        val flatStyle: Boolean,
        val palettes: Array<UtilityCablePalette>,
        val poleEligible: Boolean = false
    )

    private class UtilityDescriptorAllocator(private val groups: IntArray) {
        private var groupIndex = 0
        private var subId = 0

        fun nextId(): Int {
            check(groupIndex < groups.size) { "Out of six-node groups while registering utility cables" }
            val id = subId + (groups[groupIndex] shl 6)
            subId++
            if (subId >= 64) {
                groupIndex++
                subId = 0
            }
            return id
        }
    }

    private fun registerUtilityCables(vararg groups: Int) {
        val allocator = UtilityDescriptorAllocator(groups)
        val renderCache = linkedMapOf<String, CableRenderDescriptor>()
        val signalCategoryAreaThresholdMm2 = 1.5

        fun <T : GenericItemBlockUsingDamageDescriptor> categoriseUtilityCable(desc: T, conductorAreaMm2: Double): T {
            return if (conductorAreaMm2 < signalCategoryAreaThresholdMm2) desc.signal() else desc.cables()
        }

        fun <T : GenericItemBlockUsingDamageDescriptor> maybeHideSmallAluminum(desc: T, material: UtilityCableMaterial, conductorAreaMm2: Double): T {
            if (material == UtilityCableMaterial.ALUMINUM && conductorAreaMm2 < signalCategoryAreaThresholdMm2) {
                desc.hideFromCreative()
            }
            return desc
        }

        fun render(texture: String, width: Float, height: Float): CableRenderDescriptor {
            val key = "$texture:$width:$height"
            return renderCache.getOrPut(key) { CableRenderDescriptor("eln", texture, width, height) }
        }

        fun newMoltenPileDescriptor(material: UtilityCableMaterial): MoltenMetalPileDescriptor {
            return MoltenMetalPileDescriptor(
                "${material.label} Molten Metal Pile",
                material,
                render("sprites/cable_melted.png", 2.4f, 0.9f)
            ).also { desc ->
                desc.setDefaultIcon("arcmetalblock")
                desc.hideFromCreative()
            }
        }

        fun iconNameFor(spec: UtilitySingleSpec, insulated: Boolean): String {
            return when {
                !insulated -> "utilitybareicon"
                spec.metricArea <= 0.5 -> "utilitysignalicon"
                spec.metricArea <= 2.5 -> "utilityinsulatedsmallicon"
                spec.metricArea <= 35.0 -> "utilityinsulatedmediumicon"
                else -> "utilityinsulatedlargeicon"
            }
        }

        fun renderWidth(metricArea: Double, conductorCount: Int, flatStyle: Boolean): Pair<Float, Float> {
            val singleWidth = when {
                metricArea <= 0.5 -> 0.65f
                metricArea <= 2.5 -> 0.95f
                metricArea <= 10.0 -> 1.25f
                metricArea <= 35.0 -> 1.75f
                metricArea <= 120.0 -> 2.35f
                else -> 3.25f
            }
            if (flatStyle) {
                val totalVolume = (singleWidth * singleWidth * conductorCount * 0.38f).coerceAtLeast(1.0f)
                val width = (conductorCount * 0.92f + singleWidth * 0.35f).coerceAtLeast(2.2f)
                val height = (totalVolume / width).coerceIn(0.95f, 2.2f)
                return Pair(width, height)
            }
            return if (conductorCount <= 1) {
                Pair(singleWidth, maxOf(0.65f, singleWidth - 0.3f))
            } else {
                val body = singleWidth + 0.75f + (conductorCount - 2) * 0.4f
                Pair(body, body)
            }
        }

        fun configureElectricalConstants(desc: UtilityCableDescriptor, area: Double, ampacity: Double, nominalVoltage: Double, maxTemperature: Double) {
            val baseAreaMm2 = 0.14
            val baseTotalResistanceOhms = 0.001
            val minimumTotalResistanceOhms = 0.00001
            val totalResistanceOhms = (baseTotalResistanceOhms * (baseAreaMm2 / area.coerceAtLeast(baseAreaMm2)))
                .coerceAtLeast(minimumTotalResistanceOhms)
            val dropFactor = (totalResistanceOhms * ampacity / nominalVoltage).coerceAtLeast(1.0e-6)
            val nominalPower = nominalVoltage * ampacity
            desc.setPhysicalConstantLikeNormalCable(
                nominalVoltage,
                nominalPower,
                dropFactor,
                maxOf(nominalVoltage * 1.5, desc.insulationVoltageRating),
                nominalPower * 1.1,
                20.0,
                maxTemperature,
                -100.0,
                Eln.cableHeatingTime,
                Eln.cableThermalConductionTao
            )
            desc.ElementClass = UtilityCableElement::class.java
            desc.RenderClass = UtilityCableRender::class.java
        }

        fun newSingleDescriptor(spec: UtilitySingleSpec, material: UtilityCableMaterial, insulated: Boolean, melted: Boolean = false): UtilityCableDescriptor {
            val insulationLabel = when {
                melted -> "Melted"
                insulated -> "${spec.insulatedVoltage.toInt()}V"
                else -> "Bare"
            }
            val name = "${material.label} ${spec.awgLabel} Cable $insulationLabel"
            val (width, height) = renderWidth(spec.metricArea, 1, false)
            val texture = when {
                melted -> "sprites/cable_melted.png"
                insulated -> "sprites/cable.png"
                else -> "sprites/currentcable.png"
            }
            return UtilityCableDescriptor(
                name = name,
                render = render(texture, width, height),
                description = if (insulated) "Insulated single-conductor utility cable." else "Bare single-conductor utility cable.",
                sizeLabel = spec.awgLabel,
                metricSizeLabel = spec.metricLabel,
                material = material,
                totalConductorAreaMm2 = spec.metricArea,
                conductorCount = 1,
                insulated = insulated && !melted,
                insulationVoltageRating = if (insulated && !melted) spec.insulatedVoltage else 0.0,
                melted = melted,
                meltTemperatureCelsius = if (melted) spec.insulatedTemperature else if (insulated) spec.insulatedTemperature else 180.0,
                flatStyle = false,
                colorPalettes = arrayOf(UtilityCablePalette("single", "Single", intArrayOf(0))),
                poleEligible = spec.poleEligible && !melted
            ).also { desc ->
                desc.setDefaultIcon(iconNameFor(spec, insulated))
                configureElectricalConstants(desc, spec.metricArea, spec.ampacity, maxOf(50.0, spec.insulatedVoltage), if (insulated || melted) spec.insulatedTemperature else 180.0)
                if (melted) {
                    desc.hideFromCreative()
                }
            }
        }

        fun newMultiDescriptor(spec: UtilityMultiSpec, material: UtilityCableMaterial, melted: Boolean = false): UtilityCableDescriptor {
            val name = "${material.label} ${spec.label} Cable ${if (melted) "Melted" else "${spec.insulationVoltage.toInt()}V"}"
            val (width, height) = renderWidth(spec.metricArea, spec.conductorCount, spec.flatStyle)
            val effectiveCount = if (melted) 1 else spec.conductorCount
            val palettes = if (melted) arrayOf(UtilityCablePalette("melted", "Melted", intArrayOf(0))) else spec.palettes
            return UtilityCableDescriptor(
                name = name,
                render = render(
                    if (melted) "sprites/cable_melted_multi.png" else "sprites/cable.png",
                    width,
                    height
                ),
                description = "Insulated multi-conductor utility cable.",
                sizeLabel = spec.label,
                metricSizeLabel = spec.metricLabel,
                material = material,
                totalConductorAreaMm2 = spec.metricArea * spec.conductorCount,
                conductorCount = effectiveCount,
                insulated = !melted,
                insulationVoltageRating = if (melted) 0.0 else spec.insulationVoltage,
                melted = melted,
                meltTemperatureCelsius = spec.maxTemperature,
                flatStyle = spec.flatStyle,
                colorPalettes = palettes,
                poleEligible = spec.poleEligible && !melted
            ).also { desc ->
                desc.setDefaultIcon("utilityinsulatedlargeicon")
                configureElectricalConstants(desc, spec.metricArea, spec.ampacity, spec.insulationVoltage, spec.maxTemperature)
                if (melted) {
                    desc.hideFromCreative()
                }
            }
        }

        val singles = listOf(
            UtilitySingleSpec("26 AWG", "0.14", 0.14, 1.0, 300.0, 80.0),
            UtilitySingleSpec("24 AWG", "0.25", 0.25, 2.0, 300.0, 80.0),
            UtilitySingleSpec("22 AWG", "0.34", 0.34, 3.0, 300.0, 80.0),
            UtilitySingleSpec("20 AWG", "0.5", 0.5, 5.0, 300.0, 80.0),
            UtilitySingleSpec("18 AWG", "0.75", 0.75, 7.0, 300.0, 90.0),
            UtilitySingleSpec("16 AWG", "1.5", 1.5, 10.0, 300.0, 90.0),
            UtilitySingleSpec("14 AWG", "2.5", 2.5, 15.0, 600.0, 105.0),
            UtilitySingleSpec("12 AWG", "4", 4.0, 20.0, 600.0, 105.0),
            UtilitySingleSpec("10 AWG", "6", 6.0, 30.0, 600.0, 105.0),
            UtilitySingleSpec("8 AWG", "10", 10.0, 40.0, 600.0, 105.0),
            UtilitySingleSpec("6 AWG", "16", 16.0, 60.0, 600.0, 105.0),
            UtilitySingleSpec("4 AWG", "25", 25.0, 80.0, 600.0, 105.0),
            UtilitySingleSpec("2 AWG", "35", 35.0, 100.0, 600.0, 105.0, poleEligible = true),
            UtilitySingleSpec("1/0 AWG", "50", 50.0, 125.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("2/0 AWG", "70", 70.0, 175.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("4/0 AWG", "120", 120.0, 260.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("250 kcmil", "120", 120.0, 290.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("350 kcmil", "185", 185.0, 350.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("500 kcmil", "240", 240.0, 430.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("750 kcmil", "400", 400.0, 600.0, 1000.0, 110.0, poleEligible = true),
            UtilitySingleSpec("1000 kcmil", "500", 500.0, 750.0, 1000.0, 110.0, poleEligible = true)
        )

        val us2 = arrayOf(
            UtilityCablePalette("us", "US", intArrayOf(0, 15, 2)),
            UtilityCablePalette("eu", "EU", intArrayOf(12, 11, 5))
        )
        val us3 = arrayOf(
            UtilityCablePalette("us", "US", intArrayOf(0, 1, 15, 2)),
            UtilityCablePalette("eu", "EU", intArrayOf(12, 15, 11, 5))
        )
        val eu3g = arrayOf(UtilityCablePalette("eu", "EU", intArrayOf(12, 11, 5)), UtilityCablePalette("us", "US", intArrayOf(15, 0, 13)))
        val eu5g = arrayOf(UtilityCablePalette("eu", "EU", intArrayOf(12, 15, 7, 11, 5)), UtilityCablePalette("us", "US", intArrayOf(15, 14, 11, 0, 13)))
        val signal2 = arrayOf(
            UtilityCablePalette("std", "Signal", intArrayOf(12, 0))
        )
        val signal3 = arrayOf(
            UtilityCablePalette("std", "Signal", intArrayOf(12, 15, 0))
        )
        val signal4 = arrayOf(
            UtilityCablePalette("std", "Signal", intArrayOf(12, 15, 2, 0))
        )
        val signal5 = arrayOf(
            UtilityCablePalette("std", "Signal", intArrayOf(12, 15, 2, 1, 0))
        )
        val signal8 = arrayOf(
            UtilityCablePalette("std", "Signal", intArrayOf(12, 15, 2, 1, 14, 4, 6, 0))
        )
        val triplex120240 = arrayOf(
            UtilityCablePalette("us", "US Split-Phase", intArrayOf(0, 12, 7)),
            UtilityCablePalette("eu", "EU", intArrayOf(4, 11, 7))
        )
        val quadruplex208480 = arrayOf(
            UtilityCablePalette("us", "US 3P", intArrayOf(0, 12, 1, 7)),
            UtilityCablePalette("eu", "EU 3P", intArrayOf(4, 11, 7, 3))
        )

        val multis = listOf(
            UtilityMultiSpec("2C 18 AWG", "0.75", 0.75, 2, 7.0, 300.0, 90.0, false, signal2),
            UtilityMultiSpec("3C 18 AWG", "0.75", 0.75, 3, 7.0, 300.0, 90.0, false, signal3),
            UtilityMultiSpec("4C 18 AWG", "0.75", 0.75, 4, 7.0, 300.0, 90.0, false, signal4),
            UtilityMultiSpec("5C 18 AWG", "0.75", 0.75, 5, 7.0, 300.0, 90.0, false, signal5),
            UtilityMultiSpec("8C 18 AWG", "0.75", 0.75, 8, 7.0, 300.0, 90.0, false, signal8),
            UtilityMultiSpec("2C 20 AWG", "0.5", 0.5, 2, 5.0, 300.0, 80.0, false, signal2),
            UtilityMultiSpec("3C 20 AWG", "0.5", 0.5, 3, 5.0, 300.0, 80.0, false, signal3),
            UtilityMultiSpec("4C 20 AWG", "0.5", 0.5, 4, 5.0, 300.0, 80.0, false, signal4),
            UtilityMultiSpec("5C 20 AWG", "0.5", 0.5, 5, 5.0, 300.0, 80.0, false, signal5),
            UtilityMultiSpec("8C 20 AWG", "0.5", 0.5, 8, 5.0, 300.0, 80.0, false, signal8),
            UtilityMultiSpec("14/2", "2.5", 2.5, 3, 15.0, 600.0, 105.0, true, us2),
            UtilityMultiSpec("14/3", "2.5", 2.5, 4, 15.0, 600.0, 105.0, true, us3),
            UtilityMultiSpec("12/2", "4", 4.0, 3, 20.0, 600.0, 105.0, true, us2),
            UtilityMultiSpec("12/3", "4", 4.0, 4, 20.0, 600.0, 105.0, true, us3),
            UtilityMultiSpec("10/2", "6", 6.0, 3, 30.0, 600.0, 105.0, true, us2),
            UtilityMultiSpec("10/3", "6", 6.0, 4, 30.0, 600.0, 105.0, true, us3),
            UtilityMultiSpec("8/2", "10", 10.0, 3, 40.0, 600.0, 105.0, true, us2),
            UtilityMultiSpec("8/3", "10", 10.0, 4, 40.0, 600.0, 105.0, true, us3),
            UtilityMultiSpec("3G1.5", "1.5", 1.5, 3, 10.0, 600.0, 105.0, false, eu3g),
            UtilityMultiSpec("3G2.5", "2.5", 2.5, 3, 15.0, 600.0, 105.0, false, eu3g),
            UtilityMultiSpec("3G4", "4", 4.0, 3, 20.0, 600.0, 105.0, false, eu3g),
            UtilityMultiSpec("5G1.5", "1.5", 1.5, 5, 10.0, 600.0, 105.0, false, eu5g),
            UtilityMultiSpec("5G2.5", "2.5", 2.5, 5, 15.0, 600.0, 105.0, false, eu5g),
            UtilityMultiSpec("5G4", "4", 4.0, 5, 20.0, 600.0, 105.0, false, eu5g),
            UtilityMultiSpec("Triplex 1/0", "50", 50.0, 3, 150.0, 600.0, 105.0, false, triplex120240, poleEligible = true),
            UtilityMultiSpec("Triplex 4/0", "120", 120.0, 3, 260.0, 600.0, 105.0, false, triplex120240, poleEligible = true),
            UtilityMultiSpec("Quadruplex 1/0", "50", 50.0, 4, 125.0, 600.0, 105.0, false, quadruplex208480, poleEligible = true),
            UtilityMultiSpec("Quadruplex 4/0", "120", 120.0, 4, 260.0, 600.0, 105.0, false, quadruplex208480, poleEligible = true)
        )

        val moltenPileByMaterial = UtilityCableMaterial.values().associateWith { material ->
            newMoltenPileDescriptor(material).also { desc ->
                Eln.sixNodeItem.addDescriptor(allocator.nextId(), desc.power())
            }
        }

        for (material in UtilityCableMaterial.values()) {
            for (spec in singles) {
                val bare = newSingleDescriptor(spec, material, insulated = false)
                bare.moltenPileDescriptor = moltenPileByMaterial[material]
                Eln.sixNodeItem.addDescriptor(allocator.nextId(), categoriseUtilityCable(maybeHideSmallAluminum(bare, material, spec.metricArea), spec.metricArea))

                val insulated = newSingleDescriptor(spec, material, insulated = true)
                val melted = newSingleDescriptor(spec, material, insulated = true, melted = true)
                insulated.meltedDescriptor = melted
                insulated.moltenPileDescriptor = moltenPileByMaterial[material]
                melted.moltenPileDescriptor = moltenPileByMaterial[material]
                Eln.sixNodeItem.addDescriptor(allocator.nextId(), categoriseUtilityCable(maybeHideSmallAluminum(insulated, material, spec.metricArea), spec.metricArea))
                Eln.sixNodeItem.addDescriptor(allocator.nextId(), categoriseUtilityCable(maybeHideSmallAluminum(melted, material, spec.metricArea), spec.metricArea))
            }
        }

        for (material in UtilityCableMaterial.values()) {
            for (spec in multis) {
                val multi = newMultiDescriptor(spec, material)
                val melted = newMultiDescriptor(spec, material, melted = true)
                multi.meltedDescriptor = melted
                multi.moltenPileDescriptor = moltenPileByMaterial[material]
                melted.moltenPileDescriptor = moltenPileByMaterial[material]
                Eln.sixNodeItem.addDescriptor(allocator.nextId(), categoriseUtilityCable(maybeHideSmallAluminum(multi, material, spec.metricArea), spec.metricArea))
                Eln.sixNodeItem.addDescriptor(allocator.nextId(), categoriseUtilityCable(maybeHideSmallAluminum(melted, material, spec.metricArea), spec.metricArea))
            }
        }
    }

    private fun registerCurrentRelays(id: Int) {
        var subId: Int
        var name: String?
        var desc: CurrentRelayDescriptor
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Low Current Relay")
            desc = CurrentRelayDescriptor(name, Eln.obj.getObj("RelayBig"), Eln.instance.lowCurrentCableDescriptor).power()
            desc.setPhysicalConstantLikeNormalCable(5.0)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Medium Current Relay")
            desc = CurrentRelayDescriptor(name, Eln.obj.getObj("relay800"), Eln.instance.mediumCurrentCableDescriptor).power()
            desc.setPhysicalConstantLikeNormalCable(20.0)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "High Current Relay")
            desc = CurrentRelayDescriptor(name, Eln.obj.getObj("relay800"), Eln.instance.highCurrentCableDescriptor).power()
            desc.setPhysicalConstantLikeNormalCable(100.0)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerConduit(id: Int) {
        val subId = 0
        val name = I18N.TR_NAME(I18N.Type.NONE, "Conduit")
        val desc = ConduitCableDescriptor(name, CableRenderDescriptor("eln", "sprites/conduit.png", 4f, 4f)).power()
        desc.hideFromCreative()
        Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
    }

    private fun registerThermalCable(id: Int) {
        var subId: Int
        var name: String?

        run {
            subId = 0
            name = "Removed from mod Copper Thermal Cable"
            val desc = ThermalCableDescriptor(
                name, (1000 - 20).toDouble(), -200.0, 500.0, 2000.0, 2.0, 400.0, 0.1,
                CableRenderDescriptor("eln", "sprites/tex_thermalcablebase.png", 4f, 4f), "Miaou !"
            )
            desc.addToData(false)
            desc.setDefaultIcon("empty-texture")
            Eln.sixNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Thermal Cable")
            val desc = ThermalCableDescriptor(
                name, (1000 - 20).toDouble(), -200.0, 500.0, 2000.0, 2.0, 10.0, 0.1,
                CableRenderDescriptor("eln", "sprites/tex_thermalcablebase.png", 4f, 4f), "Miaou !"
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerLampSocket(id: Int) {
        var subId: Int
        var name: String?

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Lamp Socket A")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "ClassicLampSocket"
                    ), false
                ), false, 4, 0f, 0f, 0f
            ).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Lamp Socket B Projector")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "ClassicLampSocket"
                    ), false
                ), false, 10, -90f, 90f, 0f
            ).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Robust Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "RobustLamp"
                    ), true
                ), false, 3, 0f, 0f, 0f
            ).lighting()
            desc.setInitialOrientation(-90f)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "Flat Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "FlatLamp"
                    ), true
                ), false, 3, 0f, 0f, 0f
            ).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Simple Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "SimpleLamp"
                    ), true
                ), false, 3, 0f, 0f, 0f
            ).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "Fluorescent Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "FluorescentLamp"
                    ), true
                ), false, 4, 0f, 0f, 0f
            ).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.cableLeft = false
            desc.cableRight = false
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Street Light")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "StreetLight"
                    ), true
                ), false, 0, 0f, 0f, 0f
            ).lighting()
            desc.setPlaceDirection(Direction.YN)
            val g = GhostGroup()
            g.addElement(1, 0, 0)
            g.addElement(2, 0, 0)
            desc.ghostGroup = g
            desc.renderIconInHand = true
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.cameraOpt = false
            desc.extendedRenderBounds = true
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Sconce Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketStandardObjRender(
                    Eln.obj.getObj(
                        "SconceLamp"
                    ), true
                ), true, 3, 0f, 0f, 0f
            ).lighting()
            desc.setPlaceDirection(
                arrayOf(
                    Direction.XP,
                    Direction.XN,
                    Direction.ZP,
                    Direction.ZN
                )
            )
            desc.setInitialOrientation(-90f)
            desc.setUserRotationLibertyDegrees(true)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "Suspended Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketSuspendedObjRender(
                    Eln.obj.getObj(
                        "RobustLampSuspended"
                    ), true, 3, true
                ),  // LampSocketType
                false, 3, 0f, 0f, 0f
            )
            desc.setPlaceDirection(Direction.YP)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.cameraOpt = false
        }
        run {
            subId = 13
            name = I18N.TR_NAME(I18N.Type.NONE, "Long Suspended Lamp Socket")
            val desc = LampSocketDescriptor(
                name, LampSocketSuspendedObjRender(
                    Eln.obj.getObj(
                        "RobustLampSuspended"
                    ), true, 7, true
                ), false, 4, 0f, 0f, 0f
            )
            desc.setPlaceDirection(Direction.YP)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.cameraOpt = false
        }
        Eln.sixNodeItem.addDescriptor(
            15 + (id shl 6), EmergencyLampDescriptor(
                I18N.TR_NAME(
                I18N.Type.NONE,
                    "12V Emergency Lamp"
                ), Eln.instance.lowVoltageCableDescriptor, (10 * 60 * 10).toDouble(), NominalVoltage.V12, 10.0, 5.0, 6,
                Eln.obj.getObj("EmergencyExitLighting")
            )
        )
        Eln.sixNodeItem.addDescriptor(
            16 + (id shl 6), EmergencyLampDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE, "240V Emergency " +
                            "Lamp"
                ),
                Eln.instance.meduimVoltageCableDescriptor,
                (10 * 60 * 20).toDouble(),
                NominalVoltage.V240,
                25.0,
                10.0,
                8,
                Eln.obj.getObj("EmergencyExitLighting")
            )
        )

        run {
            subId = 17
            name = I18N.TR_NAME(I18N.Type.NONE, "Suspended Lamp Socket (No Swing)")
            val desc = LampSocketDescriptor(
                name, LampSocketSuspendedObjRender(
                    Eln.obj.getObj(
                        "RobustLampSuspended"
                    ), true, 3, false
                ), false, 3, 0f, 0f, 0f
            )
            desc.setPlaceDirection(Direction.YP)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.cameraOpt = false
        }
        run {
            subId = 18
            name = I18N.TR_NAME(I18N.Type.NONE, "Long Suspended Lamp Socket (No Swing)")
            val desc = LampSocketDescriptor(
                name, LampSocketSuspendedObjRender(
                    Eln.obj.getObj(
                        "RobustLampSuspended"
                    ), true, 7, false
                ), false, 4, 0f, 0f, 0f
            )
            desc.setPlaceDirection(Direction.YP)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.cameraOpt = false
        }
    }

    private fun registerLampSupply(id: Int) {
        var subId: Int

        run {
            subId = 0
            val name = I18N.TR_NAME(I18N.Type.NONE, "120V Lamp Supply")
            val desc = LampSupplyDescriptor(name, Eln.obj.getObj("DistributionBoard"), 32, NominalVoltage.V120).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            val name = I18N.TR_NAME(I18N.Type.NONE, "240V Lamp Supply")
            val desc = LampSupplyDescriptor(name, Eln.obj.getObj("DistributionBoard"), 32, NominalVoltage.V240).lighting()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerPowerSocket(id: Int) {
        var subId: Int
        var desc: PowerSocketDescriptor
        run {
            subId = 1
            val name = I18N.TR_NAME(I18N.Type.NONE, "240V Type J Socket")
            desc = PowerSocketDescriptor(subId, name, Eln.obj.getObj("PowerSocket")).power()
            desc.setPlaceDirection(
                arrayOf(
                    Direction.XP,
                    Direction.XN,
                    Direction.ZP,
                    Direction.ZN
                )
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            val name = I18N.TR_NAME(I18N.Type.NONE, "240V Type E Socket")
            desc = PowerSocketDescriptor(subId, name, Eln.obj.getObj("PowerSocket")).power()
            desc.setPlaceDirection(
                arrayOf(
                    Direction.XP,
                    Direction.XN,
                    Direction.ZP,
                    Direction.ZN
                )
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerPassiveComponent(id: Int) {
        var subId: Int
        var name: String
        var function: IFunction
        val baseFunction = FunctionTableYProtect(
            doubleArrayOf(
                0.0, 0.01, 0.03, 0.1, 0.2, 0.4,
                0.8, 1.2
            ), 1.0, 0.0, 5.0
        )

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "10A Diode")
            function = FunctionTableYProtect(doubleArrayOf(0.0, 0.1, 0.3, 1.0, 2.0, 4.0, 8.0, 12.0), 1.0, 0.0, 100.0)
            val desc = DiodeDescriptor(
                name,
                function,
                10.0,
                1.0,
                10.0,
                Eln.sixNodeThermalLoadInitializer.copy(),
                Eln.instance.lowVoltageCableDescriptor,
                Eln.obj.getObj("PowerElectricPrimitives")
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "25A Diode")
            function = FunctionTableYProtect(
                doubleArrayOf(0.0, 0.25, 0.75, 2.5, 5.0, 10.0, 20.0, 30.0), 1.0, 0.0,
                100.0
            )
            val desc = DiodeDescriptor(
                name,
                function,
                25.0,
                1.0,
                25.0,
                Eln.sixNodeThermalLoadInitializer.copy(),
                Eln.instance.lowVoltageCableDescriptor,
                Eln.obj.getObj("PowerElectricPrimitives")
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Diode")
            function = baseFunction.duplicate(1.0, 0.1)
            val desc = DiodeDescriptor(
                name,
                function,
                0.1,
                1.0,
                0.1,
                Eln.sixNodeThermalLoadInitializer.copy(),
                Eln.instance.signalCableDescriptor,
                Eln.obj.getObj("PowerElectricPrimitives")
            ).signal()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal 20H inductor")
            val desc = SignalInductorDescriptor(name, 20.0, Eln.instance.lowVoltageCableDescriptor).signal()
            desc.setDefaultIcon("empty-texture")
            Eln.sixNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }

        run {
            subId = 32
            name = I18N.TR_NAME(I18N.Type.NONE, "Power Capacitor")
            val desc = PowerCapacitorSixDescriptor(
                name, Eln.obj.getObj(
                    "PowerElectricPrimitives"
                ), newE6(-1.0), (60 * 2000).toDouble()
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 34
            name = I18N.TR_NAME(I18N.Type.NONE, "Power Inductor")
            val desc = PowerInductorSixDescriptor(
                name, Eln.obj.getObj(
                    "PowerElectricPrimitives"
                ), newE6(-1.0)
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 35
            name = I18N.TR_NAME(I18N.Type.NONE, "Variable inductor")
            val desc = VariableInductorSixDescriptor(
                name,
                Eln.obj.getObj("PowerElectricPrimitives"),
                newE6(-1.0),
                PowerInductorSixContainer.cableStackLimit
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 36
            name = I18N.TR_NAME(I18N.Type.NONE, "Power Resistor")
            val desc = ResistorDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"),
                newE12(-2.0), 0.0, false
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 37
            name = I18N.TR_NAME(I18N.Type.NONE, "Rheostat")
            val desc = ResistorDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"),
                newE12(-2.0), 0.0, true
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 38
            name = I18N.TR_NAME(I18N.Type.NONE, "Thermistor")
            val desc = ResistorDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"),
                newE12(-2.0), -0.01, false
            ).power()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 39
            name = I18N.TR_NAME(I18N.Type.NONE, "Creative Power Capacitor")
            val desc = CreativePowerCapacitorDescriptor(name, Eln.obj.getObj("PowerElectricPrimitives")).creative()
            desc.setDefaultIcon("powercapacitor")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 40
            name = I18N.TR_NAME(I18N.Type.NONE, "Creative Power Inductor")
            val desc = CreativePowerInductorDescriptor(name, Eln.obj.getObj("PowerElectricPrimitives")).creative()
            desc.setDefaultIcon("powerinductor")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 41
            name = I18N.TR_NAME(I18N.Type.NONE, "Creative Power Resistor")
            val desc = CreativePowerResistorDescriptor(name, Eln.obj.getObj("PowerElectricPrimitives")).creative()
            desc.setDefaultIcon("powerresistor")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }



    private fun registerSwitch(id: Int) {
        var subId: Int
        var name: String?
        var desc: ElectricalSwitchDescriptor

        run {
            subId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "5A Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender50V, Eln.obj.getObj("LowVoltageSwitch"), 300.0,
                300.0 * 5.0, 0.04, 300.0, 300.0 * 5.0, Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.setDefaultIcon("switch")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "20A Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender200V, Eln.obj.getObj("LowVoltageSwitch"), 600.0,
                600.0 * 20.0, 0.01, 600.0, 600.0 * 20.0, Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.setDefaultIcon("switch")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "50A Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender800V, Eln.obj.getObj("HighVoltageSwitch"), 600.0,
                600.0 * 50.0, 0.005, 600.0, 600.0 * 50.0, Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.setDefaultIcon("switch")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "100A Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender3200V, Eln.obj.getObj("HighVoltageSwitch"), 600.0,
                600.0 * 100.0, 0.0025, 600.0, 600.0 * 100.0, Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.setDefaultIcon("switch")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Very High Voltage Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender3200V, Eln.obj.getObj("HighVoltageSwitch"), Eln.VVU,
                Eln.instance.VVP(), Eln.instance.veryHighVoltageCableDescriptor.electricalRs * 2, Eln.VVU * 1.5, Eln.instance.VVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "High Voltage Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender800V, Eln.obj.getObj("HighVoltageSwitch"), Eln.HVU,
                Eln.instance.HVP(), Eln.instance.highVoltageCableDescriptor.electricalRs * 2, Eln.HVU * 1.5, Eln.instance.HVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Low Voltage Switch")
            desc = ElectricalSwitchDescriptor(
                name,
                Eln.instance.stdCableRender50V,
                Eln.obj.getObj("LowVoltageSwitch"),
                Eln.LVU,
                Eln.instance.LVP(),
                Eln.instance.lowVoltageCableDescriptor.electricalRs * 2,
                Eln.LVU * 1.5,
                Eln.instance.LVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(),
                false
            ).power()
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "Medium Voltage Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRender200V, Eln.obj.getObj("LowVoltageSwitch"), Eln.MVU,
                Eln.instance.MVP(), Eln.instance.meduimVoltageCableDescriptor.electricalRs * 2, Eln.MVU * 1.5, Eln.instance.MVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false
            ).power()
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Switch")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRenderSignal, Eln.obj.getObj("LowVoltageSwitch"), Eln.SVU,
                Eln.SVP, 0.02, Eln.SVU * 1.5, Eln.SVP * 1.2, Eln.cableThermalLoadInitializer.copy(), true
            ).signal()
            desc.hideFromCreative()
            Eln.sixNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
        // 4 taken
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Switch with LED")
            desc = ElectricalSwitchDescriptor(
                name, Eln.instance.stdCableRenderSignal, Eln.obj.getObj("ledswitch"), Eln.SVU, Eln.SVP, 0.02,
                Eln.SVU * 1.5, Eln.SVP * 1.2, Eln.cableThermalLoadInitializer.copy(), true
            ).signal()
            desc.hideFromCreative()
            Eln.sixNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
    }

    private fun registerSixNodeMisc(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Modbus RTU")
            val desc = ModbusRtuDescriptor(
                name, Eln.obj.getObj("RTU")

            )
            if (Eln.config.getBooleanOrElse("integrations.modbus.enabled", false)) {
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            } else {
                Eln.sixNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
            }
        }

        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Analog Watch")
            val desc = ElectricalWatchDescriptor(
                name, Eln.obj.getObj("WallClock"),
                20000.0 / (3600 * 40)
            ).signal()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "Digital Watch")
            val desc = ElectricalWatchDescriptor(
                name, Eln.obj.getObj("DigitalWallClock"),
                20000.0 / (3600 * 15)
            ).signal()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Digital Display")
            val desc = ElectricalDigitalDisplayDescriptor(
                name, Eln.obj.getObj(
                    "DigitalDisplay"
                )
            ).signal()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }



        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Tutorial Sign")
            val desc = TutorialSignDescriptor(name, Eln.obj.getObj("TutoPlate")).other()
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalManager(id: Int) {
        var subId: Int
        var name: String

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "5A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 5.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "10A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 10.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "15A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 15.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "20A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 20.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "30A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 30.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "40A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 40.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "50A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 50.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 10
            name = I18N.TR_NAME(I18N.Type.NONE, "60A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 60.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 11
            name = I18N.TR_NAME(I18N.Type.NONE, "100A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 100.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 13
            name = I18N.TR_NAME(I18N.Type.NONE, "125A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 125.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 14
            name = I18N.TR_NAME(I18N.Type.NONE, "200A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 200.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 15
            name = I18N.TR_NAME(I18N.Type.NONE, "400A Electrical Breaker")
            val desc = ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"), 400.0)
            desc.setDefaultIcon("electricalbreaker")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Energy Meter")
            val desc = EnergyMeterDescriptor(name, Eln.obj.getObj("EnergyMeter"), 8, 0)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Energy Meter")
            val desc = EnergyMeterDescriptor(name, Eln.obj.getObj("AdvancedEnergyMeter"), 7, 8)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "MQTT Energy Meter")
            val desc = EnergyMeterDescriptor(
                name,
                Eln.obj.getObj("MqttEnergyMeter"),
                8,
                0,
                MqttEnergyMeterElement::class.java,
                MqttEnergyMeterRender::class.java
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Fuse Holder")
            val desc = ElectricalFuseHolderDescriptor(
                name, Eln.obj.getObj(
                    "ElectricalFuse"
                )
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "5A Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"), 5.0)
            desc.creativeTab = Eln.creativeTabPowerElectronics
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 17
            name = I18N.TR_NAME(I18N.Type.NONE, "10A Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"), 10.0)
            desc.creativeTab = Eln.creativeTabPowerElectronics
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 18
            name = I18N.TR_NAME(I18N.Type.NONE, "15A Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"), 15.0)
            desc.creativeTab = Eln.creativeTabPowerElectronics
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 19
            name = I18N.TR_NAME(I18N.Type.NONE, "20A Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"), 20.0)
            desc.creativeTab = Eln.creativeTabPowerElectronics
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 20
            name = I18N.TR_NAME(I18N.Type.NONE, "30A Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"), 30.0)
            desc.creativeTab = Eln.creativeTabPowerElectronics
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 21
            name = I18N.TR_NAME(I18N.Type.NONE, "50A Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"), 50.0)
            desc.creativeTab = Eln.creativeTabPowerElectronics
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Fuse for low voltage cables")
            val desc = ElectricalFuseDescriptor(
                name, Eln.instance.lowVoltageCableDescriptor, Eln.obj.getObj(
                    "ElectricalFuse"
                )
            )
            desc.hideFromCreative()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Fuse for medium voltage cables")
            val desc = ElectricalFuseDescriptor(
                name, Eln.instance.meduimVoltageCableDescriptor,
                Eln.obj.getObj("ElectricalFuse")
            )
            desc.hideFromCreative()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Fuse for high voltage cables")
            val desc = ElectricalFuseDescriptor(
                name, Eln.instance.highVoltageCableDescriptor,
                Eln.obj.getObj("ElectricalFuse")
            )
            desc.hideFromCreative()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 10
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Fuse for very high voltage cables")
            val desc = ElectricalFuseDescriptor(
                name, Eln.instance.veryHighVoltageCableDescriptor,
                Eln.obj.getObj("ElectricalFuse")
            )
            desc.hideFromCreative()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 11
            name = I18N.TR_NAME(I18N.Type.NONE, "Blown Lead Fuse")
            val desc = ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"))
            BlownFuse = desc
            Eln.sharedItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalSensor(id: Int) {
        var subId: Int
        var name: String?
        var desc: ElectricalSensorDescriptor

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Probe")
            desc = ElectricalSensorDescriptor(name, "electricalsensor", false)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Voltage Probe")
            desc = ElectricalSensorDescriptor(name, "voltagesensor", true)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerThermalSensor(id: Int) {
        var subId: Int
        var name: String?
        var desc: ThermalSensorDescriptor

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Thermal Probe")
            desc = ThermalSensorDescriptor(name, Eln.obj.getObj("thermalsensor"), false)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Temperature Probe")
            desc = ThermalSensorDescriptor(name, Eln.obj.getObj("temperaturesensor"), true)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalVuMeter(id: Int) {
        var subId: Int
        var name: String
        var desc: ElectricalVuMeterDescriptor
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Analog vuMeter")
            desc = ElectricalVuMeterDescriptor(name, "Vumeter", false)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "LED vuMeter")
            desc = ElectricalVuMeterDescriptor(name, "Led", true)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Multicolor LED vuMeter")
            desc = ElectricalVuMeterDescriptor(name, "Led", false)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalAlarm(id: Int) {
        var subId: Int
        var name: String?
        var desc: ElectricalAlarmDescriptor
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Nuclear Alarm")
            desc = ElectricalAlarmDescriptor(name, Eln.obj.getObj("alarmmedium"), 7, "eln:alarma", 11.0, 1f, NominalVoltage.V12)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Standard Alarm")
            desc = ElectricalAlarmDescriptor(
                name, Eln.obj.getObj("alarmmedium"), 7, "eln:smallalarm_critical", 1.2,
                2f, NominalVoltage.V12
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalEnvironmentalSensor(id: Int) {
        var subId: Int
        var name: String
        run {
            var desc: ElectricalLightSensorDescriptor
            run {
                subId = 0
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Daylight Sensor")
                desc = ElectricalLightSensorDescriptor(name, Eln.obj.getObj("daylightsensor"), true)
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
            run {
                subId = 1
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Light Sensor")
                desc = ElectricalLightSensorDescriptor(name, Eln.obj.getObj("lightsensor"), false)
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ElectricalWeatherSensorDescriptor
            run {
                subId = 4
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Weather Sensor")
                desc = ElectricalWeatherSensorDescriptor(name, Eln.obj.getObj("electricalweathersensor"))
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ElectricalHumiditySensorDescriptor
            run {
                subId = 5
                name = I18N.TR_NAME(I18N.Type.NONE, "Humidity Sensor")
                desc = ElectricalHumiditySensorDescriptor(name, Eln.obj.getObj("electricalweathersensor"))
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ThermometerSensorDescriptor
            run {
                subId = 6
                name = I18N.TR_NAME(I18N.Type.NONE, "Thermometer Sensor")
                desc = ThermometerSensorDescriptor(name, Eln.obj.getObj("electricalweathersensor"))
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ElectricalWindSensorDescriptor
            run {
                subId = 8
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Anemometer Sensor")
                desc = ElectricalWindSensorDescriptor(name, Eln.obj.getObj("Anemometer"), 25.0)
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ElectricalEntitySensorDescriptor
            run {
                subId = 12
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Entity Sensor")
                desc = ElectricalEntitySensorDescriptor(name, Eln.obj.getObj("ProximitySensor"), 10.0)
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ElectricalFireDetectorDescriptor
            run {
                subId = 13
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Fire Detector")
                desc = ElectricalFireDetectorDescriptor(name, Eln.obj.getObj("FireDetector"), 15.0, false)
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ElectricalFireDetectorDescriptor
            run {
                subId = 14
                name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Fire Buzzer")
                desc = ElectricalFireDetectorDescriptor(name, Eln.obj.getObj("FireDetector"), 15.0, true)
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
        run {
            var desc: ScannerDescriptor
            run {
                subId = 15
                name = I18N.TR_NAME(I18N.Type.NONE, "Scanner")
                desc = ScannerDescriptor(name, Eln.obj.getObj("scanner"))
                Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
    }

    private fun registerElectricalRedstone(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Redstone-to-Voltage Converter")
            val desc = ElectricalRedstoneInputDescriptor(name, Eln.obj.getObj("redtoele"))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Voltage-to-Redstone Converter")
            val desc = ElectricalRedstoneOutputDescriptor(name, Eln.obj.getObj("eletored"))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalGate(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Timer")
            val desc = ElectricalTimeoutDescriptor(name, Eln.obj.getObj("electricaltimer"))
            desc.setTickSound("eln:timer", 0.01f)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Processor")
            val desc = ElectricalMathDescriptor(name, Eln.obj.getObj("PLC"))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "MQTT Signal Controller")
            val desc = MqttSignalControllerDescriptor(name, Eln.obj.getObj("MqttSignalController"))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerWirelessSignal(id: Int) {
        var subId: Int
        var name: String?

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Wireless Signal Receiver")
            val desc = WirelessSignalRxDescriptor(name, Eln.obj.getObj("wirelesssignalrx"))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Wireless Signal Transmitter")
            val desc =
                WirelessSignalTxDescriptor(name, Eln.obj.getObj("wirelesssignaltx"), Eln.config.getIntOrElse("wireless.transmitter.maxRangeBlocks", 32))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Wireless Signal Repeater")
            val desc =
                WirelessSignalRepeaterDescriptor(name, Eln.obj.getObj("wirelesssignalrepeater"), Eln.config.getIntOrElse("wireless.transmitter.maxRangeBlocks", 32))
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalDataLogger(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Data Logger")
            val desc =
                ElectricalDataLoggerDescriptor(name, true, "DataloggerCRTFloor", 1f, 0.5f, 0f, "§6")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Modern Data Logger")
            val desc =
                ElectricalDataLoggerDescriptor(name, true, "FlatScreenMonitor", 0.0f, 1f, 0.0f, "§a")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "Industrial Data Logger")
            val desc =
                ElectricalDataLoggerDescriptor(name, false, "IndustrialPanel", 0.25f, 0.5f, 1f, "§f")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalRelay(id: Int) {
        var subId: Int
        var name: String?
        var desc: ElectricalRelayDescriptor

        run {
            subId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "5V Control Relay 10A")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("RelaySmall"), NominalVoltage.V120, 10.0, 0.04, NominalVoltage.V5)
            desc.setDefaultIcon("lowvoltagerelay")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "12V Control Relay 20A")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("RelayBig"), 600.0, 20.0, 0.01, NominalVoltage.V12)
            desc.setDefaultIcon("mediumvoltagerelay")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "12V Control Relay 50A")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("relay800"), 600.0, 50.0, 0.005, NominalVoltage.V12)
            desc.setDefaultIcon("highvoltagerelay")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "12V Control Relay 100A")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("relay800"), 600.0, 100.0, 0.0025, NominalVoltage.V12)
            desc.setDefaultIcon("veryhighvoltagerelay")
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Low Voltage Relay")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("RelayBig"), Eln.instance.lowVoltageCableDescriptor)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Medium Voltage Relay")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("RelayBig"), Eln.instance.meduimVoltageCableDescriptor)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "High Voltage Relay")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("relay800"), Eln.instance.highVoltageCableDescriptor)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "Very High Voltage Relay")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("relay800"), Eln.instance.veryHighVoltageCableDescriptor)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Relay")
            desc = ElectricalRelayDescriptor(name, Eln.obj.getObj("RelaySmall"), Eln.instance.signalCableDescriptor)
            desc.hideFromCreative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalGateSource(id: Int) {
        var subId: Int
        var name: String?

        val signalsourcepot = ElectricalGateSourceRenderObj(
            Eln.obj.getObj(
                "signalsourcepot"
            )
        )
        val ledswitch = ElectricalGateSourceRenderObj(Eln.obj.getObj("ledswitch"))

        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Trimmer")
            val desc = ElectricalGateSourceDescriptor(
                name, signalsourcepot, false,
                "trimmer"
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Switch")
            val desc = ElectricalGateSourceDescriptor(
                name, ledswitch, true,
                if (Eln.config.getBooleanOrElse("ui.icons.noSymbols", false)) "signalswitch" else "switch"
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Button")
            val desc = ElectricalGateSourceDescriptor(name, ledswitch, true, "button")
            desc.setWithAutoReset()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "Wireless Button")
            val desc = WirelessSignalSourceDescriptor(
                name, ledswitch, Eln.config.getIntOrElse("wireless.transmitter.maxRangeBlocks", 32),
                true
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Wireless Switch")
            val desc = WirelessSignalSourceDescriptor(
                name, ledswitch, Eln.config.getIntOrElse("wireless.transmitter.maxRangeBlocks", 32),
                false
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerLogicalGates(id: Int) {
        val model = Eln.obj.getObj("LogicGates")
        Eln.sixNodeItem.addDescriptor(
            0 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "NOT Chip"), model, "NOT",
                Not::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            1 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "AND Chip"), model, "AND",
                And::class.java
            )
        )
        Eln.sixNodeItem.addDescriptor(
            2 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "NAND Chip"), model,
                "NAND",
                Nand::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            3 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "OR Chip"), model, "OR",
                Or::class.java
            )
        )
        Eln.sixNodeItem.addDescriptor(
            4 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "NOR Chip"), model, "NOR",
                Nor::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            5 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "XOR Chip"), model, "XOR",
                Xor::class.java
            )
        )
        Eln.sixNodeItem.addDescriptor(
            6 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "XNOR Chip"), model,
                "XNOR",
                XNor::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(7 + (id shl 6), PalDescriptor(I18N.TR_NAME(I18N.Type.NONE, "PAL Chip"), model))

        Eln.sixNodeItem.addDescriptor(
            8 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Schmitt Trigger Chip"),
                model, "SCHMITT", SchmittTrigger::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            9 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "D Flip Flop Chip"),
                model, "DFF", DFlipFlop::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            10 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Oscillator Chip"),
                model, "OSC", Oscillator::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            11 + (id shl 6), LogicGateDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "JK Flip Flop Chip"),
                model, "JKFF", JKFlipFlop::class.java
            )
        )
    }

    private fun registerAnalogChips(id: Int) {
        val baseId = id shl 6
        // TODO: Breaking change! These should be using a subId; the numbers here are not being shifted properly and might overlap.

        val model = Eln.obj.getObj("AnalogChips")
        Eln.sixNodeItem.addDescriptor(
            baseId + 0, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "OpAmp"), model, "OP",
                OpAmp::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 1, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "PID Regulator"), model, "PID",
                PIDRegulator::class.java,
                PIDRegulatorElement::class.java,
                PIDRegulatorRender::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 2, AnalogChipDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE, "Voltage controlled sawtooth " +
                            "oscillator"
                ), model, "VCO-SAW", VoltageControlledSawtoothOscillator::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 3, AnalogChipDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE, "Voltage controlled sine " +
                            "oscillator"
                ), model, "VCO-SIN", VoltageControlledSineOscillator::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 4, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Amplifier"), model, "AMP",
                Amplifier::class.java,
                AmplifierElement::class.java,
                AmplifierRender::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 5, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Voltage controlled amplifier"),
                model, "VCA", VoltageControlledAmplifier::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 6, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Configurable summing unit"),
                model, "SUM", SummingUnit::class.java, SummingUnitElement::class.java, SummingUnitRender::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 7, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Sample and hold"), model, "SAH",
                SampleAndHold::class.java
            )
        )

        Eln.sixNodeItem.addDescriptor(
            baseId + 8, AnalogChipDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Lowpass filter"), model, "LPF",
                Filter::class.java,
                FilterElement::class.java,
                FilterRender::class.java
            )
        )
    }

    private fun registerBatteryCharger(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: BatteryChargerDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "120V Battery Charger")
            descriptor = BatteryChargerDescriptor(
                name, Eln.obj.getObj("batterychargera"), Eln.instance.meduimVoltageCableDescriptor,
                NominalVoltage.V120, 200.0
            )
            Eln.sixNodeItem.addDescriptor(completId, descriptor)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "120V Fast Battery Charger")
            descriptor = BatteryChargerDescriptor(
                name, Eln.obj.getObj("batterychargera"), Eln.instance.meduimVoltageCableDescriptor,
                NominalVoltage.V120, 400.0
            )
            Eln.sixNodeItem.addDescriptor(completId, descriptor)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "240V Battery Charger")
            descriptor = BatteryChargerDescriptor(
                name, Eln.obj.getObj("batterychargera"),
                Eln.instance.highVoltageCableDescriptor, NominalVoltage.V240, 1000.0
            )
            Eln.sixNodeItem.addDescriptor(completId, descriptor)
        }
    }

    private fun registerTreeResinCollector(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: TreeResinCollectorDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tree Resin Collector")
            descriptor = TreeResinCollectorDescriptor(name, Eln.obj.getObj("treeresincolector")).machines()
            Eln.sixNodeItem.addDescriptor(completId, descriptor)
        }
    }

    fun registerPortableNaN() {
        var subId: Int
        var name: String
        val id = 125
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable NaN")
            Eln.stdPortableNaN = CableRenderDescriptor("eln", "sprites/nan.png", 3.95f, 0.95f)
            Eln.portableNaNDescriptor = PortableNaNDescriptor(name, Eln.stdPortableNaN).creative()
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), Eln.portableNaNDescriptor)
        }
    }
}

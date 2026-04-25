package mods.eln.registration

import mods.eln.Eln
import mods.eln.Eln.instance
import mods.eln.Eln.transparentNodeItem
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.gridnode.GridSwitchDescriptor
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor
import mods.eln.gridnode.electricalpole.Kind
import mods.eln.gridnode.transformer.GridTransformerDescriptor
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.TR_NAME
import mods.eln.mechanical.*
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.FunctionTable
import mods.eln.misc.FunctionTableYProtect
import mods.eln.misc.SeriesFunction.Companion.newE12
import mods.eln.misc.SeriesFunction.Companion.newE6
import mods.eln.misc.Utils.coalEnergyReference
import mods.eln.misc.Utils.printFunction
import mods.eln.misc.NominalVoltage
import mods.eln.railroad.OverheadLinesDescriptor
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.ThermalLoadInitializerByPowerDrop
import mods.eln.sound.SoundCommand
import mods.eln.transparentnode.*
import mods.eln.transparentnode.autominer.AutoMinerDescriptor
import mods.eln.transparentnode.battery.BatteryDescriptor
import mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor
import mods.eln.transparentnode.electricalmachine.*
import mods.eln.transparentnode.festive.ChristmasTreeDescriptor
import mods.eln.transparentnode.festive.HolidayCandleDescriptor
import mods.eln.transparentnode.festive.StringLightsDescriptor
import mods.eln.transparentnode.floodlight.FloodlightDescriptor
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor
import mods.eln.transparentnode.teleporter.TeleporterDescriptor
import mods.eln.transparentnode.themralheatexchanger.ThermalHeatExchangerDescriptor
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor
import mods.eln.transparentnode.turbine.TurbineDescriptor
import mods.eln.transparentnode.turret.TurretDescriptor
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import kotlin.math.pow

object TransparentNodeRegistration {
    private const val LARGE_MACHINE_VOLUME_SCALE = 27f
    private val LARGE_MACHINE_MODEL_SCALE = Math.cbrt(LARGE_MACHINE_VOLUME_SCALE.toDouble()).toFloat()

    private fun createLargeMachineGhostGroup(): GhostGroup {
        val g = GhostGroup()
        for (x in -1..1) {
            for (y in 0..2) {
                for (z in -1..1) {
                    if (x == 0 && y == 0 && z == 0) continue
                    if (y == 0 && x != 0) continue
                    g.addElement(x, y, z)
                }
            }
        }
        return g
    }

    private fun <T : SimpleShaftDescriptor> T.applyLargeMachineLayout(): T {
        ghostGroup = createLargeMachineGhostGroup()
        ghostGroup?.removeElement(0, 1, -1)
        ghostGroup?.removeElement(0, 1, 1)
        addShaftGhostPort(Coordinate(0, 1, -1, 0), Direction.ZN, Direction.ZN)
        addShaftGhostPort(Coordinate(0, 1, 1, 0), Direction.ZP, Direction.ZP)
        modelScale = LARGE_MACHINE_MODEL_SCALE
        shaftMass *= LARGE_MACHINE_VOLUME_SCALE.toDouble()
        disableCameraOptimization = true
        return this
    }

    private fun <T : GenericItemBlockUsingDamageDescriptor> T.machines() = apply {
        setCreativeTab(Eln.creativeTabMachines)
    }

    fun registerTransparent() {
        Eln.transparentNodeItem.setCreativeTabForGroup(1, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(2, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(3, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(4, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(7, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(16, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(32, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(33, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(34, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(35, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(36, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(37, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(41, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(42, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(48, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(49, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(64, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(65, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(66, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(67, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(68, Eln.creativeTabLighting)
        Eln.transparentNodeItem.setCreativeTabForGroup(69, Eln.creativeTabLighting)
        Eln.transparentNodeItem.setCreativeTabForGroup(70, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(71, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(72, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(96, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(117, Eln.creativeTabSignalProcessing)
        Eln.transparentNodeItem.setCreativeTabForGroup(123, Eln.creativeTabPowerElectronics)

        registerPowerComponent(1)
        registerTransformer(2)
        registerHeatFurnace(3)
        registerTurbine(4)
        registerElectricalAntenna(7)
        registerBattery(16)
        registerElectricalFurnace(32)
        registerMacerator(33)
        registerArcFurnace(34)
        registerCompressor(35)
        registerMagnetizer(36)
        registerPlateMachine(37)
        registerEggIncubator(41)
        registerAutoMiner(42)
        registerSolarPanel(48)
        registerWindTurbine(49)
        registerThermalDissipatorPassiveAndActive(64)
        registerTransparentNodeMisc(65)
        registerTurret(66)
        registerFuelGenerator(67)
        registerFloodlight(68)
        registerFestive(69)
        registerFab(70)
        registerRailroad(71)
        registerWireProcessingMachines(72)
        registerLargeRheostat() // 96, but from the wrong side.
        registerNixieTube() // 117, but from the wrong side.
        registerGridDevices(123)
    }


    private fun registerPowerComponent(id: Int) {
        var subId: Int
        var name: String?

        run {
            subId = 16
            name = TR_NAME(I18N.Type.NONE, "Power inductor")
            val desc = PowerInductorDescriptor(name, null, newE12(-1.0))
            transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }

        run {
            subId = 20
            name = TR_NAME(I18N.Type.NONE, "Power capacitor")
            val desc = PowerCapacitorDescriptor(name, null, newE6(-2.0), 300.0)
            transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
    }

    private fun registerStreetLamps(id: Int) {
        var subId: Int
        var name: String
        run {
            println("Street Light?")
            println(Eln.obj.objectList)
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "StreetLightWall")
            val desc = StreetLightWallDescriptor(name, Eln.obj.getObj("StreetLightWall"))
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerRailroad(id: Int) {
        var subId: Int
        run {
            subId = 0
            val name = TR_NAME(I18N.Type.NONE, "Overhead Lines")
            val desc = OverheadLinesDescriptor(name, Eln.obj.getObj("OverheadGantry"))
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        /*
        run {
            subId = 1
            val name = TR_NAME(I18N.Type.NONE, "Under Track Power")
            val desc = UnderTrackPowerDescriptor(name, Eln.obj.getObj("OverheadGantry"))
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
         */
    }

    private fun registerWireProcessingMachines(id: Int) {
        run {
            val subId = 0
            val desc = WireMachineDescriptor(
                TR_NAME(I18N.Type.NONE, "Wire Roller"),
                WireMachineKind.ROLLER,
                Eln.obj.getObj("platemachinea")
            )
            desc.setDefaultIcon("50vplatemachine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            val subId = 1
            val desc = WireMachineDescriptor(
                TR_NAME(I18N.Type.NONE, "Wire Insulator"),
                WireMachineKind.INSULATOR,
                Eln.obj.getObj("fabricator")
            )
            desc.setDefaultIcon("machineblock")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            val subId = 2
            val desc = WireMachineDescriptor(
                TR_NAME(I18N.Type.NONE, "Wire Combiner"),
                WireMachineKind.COMBINER,
                Eln.obj.getObj("magnetizera")
            )
            desc.setDefaultIcon("50vmagnetizer")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerFloodlight(id: Int) {
        var subId: Int
        var completeId: Int
        var descriptor: FloodlightDescriptor

        run {
            subId = 0
            completeId = subId + (id shl 6)
            descriptor = FloodlightDescriptor(
                TR_NAME(I18N.Type.NONE, "120V Basic Floodlight"),
                Eln.obj.getObj("Floodlight"),
                false)
            descriptor.setDefaultIcon("basicfloodlight")
            transparentNodeItem.addDescriptor(completeId, descriptor)
        }

        run {
            subId = 1
            completeId = subId + (id shl 6)
            descriptor = FloodlightDescriptor(
                TR_NAME(I18N.Type.NONE, "240V Motorized Floodlight"),
                Eln.obj.getObj("FloodlightMotor"),
                true)
            descriptor.setDefaultIcon("motorizedfloodlight")
            transparentNodeItem.addDescriptor(completeId, descriptor)
        }
    }

    private fun registerLargeRheostat() {
        val id = 96
        run {
            val subId = 39
            val name = I18N.TR_NAME(I18N.Type.NONE, "Large Rheostat")
            val dissipator = ThermalDissipatorPassiveDescriptor(
                name, Eln.obj.getObj(
                    "LargeRheostat"
                ), 1000.0, -100.0, 4000.0, 800.0, 10.0, 1.0
            )
            val desc = LargeRheostatDescriptor(
                name!!, dissipator,
                instance.highVoltageCableDescriptor, newE12(0.0)
            )
            Eln.transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerNixieTube() {
        run {
            val id = 117
            val subId = 7
            val name = I18N.TR_NAME(I18N.Type.NONE, "Nixie Tube")
            val desc = NixieTubeDescriptor(name, Eln.obj.getObj("NixieTube"))
            Eln.transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerTransformer(id: Int) {
        var subId: Int
        var name: String

        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Legacy DC-DC Converter")
            val desc = LegacyDcDcDescriptor(
                name, Eln.obj.getObj("transformator"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), 0.5f
            )
            transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Variable DC-DC Converter")
            val desc = VariableDcDcDescriptor(
                name, Eln.obj.getObj("variabledcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase")
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "DC-DC Converter")
            val desc = DcDcDescriptor(
                name, Eln.obj.getObj("transformator"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), 0.5f
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerHeatFurnace(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Stone Heat Furnace")
            val desc = HeatFurnaceDescriptor(
                name,
                "stonefurnace",
                4000.0,
                coalEnergyReference * 2 / 3,
                8,
                500.0,
                ThermalLoadInitializerByPowerDrop(780.0, -100.0, 10.0, 2.0)
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Fuel Heat Furnace")
            val desc = FuelHeatFurnaceDescriptor(
                name, Eln.obj.getObj("FuelHeater"),
                ThermalLoadInitializerByPowerDrop(780.0, -100.0, 10.0, 2.0)
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerTurbine(id: Int) {
        var subId: Int
        var name: String?
        val heatTurbinePowerFactor = Eln.config.getDoubleOrElse("balance.generators.heatTurbinePowerFactor", 1.0)

        val TtoU = FunctionTable(doubleArrayOf(0.0, 0.1, 0.85, 1.0, 1.1, 1.15, 1.18, 1.19, 1.25), 8.0 / 5.0)
        val PoutToPin = FunctionTable(
            doubleArrayOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7),
            8.0 / 5.0
        )

        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "48V Turbine")
            val RsFactor = 0.1
            val nominalU = NominalVoltage.V48
            val nominalP: Double = 1000 * heatTurbinePowerFactor // it was 300 before
            val nominalDeltaT = 250.0
            val desc =
                TurbineDescriptor(
                    name, "turbineb", instance.lowVoltageCableDescriptor.render,
                    TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                    nominalU, nominalP, nominalP / 40, instance.lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                    nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v"
                )
            desc.setDefaultIcon("50vturbine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = TR_NAME(I18N.Type.NONE, "240V Turbine")
            val RsFactor = 0.10
            val nominalU = NominalVoltage.V240
            val nominalP: Double = 2000 * heatTurbinePowerFactor
            val nominalDeltaT = 350.0
            val desc =
                TurbineDescriptor(
                    name, "turbinebblue", instance.meduimVoltageCableDescriptor.render,
                    TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                    nominalU, nominalP, nominalP / 40, instance.meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                    nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v"
                )
            desc.setDefaultIcon("200vturbine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 9
            val desc = SteamTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Steam Turbine"), Eln.obj.getObj(
                    "Turbine"
                )
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 10
            val nominalRads = 200f
            val nominalU = NominalVoltage.V480.toFloat()
            val nominalP = 16000f
            val desc = GeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "Generator"),
                Eln.obj.getObj(
                    "Generator"
                ),
                instance.highVoltageCableDescriptor,
                nominalRads,
                nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                Eln.sixNodeThermalLoadInitializer.copy()
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 11
            val desc = GasTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Gas Turbine"), Eln.obj.getObj(
                    "GasTurbine"
                )
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 12
            val desc = StraightJointDescriptor(
                TR_NAME(I18N.Type.NONE, "Joint"), Eln.obj.getObj(
                    "StraightJoint"
                )
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 13
            val desc = VerticalHubDescriptor(
                TR_NAME(I18N.Type.NONE, "Joint hub"), Eln.obj.getObj(
                    "VerticalHub"
                )
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 14
            val desc = FlywheelDescriptor(
                TR_NAME(I18N.Type.NONE, "Flywheel"),
                Eln.obj.getObj("Flywheel")
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 15
            val desc = TachometerDescriptor(
                TR_NAME(I18N.Type.NONE, "Tachometer"), Eln.obj.getObj(
                    "Tachometer"
                )
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 16
            val nominalRads = 200f
            val nominalU = NominalVoltage.V480.toFloat()
            val nominalP = 16000f

            val desc = MotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Shaft Motor"), Eln.obj.getObj("Motor"),
                instance.highVoltageCableDescriptor, nominalRads, nominalU, nominalP, 25.0f * nominalP / nominalU,
                25.0f * nominalP / nominalU, Eln.sixNodeThermalLoadInitializer.copy()
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 17
            val desc = ClutchDescriptor(
                TR_NAME(I18N.Type.NONE, "Clutch"),
                Eln.obj.getObj("Clutch")
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 18
            val desc = FixedShaftDescriptor(
                TR_NAME(I18N.Type.NONE, "Fixed Shaft"), Eln.obj.getObj(
                    "FixedShaft"
                )
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 19
            val desc = RadialMotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Radial Motor"), Eln.obj.getObj(
                    "Starter_Motor"
                )
            )
            val g = GhostGroup()
            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1 downTo -3 + 1) {
                        g.addElement(x, y, z)
                    }
                }
            }
            g.removeElement(0, 0, 0)
            desc.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 20
            /*
            Humans generate roughly 75-100 watts of power over time based on Wikipedia, peaking at 1,000 watts
            for short periods of time if they are _really_ in shape (and using legs). Let's say 200 watts is good?
         */
            val desc = CrankableShaftDescriptor(
                TR_NAME(I18N.Type.NONE, "Crank Shaft"),
                Eln.obj.getObj("StraightJoint"), 20.0f, 200.0f
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 21
            val desc = RollingShaftMachineDescriptor(
                TR_NAME(
                    I18N.Type.NONE, "Rolling Shaft " +
                            "Machine"
                ), Eln.obj.getObj("platemachinea")
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 22
            val desc = SteamTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Large Steam Turbine"),
                Eln.obj.getObj("Turbine"),
                LARGE_MACHINE_VOLUME_SCALE
            ).applyLargeMachineLayout()
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 23
            val desc = GasTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Large Gas Turbine"),
                Eln.obj.getObj("GasTurbine"),
                LARGE_MACHINE_VOLUME_SCALE
            ).applyLargeMachineLayout()
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 24
            val nominalRads = 200f
            val nominalU = NominalVoltage.V7200.toFloat()
            val nominalP = 4000f * LARGE_MACHINE_VOLUME_SCALE

            val desc = GeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "Large Generator"),
                Eln.obj.getObj("Generator"),
                instance.veryHighVoltageCableDescriptor,
                nominalRads,
                nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                Eln.sixNodeThermalLoadInitializer.copy()
            ).applyLargeMachineLayout()
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 25
            val nominalRads = 200f
            val nominalU = NominalVoltage.V7200.toFloat()
            val nominalP = 1200f * LARGE_MACHINE_VOLUME_SCALE

            val desc = MotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Large Shaft Motor"),
                Eln.obj.getObj("Motor"),
                instance.veryHighVoltageCableDescriptor,
                nominalRads,
                nominalU,
                nominalP,
                25.0f * nominalP / nominalU,
                25.0f * nominalP / nominalU,
                Eln.sixNodeThermalLoadInitializer.copy()
            ).applyLargeMachineLayout()
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 26
            val nominalRads = 200f
            val nominalU = NominalVoltage.V480.toFloat()
            val nominalP = 16000f
            val desc = GeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "Polarized Shaft Generator"),
                Eln.obj.getObj("PolarizedShaftGenerator"),
                instance.highVoltageCableDescriptor,
                nominalRads,
                nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                Eln.sixNodeThermalLoadInitializer.copy(),
                bipolarTerminals = true
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 27
            val nominalRads = 200f
            val nominalU = NominalVoltage.V480.toFloat()
            val nominalP = 16000f
            val desc = MotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Polarized Shaft Motor"),
                Eln.obj.getObj("PolarizedShaftMotor"),
                instance.highVoltageCableDescriptor,
                nominalRads,
                nominalU,
                nominalP,
                25.0f * nominalP / nominalU,
                25.0f * nominalP / nominalU,
                Eln.sixNodeThermalLoadInitializer.copy(),
                bipolarTerminals = true
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalFurnace(id: Int) {
        var subId: Int
        var name: String?
        instance.furnaceList.add(ItemStack(Blocks.furnace))
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Electrical Furnace")
            val PfTTable =
                doubleArrayOf(0.0, 20.0, 40.0, 80.0, 160.0, 240.0, 360.0, 540.0, 756.0, 1058.4, 1481.76)

            val thermalPlostfTTable = DoubleArray(PfTTable.size)
            for (idx in thermalPlostfTTable.indices) {
                thermalPlostfTTable[idx] =
                    PfTTable[idx] * (((idx + 1.0) / thermalPlostfTTable.size).pow(2.0)) * 2
            }

            val PfT = FunctionTableYProtect(PfTTable, 800.0, 0.0, 100000.0)

            val thermalPlostfT = FunctionTableYProtect(
                thermalPlostfTTable, 800.0, 0.001,
                10000000.0
            )

            val desc = ElectricalFurnaceDescriptor(name, PfT, thermalPlostfT, 40.0)
            instance.electricalFurnace = desc
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            instance.furnaceList.add(desc.newItemStack())
        }
    }

    private fun registerMacerator(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "48V Macerator")
            val desc = MaceratorDescriptor(
                name, "maceratora", NominalVoltage.V48, 200.0, NominalVoltage.V48 * 1.25,
                ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor, instance.maceratorRecipes
            )
            desc.setDefaultIcon("50vmacerator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:macerator")
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "240V Macerator")
            val desc = MaceratorDescriptor(
                name, "maceratorb", NominalVoltage.V240, 2000.0, NominalVoltage.V240 * 1.25,
                ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,  // cable
                instance.maceratorRecipes
            )
            desc.setDefaultIcon("200vmacerator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:macerator")
        }
    }

    private fun registerArcFurnace(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Old 480V Arc Furnace")
            val desc = OldArcFurnaceDescriptor(
                name, Eln.obj.getObj("arcfurnaceold"), NominalVoltage.V480, 10000.0,
                NominalVoltage.V480 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.highVoltageCableDescriptor,
                instance.arcFurnaceRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setDefaultIcon("old800varcfurnace")
            desc.setRunningSound("eln:Arcfurnace_loop")
        }
        /*

        To be released at a later date. Needs a bit of code in the backend, and there's a rendering bug and some other
        minor issues to be resolved.

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "800V Arc Furnace");

            ArcFurnaceDescriptor desc = new ArcFurnaceDescriptor(name, obj.getObj("arcfurnace"));

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            //desc.setRunningSound("eln:arc_furnace");

        }
        */
    }

    private fun registerPlateMachine(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "48V Plate Machine")
            val desc = PlateMachineDescriptor(
                name, Eln.obj.getObj("platemachinea"), NominalVoltage.V48, 200.0,
                NominalVoltage.V48 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor,
                instance.plateMachineRecipes
            )
            desc.setDefaultIcon("50vplatemachine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:plate_machine")
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "240V Plate Machine")
            val desc = PlateMachineDescriptor(
                name, Eln.obj.getObj("platemachineb"), NominalVoltage.V240, 2000.0,
                NominalVoltage.V240 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,
                instance.plateMachineRecipes
            )
            desc.setDefaultIcon("200vplatemachine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:plate_machine")
        }
    }

    private fun registerEggIncubator(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "24V Egg Incubator")
            val desc = EggIncubatorDescriptor(
                name, Eln.obj.getObj("eggincubator"),
                instance.lowVoltageCableDescriptor, NominalVoltage.V24, 50.0
            )
            desc.setDefaultIcon("50veggincubator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerCompressor(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "48V Compressor")
            val desc = CompressorDescriptor(
                name, Eln.obj.getObj("compressora"), NominalVoltage.V48, 200.0,
                NominalVoltage.V48 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor,
                instance.compressorRecipes
            )
            desc.setDefaultIcon("50vcompressor")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:compressor_run")
            desc.setEndSound(SoundCommand("eln:compressor_end"))
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "240V Compressor")
            val desc = CompressorDescriptor(
                name, Eln.obj.getObj("compressorb"), NominalVoltage.V240, 2000.0,
                NominalVoltage.V240 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,
                instance.compressorRecipes
            )
            desc.setDefaultIcon("200vcompressor")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:compressor_run")
            desc.setEndSound(SoundCommand("eln:compressor_end"))
        }
    }

    private fun registerMagnetizer(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "48V Magnetizer")
            val desc = MagnetizerDescriptor(
                name, Eln.obj.getObj("magnetizera"), NominalVoltage.V48, 200.0,
                NominalVoltage.V48 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor,
                instance.magnetiserRecipes
            )
            desc.setDefaultIcon("50vmagnetizer")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:Motor")
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "240V Magnetizer")
            val desc = MagnetizerDescriptor(
                name, Eln.obj.getObj("magnetizerb"), NominalVoltage.V240, 2000.0,
                NominalVoltage.V240 * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,
                instance.magnetiserRecipes
            )
            desc.setDefaultIcon("200vmagnetizer")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:Motor")
        }
    }

    private fun registerSolarPanel(id: Int) {
        var subId: Int
        var ghostGroup: GhostGroup
        var name: String?
        val solarPanelPowerFactor = Eln.config.getDoubleOrElse("balance.generators.solarPanelPowerFactor", 1.0)
        val smallSolarUmax = 36.0
        val smallSolarPmax = 200.0 * solarPanelPowerFactor
        val largeSolarUmax = 108.0
        val largeSolarPmax = smallSolarPmax * 6.0

        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Small Solar Panel")
            ghostGroup = GhostGroup()
            val desc = SolarPanelDescriptor(
                name,
                Eln.obj.getObj("smallsolarpannel"),
                null,
                ghostGroup,
                0,
                1,
                0,
                null,
                smallSolarUmax,
                smallSolarPmax,
                0.01,
                Math.PI / 2,
                Math.PI / 2
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "Small Rotating Solar Panel")
            ghostGroup = GhostGroup()
            val desc = SolarPanelDescriptor(
                name, Eln.obj.getObj("smallsolarpannelrot"),
                instance.lowVoltageCableDescriptor.render, ghostGroup, 0, 1, 0, null, smallSolarUmax,
                smallSolarPmax, 0.01, Math.PI / 4, Math.PI / 4 * 3
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            name = TR_NAME(I18N.Type.NONE, "2x3 Solar Panel")
            val groundCoordinate = Coordinate(1, 0, 0, 0)
            ghostGroup = GhostGroup()
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1)
            ghostGroup.removeElement(0, 0, 0)
            val desc = SolarPanelDescriptor(
                name, Eln.obj.getObj("bigSolarPanel"),
                instance.meduimVoltageCableDescriptor.render, ghostGroup, 1, 1, 0, groundCoordinate, largeSolarUmax,
                largeSolarPmax, 0.01, Math.PI / 2, Math.PI / 2
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "2x3 Rotating Solar Panel")
            val groundCoordinate = Coordinate(1, 0, 0, 0)
            ghostGroup = GhostGroup()
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1)
            ghostGroup.removeElement(0, 0, 0)
            val desc = SolarPanelDescriptor(
                name,
                Eln.obj.getObj("bigSolarPanelrot"),
                instance.meduimVoltageCableDescriptor.render,
                ghostGroup,
                1,
                1,
                1,
                groundCoordinate,
                largeSolarUmax,
                largeSolarPmax,
                0.01,
                Math.PI / 8 * 3,
                Math.PI / 8 * 5
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricalAntenna(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            val desc: ElectricalAntennaTxDescriptor
            name = TR_NAME(I18N.Type.NONE, "Low Power Transmitter Antenna")
            val P = 250.0
            desc = ElectricalAntennaTxDescriptor(
                name, Eln.obj.getObj("lowpowertransmitterantenna"), 200, 0.9, 0.7,
                NominalVoltage.V48, P, NominalVoltage.V48 * 1.3, P * 1.3, instance.lowVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            val desc: ElectricalAntennaRxDescriptor
            name = TR_NAME(I18N.Type.NONE, "Low Power Receiver Antenna")
            val P = 250.0
            desc = ElectricalAntennaRxDescriptor(
                name, Eln.obj.getObj("lowpowerreceiverantenna"), NominalVoltage.V48, P, NominalVoltage.V48 * 1.3,
                P * 1.3, instance.lowVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 2
            val desc: ElectricalAntennaTxDescriptor
            name = TR_NAME(I18N.Type.NONE, "Medium Power Transmitter Antenna")
            val P = 1000.0
            desc = ElectricalAntennaTxDescriptor(
                name, Eln.obj.getObj("lowpowertransmitterantenna"), 250, 0.9, 0.75,
                NominalVoltage.V240, P, NominalVoltage.V240 * 1.3, P * 1.3, instance.meduimVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            val desc: ElectricalAntennaRxDescriptor
            name = TR_NAME(I18N.Type.NONE, "Medium Power Receiver Antenna")
            val P = 1000.0
            desc = ElectricalAntennaRxDescriptor(
                name, Eln.obj.getObj("lowpowerreceiverantenna"), NominalVoltage.V240, P, NominalVoltage.V240 * 1.3,
                P * 1.3, instance.meduimVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 4
            val desc: ElectricalAntennaTxDescriptor
            name = TR_NAME(I18N.Type.NONE, "High Power Transmitter Antenna")
            val P = 2000.0
            desc = ElectricalAntennaTxDescriptor(
                name, Eln.obj.getObj("lowpowertransmitterantenna"), 300, 0.95, 0.8,
                NominalVoltage.V480, P, NominalVoltage.V480 * 1.3, P * 1.3, instance.highVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 5
            val desc: ElectricalAntennaRxDescriptor
            name = TR_NAME(I18N.Type.NONE, "High Power Receiver Antenna")
            val P = 2000.0
            desc = ElectricalAntennaRxDescriptor(
                name, Eln.obj.getObj("lowpowerreceiverantenna"), NominalVoltage.V480, P, NominalVoltage.V480 * 1.3,
                P * 1.3, instance.highVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerBattery(id: Int) {
        var subId: Int
        var name: String
        val batteryCapacityFactor = Eln.config.getDoubleOrElse("balance.storage.batteryCapacityFactor", 1.0)
        val stdBatteryHalfLife = Eln.config.getDoubleOrElse("runtime.items.batteries.standardHalfLifeTicks", 2.0)
        val heatTIme = 30.0
        val voltageFunctionTable = doubleArrayOf(0.000, 0.9, 1.0, 1.025, 1.04, 1.05, 2.0)
        val voltageFunction = FunctionTable(voltageFunctionTable, 6.0 / 5)

        printFunction(voltageFunction, -0.2, 1.2, 0.1)

        val stdDischargeTime = (60 * 8).toDouble()
        val stdU = NominalVoltage.V12
        val stdP: Double = instance.LVP() / 4
        val stdEfficiency = 1.0 - 2.0 / 50.0

        instance.batteryVoltageFunctionTable = voltageFunction
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Cost Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.0, stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife,
                heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("lowcost")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Capacity Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction,
                stdU, stdP / 2 * 1.2, 0.000, stdP / 2, stdDischargeTime * 8 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("capacity")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "Voltage Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction,
                NominalVoltage.V48, stdP * 1.2, 0.000, stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("highvoltage")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 3
            name = TR_NAME(I18N.Type.NONE, "Current Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction, NominalVoltage.V24,
                stdP * 1.2 * 4, 0.000, stdP * 4, stdDischargeTime / 6 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("current")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "Life Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, false, voltageFunction,
                stdU, stdP * 1.2, 0.000, stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("life")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 5
            name = TR_NAME(I18N.Type.NONE, "Single-use Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 1.0, false, false, voltageFunction,
                stdU, stdP * 1.2 * 2, 0.000, stdP * 2, stdDischargeTime / 4 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("coal")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = TR_NAME(I18N.Type.NONE, "Experimental Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, false, voltageFunction,
                NominalVoltage.V120, stdP * 1.2 * 8, 0.025, stdP * 8, stdDischargeTime / 4 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("highvoltage")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerAutoMiner(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Auto Miner")
            val powerLoad = arrayOfNulls<Coordinate>(2)
            powerLoad[0] = Coordinate(-2, -1, 1, 0)
            powerLoad[1] = Coordinate(-2, -1, -1, 0)
            val lightCoord = Coordinate(-3, 0, 0, 0)
            val miningCoord = Coordinate(-1, 0, 1, 0)
            val desc = AutoMinerDescriptor(
                name, Eln.obj.getObj("AutoMiner"), powerLoad, lightCoord,
                miningCoord, 2, 1, 0, instance.highVoltageCableDescriptor, 1.0, 50.0
            )
            val ghostGroup = GhostGroup()
            ghostGroup.addRectangle(-2, -1, -1, 0, -1, 1)
            ghostGroup.addRectangle(1, 1, -1, 0, 1, 1)
            ghostGroup.addRectangle(1, 1, -1, 0, -1, -1)
            ghostGroup.addElement(1, 0, 0)
            ghostGroup.addElement(0, 0, 1)
            ghostGroup.addElement(0, 1, 0)
            ghostGroup.addElement(0, 0, -1)
            ghostGroup.removeElement(-1, -1, 0)
            desc.ghostGroup = ghostGroup
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerWindTurbine(id: Int) {
        var subId: Int
        var name: String?
        val windTurbinePowerFactor = Eln.config.getDoubleOrElse("balance.generators.windTurbinePowerFactor", 1.0)

        val PfW = FunctionTable(doubleArrayOf(0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2), 8.0 / 5.0)
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Wind Turbine")

            val desc = WindTurbineDescriptor(
                name,
                Eln.obj.getObj("WindTurbineMini"),
                instance.lowVoltageCableDescriptor,
                PfW,
                160 * windTurbinePowerFactor,
                10.0,
                NominalVoltage.V24 * 1.18,
                22.0,
                3,
                7,
                2,
                2,
                2,
                0.07,
                "eln:WINDTURBINE_BIG_SF",
                1f
            )

            val g = GhostGroup()
            g.addElement(0, 1, 0)
            g.addElement(0, 2, -1)
            g.addElement(0, 2, 1)
            g.addElement(0, 3, -1)
            g.addElement(0, 3, 1)
            g.addRectangle(0, 0, 1, 3, 0, 0)
            desc.setGhostGroup(g)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        /*{ //TODO Work on the large wind turbine
        subId = 1;
        name = TR_NAME(Type.NONE, "Large Wind Turbine");

        WindTurbineDescriptor desc = new WindTurbineDescriptor(
            name, obj.getObj("WindTurbineMini"), // name,Obj3D obj,
            lowVoltageCableDescriptor,// ElectricalCableDescriptor
            // cable,
            PfW,// PfW
            160 * windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
            LVU * 1.18, 22,// double maxVoltage, double maxWind,
            3,// int offY,
            7, 2, 2,// int rayX,int rayY,int rayZ,
            2, 0.07,// int blockMalusMinCount,double blockMalus
            "eln:WINDTURBINE_BIG_SF", 1f // Use the wind turbine sound and play at normal volume (1 => 100%)
        );

        GhostGroup g = new GhostGroup();
        g.addElement(0, 1, 0);
        g.addElement(0, 2, -1);
        g.addElement(0, 2, 1);
        g.addElement(0, 3, -1);
        g.addElement(0, 3, 1);
        g.addRectangle(0, 0, 1, 3, 0, 0);
        desc.setGhostGroup(g);
        transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    } */
        run {
            subId = 16
            name = TR_NAME(I18N.Type.NONE, "Water Turbine")
            val waterCoord = Coordinate(1, -1, 0, 0)
            val desc = WaterTurbineDescriptor(
                name,
                Eln.obj.getObj("SmallWaterWheel"),
                instance.lowVoltageCableDescriptor,
                30 * Eln.config.getDoubleOrElse("balance.generators.waterTurbinePowerFactor", 1.0),
                NominalVoltage.V24 * 1.18,
                waterCoord,
                "eln:water_turbine",
                1f
            )

            val g = GhostGroup()
            g.addRectangle(1, 1, 0, 1, -1, 1)
            desc.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerFuelGenerator(id: Int) {
        var subId: Int
        val fuelGeneratorPowerFactor = Eln.config.getDoubleOrElse("balance.generators.fuelGeneratorPowerFactor", 1.0)
        val fuelGeneratorTankCapacity = Eln.config.getDoubleOrElse("machines.fuelGenerator.tankCapacitySecondsAtNominalPower", 20.0 * 60.0)
        run {
            subId = 1
            val descriptor = FuelGeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "48V Fuel Generator"),
                Eln.obj.getObj("FuelGenerator50V"),
                instance.lowVoltageCableDescriptor,
                fuelGeneratorPowerFactor * 1200,
                NominalVoltage.V48 * 1.25,
                fuelGeneratorTankCapacity
            )
            descriptor.setDefaultIcon("50vfuelgenerator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 2
            val descriptor = FuelGeneratorDescriptor(
                TR_NAME(
                    I18N.Type.NONE,
                    "240V Fuel Generator"
                ), Eln.obj.getObj("FuelGenerator200V"), instance.meduimVoltageCableDescriptor,
                fuelGeneratorPowerFactor * 6000, NominalVoltage.V240 * 1.25, fuelGeneratorTankCapacity
            )
            descriptor.setDefaultIcon("200vfuelgenerator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
    }

    private fun registerThermalDissipatorPassiveAndActive(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Small Passive Thermal Dissipator")
            val desc = ThermalDissipatorPassiveDescriptor(
                name, Eln.obj.getObj(
                    "passivethermaldissipatora"
                ), 200.0, -100.0, 250.0, 30.0, 10.0, 1.0
            )
            desc.setDefaultIcon("smallpassivethermaldissipator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 32
            name = TR_NAME(I18N.Type.NONE, "24V Small Active Thermal Dissipator")
            val desc = ThermalDissipatorActiveDescriptor(
                name, Eln.obj.getObj(
                    "activethermaldissipatora"
                ), NominalVoltage.V24, 50.0, 800.0, instance.lowVoltageCableDescriptor, 130.0, -100.0, 200.0, 30.0, 10.0, 1.0
            )
            desc.setDefaultIcon("smallactivethermaldissipator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 34
            name = TR_NAME(I18N.Type.NONE, "240V Active Thermal Dissipator")
            val desc = ThermalDissipatorActiveDescriptor(
                name, Eln.obj.getObj(
                    "200vactivethermaldissipatora"
                ), NominalVoltage.V240, 60.0, 1200.0, instance.meduimVoltageCableDescriptor, 130.0, -100.0, 200.0, 30.0,
                10.0, 1.0
            )
            desc.setDefaultIcon("200vactivethermaldissipator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerTransparentNodeMisc(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Experimental Transporter")

            val powerLoad = arrayOfNulls<Coordinate>(2)
            powerLoad[0] = Coordinate(-1, 0, 1, 0)
            powerLoad[1] = Coordinate(-1, 0, -1, 0)

            val doorOpen = GhostGroup()
            doorOpen.addRectangle(-4, -3, 2, 2, 0, 0)

            val doorClose = GhostGroup()
            doorClose.addRectangle(-2, -2, 0, 1, 0, 0)

            val desc = TeleporterDescriptor(
                name, Eln.obj.getObj("Transporter"),
                instance.highVoltageCableDescriptor, Coordinate(-1, 0, 0, 0), Coordinate(-1, 1, 0, 0), 2,  // int areaH
                powerLoad, doorOpen, doorClose

            )
            desc.setChargeSound("eln:transporter", 0.5f)
            val g = GhostGroup()
            g.addRectangle(-2, 0, 0, 1, -1, -1)
            g.addRectangle(-2, 0, 0, 1, 1, 1)
            g.addRectangle(-4, -1, 2, 2, 0, 0)
            g.addElement(0, 1, 0)
            g.addElement(-1, 0, 0, Eln.ghostBlock, GhostBlock.tFloor)
            g.addRectangle(-3, -3, 0, 1, -1, -1)
            g.addRectangle(-3, -3, 0, 1, 1, 1)
            desc.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "Thermal Heat Exchanger")
            val desc = ThermalHeatExchangerDescriptor(
                name,
                ThermalLoadInitializerByPowerDrop(780.0, -100.0, 10.0, 2.0)
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerTurret(id: Int) {
        run {
            val subId = 0
            val name = TR_NAME(I18N.Type.NONE, "480V Defence Turret")
            val desc = TurretDescriptor(name, "Turret")
            desc.setDefaultIcon("800vdefenceturret")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerFestive(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Christmas Tree")
            val desc = ChristmasTreeDescriptor(name, Eln.obj.getObj("Christmas_Tree"))
            if (Eln.config.getBooleanOrElse("gameplay.seasonal.enableFestiveItems", true)) {
                transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            } else {
                transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
            }
        }
        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Holiday Candle")
            val desc = HolidayCandleDescriptor(name, Eln.obj.getObj("Candle_Light"))
            if (Eln.config.getBooleanOrElse("gameplay.seasonal.enableFestiveItems", true)) {
                transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            } else {
                transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
            }
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "String Lights")
            val desc = StringLightsDescriptor(name, Eln.obj.getObj("Christmas_Lights"))
            if (Eln.config.getBooleanOrElse("gameplay.seasonal.enableFestiveItems", true)) {
                transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            } else {
                transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
            }
        }
    }

    private fun registerFab(id: Int) {
        var subId: Int
        run {
            subId = 0
            val desc =
                FabricatorDescriptor(TR_NAME(I18N.Type.NONE, "Fabricator")).machines()
            desc.setDefaultIcon("machineblock")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerGridDevices(id: Int) {
        var subId: Int
        run {
            subId = 3
            val descriptor = GridTransformerDescriptor(
                "Grid DC-DC Converter", Eln.obj.getObj(
                    "GridConverter"
                ), "textures/wire.png", instance.veryHighVoltageCableDescriptor
            )
            val g = GhostGroup()
            g.addElement(1, 0, 0)
            g.addElement(0, 0, -1)
            g.addElement(1, 0, -1)
            g.addElement(1, 1, 0)
            g.addElement(0, 1, 0)
            g.addElement(1, 1, -1)
            g.addElement(0, 1, -1)
            descriptor.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 4
            val descriptor = ElectricalPoleDescriptor(
                "Utility Pole",
                Eln.obj.getObj(
                    "UtilityPole"
                ),
                "textures/wire.png",
                instance.veryHighVoltageCableDescriptor,
                Kind.OVERHEAD,
                40,
                51200.0
            )
            descriptor.renderOffset = Vec3.createVectorHelper(0.0, -0.1, 0.0)
            val g = GhostGroup()
            g.addElement(0, 1, 0)
            g.addElement(0, 2, 0)
            g.addElement(0, 3, 0)
            descriptor.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 5
            val descriptor = ElectricalPoleDescriptor(
                "Utility Pole w/DC-DC Converter",
                Eln.obj.getObj("UtilityPole"),
                "textures/wire.png",
                instance.veryHighVoltageCableDescriptor,
                Kind.TRANSFORMER_TO_GROUND,
                40,
                51200.0
            )
            val g = GhostGroup()
            g.addElement(0, 1, 0)
            g.addElement(0, 2, 0)
            g.addElement(0, 3, 0)
            descriptor.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 6
            val descriptor = ElectricalPoleDescriptor(
                "Transmission Tower",
                Eln.obj.getObj(
                    "TransmissionTower"
                ),
                "textures/wire.png",
                instance.veryHighVoltageCableDescriptor,
                Kind.OVERHEAD,
                96,
                51200.0
            )
            val g = GhostGroup()
            g.addRectangle(-1, 1, 0, 0, -1, 1)
            g.addRectangle(0, 0, 1, 8, 0, 0)
            g.removeElement(0, 0, 0)
            descriptor.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 7
            val descriptor = ElectricalPoleDescriptor(
                "Direct Utility Pole",
                Eln.obj.getObj(
                    "UtilityPole"
                ),
                "textures/wire.png",
                instance.veryHighVoltageCableDescriptor,
                Kind.SHUNT_TO_GROUND,
                40,
                51200.0
            )
            val g = GhostGroup()
            g.addElement(0, 1, 0)
            g.addElement(0, 2, 0)
            g.addElement(0, 3, 0)
            descriptor.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 8
            val name = TR_NAME(I18N.Type.NONE, "Grid Switch")
            val desc = GridSwitchDescriptor(name)
            val g = GhostGroup()
            g.addRectangle(-1, 1, 0, 4, -2, 2)
            g.removeRectangle(-1, -1, 2, 4, -1, 1)
            g.removeRectangle(1, 1, 2, 4, -1, 1)
            g.removeRectangle(0, 0, 1, 4, -2, 2)
            g.removeElement(0, 0, 0)
            desc.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }


}

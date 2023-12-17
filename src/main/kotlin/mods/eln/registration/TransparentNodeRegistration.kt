package mods.eln.registration

import mods.eln.Eln
import mods.eln.Eln.instance
import mods.eln.Eln.transparentNodeItem
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
import mods.eln.misc.FunctionTable
import mods.eln.misc.FunctionTableYProtect
import mods.eln.misc.SeriesFunction.Companion.newE12
import mods.eln.misc.SeriesFunction.Companion.newE6
import mods.eln.misc.Utils.coalEnergyReference
import mods.eln.misc.Utils.printFunction
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

    fun registerTransparent() {
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
        //registerFloodlight(68);
        registerFestive(69)
        registerFab(70)
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

    /*
        private void registerFloodlight(int id) {
            int subId;
            String name;
            {
                subId = 0;
                name = TR_NAME(Type.NONE, "Basic Floodlight");
                BasicFloodlightDescriptor desc = new BasicFloodlightDescriptor(name, obj.getObj("Floodlight"));
                transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            }
            {
                subId = 1;
                name = TR_NAME(Type.NONE, "Motorized Floodlight");
                MotorizedFloodlightDescriptor desc = new MotorizedFloodlightDescriptor(name, obj.getObj
                ("FloodlightMotor"));
                transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
    */


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
                instance.veryHighVoltageCableDescriptor, newE12(0.0)
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
        var name = ""

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
        var name = ""
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

        val TtoU = FunctionTable(doubleArrayOf(0.0, 0.1, 0.85, 1.0, 1.1, 1.15, 1.18, 1.19, 1.25), 8.0 / 5.0)
        val PoutToPin = FunctionTable(
            doubleArrayOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7),
            8.0 / 5.0
        )

        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "50V Turbine")
            val RsFactor = 0.1
            val nominalU = Eln.LVU
            val nominalP: Double = 1000 * instance.heatTurbinePowerFactor // it was 300 before
            val nominalDeltaT = 250.0
            val desc =
                TurbineDescriptor(
                    name, "turbineb", instance.lowVoltageCableDescriptor.render,
                    TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                    nominalU, nominalP, nominalP / 40, instance.lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                    nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v"
                )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = TR_NAME(I18N.Type.NONE, "200V Turbine")
            val RsFactor = 0.10
            val nominalU = Eln.MVU
            val nominalP: Double = 2000 * instance.heatTurbinePowerFactor
            val nominalDeltaT = 350.0
            val desc =
                TurbineDescriptor(
                    name, "turbinebblue", instance.meduimVoltageCableDescriptor.render,
                    TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                    nominalU, nominalP, nominalP / 40, instance.meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                    nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v"
                )
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
            val nominalU = 3200f
            val nominalP = 4000f
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
            val nominalU = 3200f
            val nominalP = 1200f

            val desc = MotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Shaft Motor"), Eln.obj.getObj("Motor"),
                instance.veryHighVoltageCableDescriptor, nominalRads, nominalU, nominalP, 25.0f * nominalP / nominalU,
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
            name = TR_NAME(I18N.Type.NONE, "50V Macerator")
            val desc = MaceratorDescriptor(
                name, "maceratora", Eln.LVU, 200.0, Eln.LVU * 1.25,
                ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor, instance.maceratorRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:macerator")
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "200V Macerator")
            val desc = MaceratorDescriptor(
                name, "maceratorb", Eln.MVU, 2000.0, Eln.MVU * 1.25,
                ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,  // cable
                instance.maceratorRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:macerator")
        }
    }

    private fun registerArcFurnace(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Old 800V Arc Furnace")
            val desc = OldArcFurnaceDescriptor(
                name, Eln.obj.getObj("arcfurnaceold"), Eln.HVU, 10000.0,
                Eln.HVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.highVoltageCableDescriptor,
                instance.arcFurnaceRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
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
            name = TR_NAME(I18N.Type.NONE, "50V Plate Machine")
            val desc = PlateMachineDescriptor(
                name, Eln.obj.getObj("platemachinea"), Eln.LVU, 200.0,
                Eln.LVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor,
                instance.plateMachineRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:plate_machine")
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "200V Plate Machine")
            val desc = PlateMachineDescriptor(
                name, Eln.obj.getObj("platemachineb"), Eln.MVU, 2000.0,
                Eln.MVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,
                instance.plateMachineRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:plate_machine")
        }
    }

    private fun registerEggIncubator(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "50V Egg Incubator")
            val desc = EggIncubatorDescriptor(
                name, Eln.obj.getObj("eggincubator"),
                instance.lowVoltageCableDescriptor, Eln.LVU, 50.0
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerCompressor(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "50V Compressor")
            val desc = CompressorDescriptor(
                name, Eln.obj.getObj("compressora"), Eln.LVU, 200.0,
                Eln.LVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor,
                instance.compressorRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:compressor_run")
            desc.setEndSound(SoundCommand("eln:compressor_end"))
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "200V Compressor")
            val desc = CompressorDescriptor(
                name, Eln.obj.getObj("compressorb"), Eln.MVU, 2000.0,
                Eln.MVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,
                instance.compressorRecipes
            )
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
            name = TR_NAME(I18N.Type.NONE, "50V Magnetizer")
            val desc = MagnetizerDescriptor(
                name, Eln.obj.getObj("magnetizera"), Eln.LVU, 200.0,
                Eln.LVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.lowVoltageCableDescriptor,
                instance.magnetiserRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:Motor")
        }

        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "200V Magnetizer")
            val desc = MagnetizerDescriptor(
                name, Eln.obj.getObj("magnetizerb"), Eln.MVU, 2000.0,
                Eln.MVU * 1.25, ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0), instance.meduimVoltageCableDescriptor,
                instance.magnetiserRecipes
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc.setRunningSound("eln:Motor")
        }
    }

    private fun registerSolarPanel(id: Int) {
        var subId: Int
        var ghostGroup: GhostGroup
        var name: String?
        val diodeIfUBase: FunctionTable =
            FunctionTableYProtect(
                doubleArrayOf(
                    0.0, 0.002, 0.005, 0.01, 0.015, 0.02, 0.025, 0.03,
                    0.035, 0.04, 0.045, 0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 1.0
                ), 1.0, 0.0, 1.0
            )
        val solarIfSBase =
            FunctionTable(doubleArrayOf(0.0, 0.1, 0.4, 0.6, 0.8, 1.0), 1.0)

        val LVSolarU = 59.0

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
                LVSolarU / 4,
                65.0 * instance.solarPanelPowerFactor,
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
                instance.lowVoltageCableDescriptor.render, ghostGroup, 0, 1, 0, null, LVSolarU / 4,
                Eln.solarPanelBasePower * instance.solarPanelPowerFactor, 0.01, Math.PI / 4, Math.PI / 4 * 3
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
                instance.meduimVoltageCableDescriptor.render, ghostGroup, 1, 1, 0, groundCoordinate, LVSolarU * 2,
                Eln.solarPanelBasePower * instance.solarPanelPowerFactor * 8, 0.01, Math.PI / 2, Math.PI / 2
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
                LVSolarU * 2,
                Eln.solarPanelBasePower * instance.solarPanelPowerFactor * 8,
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
                Eln.LVU, P, Eln.LVU * 1.3, P * 1.3, instance.lowVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            val desc: ElectricalAntennaRxDescriptor
            name = TR_NAME(I18N.Type.NONE, "Low Power Receiver Antenna")
            val P = 250.0
            desc = ElectricalAntennaRxDescriptor(
                name, Eln.obj.getObj("lowpowerreceiverantenna"), Eln.LVU, P, Eln.LVU * 1.3,
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
                Eln.MVU, P, Eln.MVU * 1.3, P * 1.3, instance.meduimVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            val desc: ElectricalAntennaRxDescriptor
            name = TR_NAME(I18N.Type.NONE, "Medium Power Receiver Antenna")
            val P = 1000.0
            desc = ElectricalAntennaRxDescriptor(
                name, Eln.obj.getObj("lowpowerreceiverantenna"), Eln.MVU, P, Eln.MVU * 1.3,
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
                Eln.HVU, P, Eln.HVU * 1.3, P * 1.3, instance.highVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 5
            val desc: ElectricalAntennaRxDescriptor
            name = TR_NAME(I18N.Type.NONE, "High Power Receiver Antenna")
            val P = 2000.0
            desc = ElectricalAntennaRxDescriptor(
                name, Eln.obj.getObj("lowpowerreceiverantenna"), Eln.HVU, P, Eln.HVU * 1.3,
                P * 1.3, instance.highVoltageCableDescriptor
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerBattery(id: Int) {
        var subId: Int
        var name = ""
        val heatTIme = 30.0
        val voltageFunctionTable = doubleArrayOf(0.000, 0.9, 1.0, 1.025, 1.04, 1.05, 2.0)
        val voltageFunction = FunctionTable(voltageFunctionTable, 6.0 / 5)

        printFunction(voltageFunction, -0.2, 1.2, 0.1)

        val stdDischargeTime = (60 * 8).toDouble()
        val stdU = Eln.LVU
        val stdP: Double = instance.LVP() / 4
        val stdEfficiency = 1.0 - 2.0 / 50.0

        instance.batteryVoltageFunctionTable = voltageFunction
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Cost Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.0, stdP, stdDischargeTime * instance.batteryCapacityFactor, stdEfficiency, instance.stdBatteryHalfLife,
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
                stdU / 4, stdP / 2 * 1.2, 0.000, stdP / 2, stdDischargeTime * 8 * instance.batteryCapacityFactor, stdEfficiency,
                instance.stdBatteryHalfLife, heatTIme, 60.0, -100.0
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
                stdU * 4, stdP * 1.2, 0.000, stdP, stdDischargeTime * instance.batteryCapacityFactor, stdEfficiency,
                instance.stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("highvoltage")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 3
            name = TR_NAME(I18N.Type.NONE, "Current Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2 * 4, 0.000, stdP * 4, stdDischargeTime / 6 * instance.batteryCapacityFactor, stdEfficiency,
                instance.stdBatteryHalfLife, heatTIme, 60.0, -100.0
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
                stdU, stdP * 1.2, 0.000, stdP, stdDischargeTime * instance.batteryCapacityFactor, stdEfficiency,
                instance.stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
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
                stdU, stdP * 1.2 * 2, 0.000, stdP * 2, stdDischargeTime / 4 * instance.batteryCapacityFactor, stdEfficiency,
                instance.stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("coal")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = TR_NAME(I18N.Type.NONE, "Experimental Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, false, voltageFunction,
                stdU * 2, stdP * 1.2 * 8, 0.025, stdP * 8, stdDischargeTime / 4 * instance.batteryCapacityFactor, stdEfficiency,
                instance.stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
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

        val PfW = FunctionTable(doubleArrayOf(0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2), 8.0 / 5.0)
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Wind Turbine")

            val desc = WindTurbineDescriptor(
                name,
                Eln.obj.getObj("WindTurbineMini"),
                instance.lowVoltageCableDescriptor,
                PfW,
                160 * instance.windTurbinePowerFactor,
                10.0,
                Eln.LVU * 1.18,
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
                30 * instance.waterTurbinePowerFactor,
                Eln.LVU * 1.18,
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
        run {
            subId = 1
            val descriptor = FuelGeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "50V Fuel Generator"),
                Eln.obj.getObj("FuelGenerator50V"),
                instance.lowVoltageCableDescriptor,
                instance.fuelGeneratorPowerFactor * 1200,
                Eln.LVU * 1.25,
                instance.fuelGeneratorTankCapacity
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), descriptor)
        }
        run {
            subId = 2
            val descriptor = FuelGeneratorDescriptor(
                TR_NAME(
                    I18N.Type.NONE,
                    "200V Fuel Generator"
                ), Eln.obj.getObj("FuelGenerator200V"), instance.meduimVoltageCableDescriptor,
                instance.fuelGeneratorPowerFactor * 6000, Eln.MVU * 1.25, instance.fuelGeneratorTankCapacity
            )
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
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 32
            name = TR_NAME(I18N.Type.NONE, "Small Active Thermal Dissipator")
            val desc = ThermalDissipatorActiveDescriptor(
                name, Eln.obj.getObj(
                    "activethermaldissipatora"
                ), Eln.LVU, 50.0, 800.0, instance.lowVoltageCableDescriptor, 130.0, -100.0, 200.0, 30.0, 10.0, 1.0
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 34
            name = TR_NAME(I18N.Type.NONE, "200V Active Thermal Dissipator")
            val desc = ThermalDissipatorActiveDescriptor(
                name, Eln.obj.getObj(
                    "200vactivethermaldissipatora"
                ), Eln.MVU, 60.0, 1200.0, instance.meduimVoltageCableDescriptor, 130.0, -100.0, 200.0, 30.0,
                10.0, 1.0
            )
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
            val name = TR_NAME(I18N.Type.NONE, "800V Defence Turret")
            val desc = TurretDescriptor(name, "Turret")
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
            if (Eln.enableFestivities) {
                transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            } else {
                transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
            }
        }
        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Holiday Candle")
            val desc = HolidayCandleDescriptor(name, Eln.obj.getObj("Candle_Light"))
            if (Eln.enableFestivities) {
                transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            } else {
                transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
            }
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "String Lights")
            val desc = StringLightsDescriptor(name, Eln.obj.getObj("Christmas_Lights"))
            if (Eln.enableFestivities) {
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
                FabricatorDescriptor(TR_NAME(I18N.Type.NONE, "Fabricator"))
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
                ), "textures/wire.png", instance.highVoltageCableDescriptor
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
                instance.highVoltageCableDescriptor,
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
                instance.highVoltageCableDescriptor,
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
                instance.highVoltageCableDescriptor,
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
                instance.highVoltageCableDescriptor,
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
package mods.eln.registry;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.i18n.I18N;
import mods.eln.item.ElectricalFuseDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.materials.MaterialType;
import mods.eln.misc.series.SerieEE;
import mods.eln.sixnode.*;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.batterycharger.BatteryChargerDescriptor;
import mods.eln.sixnode.currentcable.CurrentCableDescriptor;
import mods.eln.sixnode.diode.DiodeDescriptor;
import mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor;
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement;
import mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor;
import mods.eln.sixnode.electricaldigitaldisplay.ElectricalDigitalDisplayDescriptor;
import mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor;
import mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor;
import mods.eln.sixnode.electricalmath.ElectricalMathDescriptor;
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor;
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor;
import mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor;
import mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor;
import mods.eln.sixnode.electricalswitch.ElectricalSwitchDescriptor;
import mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor;
import mods.eln.sixnode.electricalvumeter.ElectricalVuMeterDescriptor;
import mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor;
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor;
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor;
import mods.eln.sixnode.energymeter.EnergyMeterDescriptor;
import mods.eln.sixnode.groundcable.GroundCableDescriptor;
import mods.eln.sixnode.hub.HubDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketStandardObjRender;
import mods.eln.sixnode.lampsocket.LampSocketSuspendedObjRender;
import mods.eln.sixnode.lampsocket.LampSocketType;
import mods.eln.sixnode.lampsupply.LampSupplyDescriptor;
import mods.eln.sixnode.logicgate.*;
import mods.eln.sixnode.modbusrtu.ModbusRtuDescriptor;
import mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor;
import mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor;
import mods.eln.sixnode.powersocket.PowerSocketDescriptor;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.sixnode.signalinductor.SignalInductorDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor;
import mods.eln.sixnode.tutorialsign.TutorialSignDescriptor;
import mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterDescriptor;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor;
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor;
import mods.eln.transparentnode.LargeRheostatDescriptor;
import mods.eln.transparentnode.NixieTubeDescriptor;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;

import static mods.eln.i18n.I18N.TR_NAME;

/**
 * SixNodeRegistry is for any components that can be placed on any of the 6 faces of a block.
 *
 * Components such as resistors, wires, capacitors, and diodes can be found here.
 */
public class SixNodeRegistry {
    public static void thingRegistration() {
        //SIX NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerGround(2);
        registerElectricalSource(3);
        registerElectricalCable(32);
        registerThermalCable(48);
        registerLampSocket(64);
        registerLampSupply(65);
        registerBatteryCharger(66);
        registerPowerSocket(67);
        registerWirelessSignal(92);
        registerElectricalDataLogger(93);
        registerElectricalRelay(94);
        registerElectricalGateSource(95);
        registerPassiveComponent(96);
        registerSwitch(97);
        registerElectricalManager(98);
        registerElectricalSensor(100);
        registerThermalSensor(101);
        registerElectricalVuMeter(102);
        registerElectricalAlarm(103);
        registerElectricalEnvironmentalSensor(104);
        registerElectricalRedstone(108);
        registerElectricalGate(109);
        registerTreeResinCollector(116);
        registerSixNodeMisc(117);
        registerLogicalGates(118);
        registerAnalogChips(124);
    }

    private static void registerElectricalCable(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Signal Cable");

            Eln.stdCableRenderSignal = new CableRenderDescriptor("eln",
                "sprites/cable.png", 0.95f, 0.95f);
            Eln.signalCableDescriptor = new ElectricalCableDescriptor(name, Eln.stdCableRenderSignal,
                "For signal transmission.", true);
            Eln.signalCableDescriptor.setPhysicalConstantLikeNormalCable(Eln.SVU, Eln.SVP, 0.02 / 50
                    * Eln.gateOutputCurrent / Eln.SVII,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Eln.SVU * 1.3, Eln.SVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                0.5,// electricalOverVoltageStartPowerLost,
                Eln.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Eln.cableHeatingTime, 1// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.signalCableDescriptor);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Low Voltage Cable");

            Eln.stdCableRender50V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 1.95f, 0.95f);
            Eln.lowVoltageCableDescriptor = new ElectricalCableDescriptor(name, Eln.stdCableRender50V,
                "For low voltage with high current.", false);
            Eln.lowVoltageCableDescriptor.setPhysicalConstantLikeNormalCable(Eln.LVU, Eln.LVP(), 0.2 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Eln.LVU * 1.3, Eln.LVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                Eln.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Eln.cableHeatingTime, Eln.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.lowVoltageCableDescriptor);
        }

        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Medium Voltage Cable");

            Eln.stdCableRender200V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 2.95f, 0.95f);
            Eln.meduimVoltageCableDescriptor = new ElectricalCableDescriptor(name, Eln.stdCableRender200V,
                "miaou", false);
            Eln.meduimVoltageCableDescriptor.setPhysicalConstantLikeNormalCable(Eln.MVU, Eln.MVP(), 0.10 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Eln.MVU * 1.3, Eln.MVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                30,// electricalOverVoltageStartPowerLost,
                Eln.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Eln.cableHeatingTime, Eln.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.meduimVoltageCableDescriptor);
        }
        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "High Voltage Cable");

            Eln.stdCableRender800V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 3.95f, 1.95f);
            Eln.highVoltageCableDescriptor = new ElectricalCableDescriptor(name, Eln.stdCableRender800V,
                "miaou2", false);
            Eln.highVoltageCableDescriptor.setPhysicalConstantLikeNormalCable(Eln.HVU, Eln.HVP(), 0.025 * 5 / 4 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Eln.HVU * 1.3, Eln.HVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                Eln.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Eln.cableHeatingTime, Eln.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.highVoltageCableDescriptor);

        }
        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Very High Voltage Cable");

            Eln.stdCableRender3200V = new CableRenderDescriptor("eln",
                "sprites/cableVHV.png", 3.95f, 1.95f);
            Eln.veryHighVoltageCableDescriptor = new ElectricalCableDescriptor(name, Eln.stdCableRender3200V,
                "miaou2", false);
            Eln.veryHighVoltageCableDescriptor.setPhysicalConstantLikeNormalCable(Eln.VVU, Eln.VVP(), 0.025 * 5 / 4 / 20 / 8,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Eln.VVU * 1.3, Eln.VVP() * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                Eln.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Eln.cableHeatingTime, Eln.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.veryHighVoltageCableDescriptor);
        }
        /* Disabling Creative Cables as the Current Cables are much better.
           subId = 24;
        */
        {
            subId = 20;
            name = TR_NAME(I18N.Type.NONE, "Signal Bus Cable");

            Eln.stdCableRenderSignalBus = new CableRenderDescriptor("eln",
                "sprites/cable.png", 3.95f, 3.95f);
            Eln.signalBusCableDescriptor = new ElectricalCableDescriptor(name, Eln.stdCableRenderSignalBus,
                "For transmitting many signals.", true);
            Eln.signalBusCableDescriptor.setPhysicalConstantLikeNormalCable(Eln.SVU, Eln.SVP, 0.02 / 50
                    * Eln.gateOutputCurrent / Eln.SVII,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Eln.SVU * 1.3, Eln.SVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                0.5,// electricalOverVoltageStartPowerLost,
                Eln.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Eln.cableHeatingTime, 1// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Eln.signalBusCableDescriptor.ElementClass = ElectricalSignalBusCableElement.class;
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.signalBusCableDescriptor);
        }
        {
            subId = 25;
            name = TR_NAME(I18N.Type.NONE, "Low Current Cable");

            Eln.stdCableRenderLowCurrent = new CableRenderDescriptor("eln", "sprites/currentcable.png", 0.45f, 0.45f);
            Eln.lowCurrentCableDescriptor = new CurrentCableDescriptor(name, Eln.stdCableRenderLowCurrent);
            Eln.lowCurrentCableDescriptor.setCableProperties(
                25,
                MaterialType.COPPER,
                0,
                Eln.cableWarmLimit,
                -100,
                Eln.cableHeatingTime
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.lowCurrentCableDescriptor);
        }
        {
            subId = 26;
            name = TR_NAME(I18N.Type.NONE, "Medium Current Cable");

            Eln.stdCableRenderMediumCurrent = new CableRenderDescriptor("eln", "sprites/currentcable.png", 0.95f, 0.45f);
            Eln.mediumCurrentCableDescriptor = new CurrentCableDescriptor(name, Eln.stdCableRenderMediumCurrent);
            Eln.mediumCurrentCableDescriptor.setCableProperties(
                75,
                MaterialType.COPPER,
                0,
                Eln.cableWarmLimit,
                -100,
                Eln.cableHeatingTime
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.mediumCurrentCableDescriptor);
        }
        {
            subId = 27;
            name = TR_NAME(I18N.Type.NONE, "High Current Cable");

            Eln.stdCableRenderHighCurrent = new CableRenderDescriptor("eln", "sprites/currentcable.png", 1.95f, 0.95f);
            Eln.highCurrentCableDescriptor = new CurrentCableDescriptor(name, Eln.stdCableRenderHighCurrent);
            Eln.highCurrentCableDescriptor.setCableProperties(
                150,
                MaterialType.COPPER,
                0,
                Eln.cableWarmLimit,
                -100,
                Eln.cableHeatingTime
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.highCurrentCableDescriptor);
        }
        {
            subId = 28;
            name = TR_NAME(I18N.Type.NONE, "Very High Current Cable");

            Eln.stdCableRenderVeryHighCurrent = new CableRenderDescriptor("eln", "sprites/currentcable.png", 3.95f, 1.95f);
            Eln.veryHighCurrentCableDescriptor = new CurrentCableDescriptor(name, Eln.stdCableRenderVeryHighCurrent);
            Eln.veryHighCurrentCableDescriptor.setCableProperties(
                600,
                MaterialType.COPPER,
                0,
                Eln.cableWarmLimit,
                -100,
                Eln.cableHeatingTime
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), Eln.veryHighCurrentCableDescriptor);
        }
    }

    private static void registerThermalCable(int id) {
        int subId;
        String name;
        /*
            subID = 0; // removed from mod Copper Thermal Cable
         */
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Copper Thermal Cable");
            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20,
                -200,
                500,
                2000,
                2,
                10,
                0.1,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerGround(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Ground Cable");
            GroundCableDescriptor desc = new GroundCableDescriptor(name, Eln.obj.getObj("groundcable"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Hub");
            HubDescriptor desc = new HubDescriptor(name, Eln.obj.getObj("hub"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalSource(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Source");
            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, Eln.obj.getObj("voltagesource"), false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Signal Source");
            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, Eln.obj.getObj("signalsource"), true);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerLampSocket(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Lamp Socket A");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("ClassicLampSocket"), false),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Lamp Socket B Projector");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("ClassicLampSocket"), false),
                LampSocketType.Douille, // LampSocketType
                false,
                10, -90, 90, 0);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Robust Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("RobustLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            desc.setInitialOrientation(-90.f);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Flat Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("FlatLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Simple Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("SimpleLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Fluorescent Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("FluorescentLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cableLeft = false;
            desc.cableRight = false;
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Street Light");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("StreetLight"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                0, 0, 0, 0);
            desc.setPlaceDirection(Direction.YN);
            GhostGroup g = new GhostGroup();
            g.addElement(1, 0, 0);
            g.addElement(2, 0, 0);
            desc.setGhostGroup(g);
            desc.renderIconInHand = true;
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        {
            subId = 9;
            name = TR_NAME(I18N.Type.NONE, "Sconce Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Eln.obj.getObj("SconceLamp"), true),
                LampSocketType.Douille, // LampSocketType
                true,
                3, 0, 0, 0);
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            desc.setInitialOrientation(-90.f);
            desc.setUserRotationLibertyDegrees(true);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "Suspended Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name,
                new LampSocketSuspendedObjRender(Eln.obj.getObj("RobustLampSuspended"), true, 3),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            desc.setPlaceDirection(Direction.YP);

            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        {
            subId = 13;
            name = TR_NAME(I18N.Type.NONE, "Long Suspended Lamp Socket");
            LampSocketDescriptor desc = new LampSocketDescriptor(name,
                new LampSocketSuspendedObjRender(Eln.obj.getObj("RobustLampSuspended"), true, 7),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            desc.setPlaceDirection(Direction.YP);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        Eln.sixNodeItem.addDescriptor(15 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(I18N.Type.NONE, "50V Emergency Lamp"),
                Eln.lowVoltageCableDescriptor, 10 * 60 * 10, 10, 5, 6, Eln.obj.getObj("EmergencyExitLighting")));
        Eln.sixNodeItem.addDescriptor(16 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(I18N.Type.NONE, "200V Emergency Lamp"),
                Eln.meduimVoltageCableDescriptor, 10 * 60 * 20, 25, 10, 8, Eln.obj.getObj("EmergencyExitLighting")));
    }

    private static void registerLampSupply(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Lamp Supply");
            LampSupplyDescriptor desc = new LampSupplyDescriptor(
                name, Eln.obj.getObj("DistributionBoard"),
                32
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerPowerSocket(int id) {
        int subId;
        String name;
        PowerSocketDescriptor desc;
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "50V Power Socket");
            desc = new PowerSocketDescriptor(
                subId, name, Eln.obj.getObj("PowerSocket"),
                10 //Range for plugged devices (without obstacles)
            );
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "200V Power Socket");
            desc = new PowerSocketDescriptor(
                subId, name, Eln.obj.getObj("PowerSocket"),
                10 //Range for plugged devices (without obstacles)
            );
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerPassiveComponent(int id) {
        int subId;
        String name;
        IFunction function;
        FunctionTableYProtect baseFunction = new FunctionTableYProtect(
            new double[]{0.0, 0.01, 0.03, 0.1, 0.2, 0.4, 0.8, 1.2}, 1.0,
            0, 5);

        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "10A Diode");
            function = new FunctionTableYProtect(new double[]{0.0, 0.1, 0.3,
                1.0, 2.0, 4.0, 8.0, 12.0}, 1.0, 0, 100);
            DiodeDescriptor desc = new DiodeDescriptor(
                name,
                function,
                10,
                1, 10,
                Eln.sixNodeThermalLoadInitializer.copy(),
                Eln.lowVoltageCableDescriptor,
                Eln.obj.getObj("PowerElectricPrimitives"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "25A Diode");
            function = new FunctionTableYProtect(new double[]{0.0, 0.25,
                0.75, 2.5, 5.0, 10.0, 20.0, 30.0}, 1.0, 0, 100);
            DiodeDescriptor desc = new DiodeDescriptor(
                name,
                function,
                25,
                1, 25,
                Eln.sixNodeThermalLoadInitializer.copy(),
                Eln.lowVoltageCableDescriptor,
                Eln.obj.getObj("PowerElectricPrimitives"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Signal Diode");
            function = baseFunction.duplicate(1.0, 0.1);
            DiodeDescriptor desc = new DiodeDescriptor(name,
                function, 0.1,
                1, 0.1,
                Eln.sixNodeThermalLoadInitializer.copy(), Eln.signalCableDescriptor,
                Eln.obj.getObj("PowerElectricPrimitives"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Signal 20H inductor");
            SignalInductorDescriptor desc = new SignalInductorDescriptor(
                name, 20, Eln.lowVoltageCableDescriptor
            );
            desc.setDefaultIcon("empty-texture");
            Eln.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "Power Capacitor");
            PowerCapacitorSixDescriptor desc = new PowerCapacitorSixDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-1), 60 * 2000
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 34;
            name = TR_NAME(I18N.Type.NONE, "Power Inductor");
            PowerInductorSixDescriptor desc = new PowerInductorSixDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-1)
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 36;
            name = TR_NAME(I18N.Type.NONE, "Power Resistor");
            ResistorDescriptor desc = new ResistorDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, false
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 37;
            name = TR_NAME(I18N.Type.NONE, "Rheostat");
            ResistorDescriptor desc = new ResistorDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, true
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 38;
            name = TR_NAME(I18N.Type.NONE, "Thermistor");
            ResistorDescriptor desc = new ResistorDescriptor(
                name, Eln.obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), -0.01, false
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        // TODO: Move Thermal Dissipator to TransparentNode Registry
        {
            subId = 39;
            name = TR_NAME(I18N.Type.NONE, "Large Rheostat");
            ThermalDissipatorPassiveDescriptor dissipator = new ThermalDissipatorPassiveDescriptor(
                name,
                Eln.obj.getObj("LargeRheostat"),
                1000, -100,// double warmLimit,double coolLimit,
                4000, 800,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop
            );
            LargeRheostatDescriptor desc = new LargeRheostatDescriptor(
                name, dissipator, Eln.veryHighVoltageCableDescriptor, SerieEE.newE12(0)
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private static void registerSwitch(int id) {
        int subId;
        String name;
        ElectricalSwitchDescriptor desc;
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Very High Voltage Switch");
            desc = new ElectricalSwitchDescriptor(name, Eln.stdCableRender3200V,
                Eln.obj.getObj("HighVoltageSwitch"), Eln.VVU, Eln.VVP(), Eln.veryHighVoltageCableDescriptor.electricalRs * 2,
                Eln.VVU * 1.5, Eln.VVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "High Voltage Switch");
            desc = new ElectricalSwitchDescriptor(name, Eln.stdCableRender800V,
                Eln.obj.getObj("HighVoltageSwitch"), Eln.HVU, Eln.HVP(), Eln.highVoltageCableDescriptor.electricalRs * 2,
                Eln.HVU * 1.5, Eln.HVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Low Voltage Switch");
            desc = new ElectricalSwitchDescriptor(name, Eln.stdCableRender50V,
                Eln.obj.getObj("LowVoltageSwitch"), Eln.LVU, Eln.LVP(), Eln.lowVoltageCableDescriptor.electricalRs * 2,
                Eln.LVU * 1.5, Eln.LVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;

            name = TR_NAME(I18N.Type.NONE, "Medium Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, Eln.stdCableRender200V,
                Eln.obj.getObj("LowVoltageSwitch"), Eln.MVU, Eln.MVP(), Eln.meduimVoltageCableDescriptor.electricalRs * 2,
                Eln.MVU * 1.5, Eln.MVP() * 1.2,
                Eln.cableThermalLoadInitializer.copy(), false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Signal Switch");
            desc = new ElectricalSwitchDescriptor(name, Eln.stdCableRenderSignal,
                Eln.obj.getObj("LowVoltageSwitch"), Eln.SVU, Eln.SVP, 0.02,
                Eln.SVU * 1.5, Eln.SVP * 1.2,
                Eln.cableThermalLoadInitializer.copy(), true);
            Eln.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
        // 4 taken
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Signal Switch with LED");
            desc = new ElectricalSwitchDescriptor(name, Eln.stdCableRenderSignal,
                Eln.obj.getObj("ledswitch"), Eln.SVU, Eln.SVP, 0.02,
                Eln.SVU * 1.5, Eln.SVP * 1.2,
                Eln.cableThermalLoadInitializer.copy(), true);
            Eln.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    private static void registerSixNodeMisc(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Modbus RTU");
            ModbusRtuDescriptor desc = new ModbusRtuDescriptor(
                name,
                Eln.obj.getObj("RTU")
            );
            if (Eln.modbusEnable) {
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            } else {
                Eln.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
            }
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Analog Watch");
            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                Eln.obj.getObj("WallClock"),
                20000.0 / (3600 * 40)
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Digital Watch");
            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                Eln.obj.getObj("DigitalWallClock"),
                20000.0 / (3600 * 15)
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Digital Display");
            ElectricalDigitalDisplayDescriptor desc = new ElectricalDigitalDisplayDescriptor(
                name,
                Eln.obj.getObj("DigitalDisplay")
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Nixie Tube");
            NixieTubeDescriptor desc = new NixieTubeDescriptor(
                name,
                Eln.obj.getObj("NixieTube")
            );
            Eln.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Tutorial Sign");
            TutorialSignDescriptor desc = new TutorialSignDescriptor(
                name, Eln.obj.getObj("TutoPlate"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalManager(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Breaker");
            ElectricalBreakerDescriptor desc = new ElectricalBreakerDescriptor(name, Eln.obj.getObj("ElectricalBreaker"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Energy Meter");
            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, Eln.obj.getObj("EnergyMeter"), 8, 0);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Advanced Energy Meter");
            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, Eln.obj.getObj("AdvancedEnergyMeter"), 7, 8);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Electrical Fuse Holder");
            ElectricalFuseHolderDescriptor desc = new ElectricalFuseHolderDescriptor(name, Eln.obj.getObj("ElectricalFuse"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for low voltage cables");
            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Eln.lowVoltageCableDescriptor, Eln.obj.getObj("ElectricalFuse"));
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for medium voltage cables");
            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Eln.meduimVoltageCableDescriptor, Eln.obj.getObj("ElectricalFuse"));
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 9;
            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for high voltage cables");
            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Eln.highVoltageCableDescriptor, Eln.obj.getObj("ElectricalFuse"));
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 10;
            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for very high voltage cables");
            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Eln.veryHighVoltageCableDescriptor, Eln.obj.getObj("ElectricalFuse"));
            Eln.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 11;
            name = TR_NAME(I18N.Type.NONE, "Blown Lead Fuse");
            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, null, Eln.obj.getObj("ElectricalFuse"));
            ElectricalFuseDescriptor.Companion.setBlownFuse(desc);
            Eln.sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalSensor(int id) {
        int subId;
        String name;
        ElectricalSensorDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Probe");
            desc = new ElectricalSensorDescriptor(name, "electricalsensor",
                false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Voltage Probe");
            desc = new ElectricalSensorDescriptor(name, "voltagesensor", true);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerThermalSensor(int id) {
        int subId;
        String name;
        ThermalSensorDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Thermal Probe");
            desc = new ThermalSensorDescriptor(name,
                Eln.obj.getObj("thermalsensor"), false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Temperature Probe");
            desc = new ThermalSensorDescriptor(name,
                Eln.obj.getObj("temperaturesensor"), true);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private static void registerElectricalVuMeter(int id) {
        int subId;
        String name;
        ElectricalVuMeterDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Analog vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Vumeter", false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "LED vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Led", true);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalAlarm(int id) {
        int subId;
        String name;
        ElectricalAlarmDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Nuclear Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                Eln.obj.getObj("alarmmedium"), 7, "eln:alarma", 11, 1f);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Standard Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                Eln.obj.getObj("alarmmedium"), 7, "eln:smallalarm_critical",
                1.2, 2f);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalEnvironmentalSensor(int id) {
        int subId;
        String name;
        {
            ElectricalLightSensorDescriptor desc;
            {
                subId = 0;
                name = TR_NAME(I18N.Type.NONE, "Electrical Daylight Sensor");
                desc = new ElectricalLightSensorDescriptor(name, Eln.obj.getObj("daylightsensor"), true);
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
            {
                subId = 1;
                name = TR_NAME(I18N.Type.NONE, "Electrical Light Sensor");
                desc = new ElectricalLightSensorDescriptor(name, Eln.obj.getObj("lightsensor"), false);
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWeatherSensorDescriptor desc;
            {
                subId = 4;
                name = TR_NAME(I18N.Type.NONE, "Electrical Weather Sensor");
                desc = new ElectricalWeatherSensorDescriptor(name, Eln.obj.getObj("electricalweathersensor"));
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWindSensorDescriptor desc;
            {
                subId = 8;
                name = TR_NAME(I18N.Type.NONE, "Electrical Anemometer Sensor");
                desc = new ElectricalWindSensorDescriptor(name, Eln.obj.getObj("Anemometer"), 25);
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalEntitySensorDescriptor desc;
            {
                subId = 12;
                name = TR_NAME(I18N.Type.NONE, "Electrical Entity Sensor");
                desc = new ElectricalEntitySensorDescriptor(name, Eln.obj.getObj("ProximitySensor"), 10);
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 13;
                name = TR_NAME(I18N.Type.NONE, "Electrical Fire Detector");
                desc = new ElectricalFireDetectorDescriptor(name, Eln.obj.getObj("FireDetector"), 15, false);
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 14;
                name = TR_NAME(I18N.Type.NONE, "Electrical Fire Buzzer");
                desc = new ElectricalFireDetectorDescriptor(name, Eln.obj.getObj("FireDetector"), 15, true);
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ScannerDescriptor desc;
            {
                subId = 15;
                name = TR_NAME(I18N.Type.NONE, "Scanner");
                desc = new ScannerDescriptor(name, Eln.obj.getObj("scanner"));
                Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
    }

    private static void registerElectricalRedstone(int id) {
        int subId;
        String name;
        {
            ElectricalRedstoneInputDescriptor desc;
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Redstone-to-Voltage Converter");
            desc = new ElectricalRedstoneInputDescriptor(name, Eln.obj.getObj("redtoele"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalRedstoneOutputDescriptor desc;
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Voltage-to-Redstone Converter");
            desc = new ElectricalRedstoneOutputDescriptor(name,
                Eln.obj.getObj("eletored"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalGate(int id) {
        int subId;
        String name;
        {
            ElectricalTimeoutDescriptor desc;
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Timer");
            desc = new ElectricalTimeoutDescriptor(name,
                Eln.obj.getObj("electricaltimer"));
            desc.setTickSound("eln:timer", 0.01f);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalMathDescriptor desc;
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Signal Processor");
            desc = new ElectricalMathDescriptor(name,
                Eln.obj.getObj("PLC"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerWirelessSignal(int id) {
        int subId;
        String name;
        {
            WirelessSignalRxDescriptor desc;
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Wireless Signal Receiver");
            desc = new WirelessSignalRxDescriptor(
                name,
                Eln.obj.getObj("wirelesssignalrx")
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            WirelessSignalTxDescriptor desc;
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Wireless Signal Transmitter");
            desc = new WirelessSignalTxDescriptor(
                name,
                Eln.obj.getObj("wirelesssignaltx"),
                Eln.wirelessTxRange
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            WirelessSignalRepeaterDescriptor desc;
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Wireless Signal Repeater");
            desc = new WirelessSignalRepeaterDescriptor(
                name,
                Eln.obj.getObj("wirelesssignalrepeater"),
                Eln.wirelessTxRange
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalDataLogger(int id) {
        int subId;
        String name;
        {
            ElectricalDataLoggerDescriptor desc;
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Data Logger");
            desc = new ElectricalDataLoggerDescriptor(name, true,
                "DataloggerCRTFloor", 1f, 0.5f, 0f, "\u00a76");
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalDataLoggerDescriptor desc;
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Modern Data Logger");
            desc = new ElectricalDataLoggerDescriptor(name, true,
                "FlatScreenMonitor", 0.0f, 1f, 0.0f, "\u00A7a");
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalDataLoggerDescriptor desc;
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Industrial Data Logger");
            desc = new ElectricalDataLoggerDescriptor(name, false,
                "IndustrialPanel", 0.25f, 0.5f, 1f, "\u00A7f");
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalRelay(int id) {
        int subId;
        String name;
        ElectricalRelayDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Low Voltage Relay");
            desc = new ElectricalRelayDescriptor(
                name, Eln.obj.getObj("RelayBig"),
                Eln.lowVoltageCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Medium Voltage Relay");
            desc = new ElectricalRelayDescriptor(
                name, Eln.obj.getObj("RelayBig"),
                Eln.meduimVoltageCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "High Voltage Relay");
            desc = new ElectricalRelayDescriptor(
                name, Eln.obj.getObj("relay800"),
                Eln.highVoltageCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Very High Voltage Relay");
            desc = new ElectricalRelayDescriptor(
                name, Eln.obj.getObj("relay800"),
                Eln.veryHighVoltageCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Signal Relay");
            desc = new ElectricalRelayDescriptor(
                name, Eln.obj.getObj("RelaySmall"),
                Eln.signalCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Low Current Relay");
            desc = new ElectricalRelayDescriptor(name, Eln.obj.getObj("RelaySmall"), Eln.lowCurrentCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId  + (id << 6), desc);
        }

        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Medium Current Relay");
            desc = new ElectricalRelayDescriptor(name, Eln.obj.getObj("RelaySmall"), Eln.mediumCurrentCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "High Current Relay");
            desc = new ElectricalRelayDescriptor(name, Eln.obj.getObj("RelaySmall"), Eln.highCurrentCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Very High Current Relay");
            desc = new ElectricalRelayDescriptor(name, Eln.obj.getObj("relay800"), Eln.veryHighCurrentCableDescriptor);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerElectricalGateSource(int id) {
        int subId;
        String name;
        ElectricalGateSourceRenderObj signalsourcepot = new ElectricalGateSourceRenderObj(Eln.obj.getObj("signalsourcepot"));
        ElectricalGateSourceRenderObj ledswitch = new ElectricalGateSourceRenderObj(Eln.obj.getObj("ledswitch"));
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Signal Trimmer");
            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, signalsourcepot, false,
                "trimmer");
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Signal Switch");
            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true,
                Eln.noSymbols ? "signalswitch" : "switch");
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Signal Button");
            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true, "button");
            desc.setWithAutoReset();
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "Wireless Button");
            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, Eln.wirelessTxRange, true);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Wireless Switch");
            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, Eln.wirelessTxRange, false);
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private static void registerLogicalGates(int id) {
        Obj3D model = Eln.obj.getObj("LogicGates");
        Eln.sixNodeItem.addDescriptor(0 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "NOT Chip"), model, "NOT", Not.class));
        Eln.sixNodeItem.addDescriptor(1 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "AND Chip"), model, "AND", And.class));
        Eln.sixNodeItem.addDescriptor(2 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "NAND Chip"), model, "NAND", Nand.class));
        Eln.sixNodeItem.addDescriptor(3 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "OR Chip"), model, "OR", Or.class));
        Eln.sixNodeItem.addDescriptor(4 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "NOR Chip"), model, "NOR", Nor.class));
        Eln.sixNodeItem.addDescriptor(5 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "XOR Chip"), model, "XOR", Xor.class));
        Eln.sixNodeItem.addDescriptor(6 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "XNOR Chip"), model, "XNOR", XNor.class));
        Eln.sixNodeItem.addDescriptor(7 + (id << 6),
            new PalDescriptor(TR_NAME(I18N.Type.NONE, "PAL Chip"), model));
        Eln.sixNodeItem.addDescriptor(8 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "Schmitt Trigger Chip"), model, "SCHMITT",
                SchmittTrigger.class));
        Eln.sixNodeItem.addDescriptor(9 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "D Flip Flop Chip"), model, "DFF", DFlipFlop.class));
        Eln.sixNodeItem.addDescriptor(10 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "Oscillator Chip"), model, "OSC", Oscillator.class));
        Eln.sixNodeItem.addDescriptor(11 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "JK Flip Flop Chip"), model, "JKFF", JKFlipFlop.class));
    }

    private static void registerAnalogChips(int id) {
        id <<= 6;
        Obj3D model = Eln.obj.getObj("AnalogChips");
        Eln.sixNodeItem.addDescriptor(id + 0,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "OpAmp"), model, "OP", OpAmp.class));
        Eln.sixNodeItem.addDescriptor(id + 1, new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "PID Regulator"), model, "PID",
            PIDRegulator.class, PIDRegulatorElement.class, PIDRegulatorRender.class));
        Eln.sixNodeItem.addDescriptor(id + 2,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Voltage controlled sawtooth oscillator"), model, "VCO-SAW",
                VoltageControlledSawtoothOscillator.class));
        Eln.sixNodeItem.addDescriptor(id + 3,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Voltage controlled sine oscillator"), model, "VCO-SIN",
                VoltageControlledSineOscillator.class));
        Eln.sixNodeItem.addDescriptor(id + 4,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Amplifier"), model, "AMP",
                Amplifier.class, AmplifierElement.class, AmplifierRender.class));
        Eln.sixNodeItem.addDescriptor(id + 5,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Voltage controlled amplifier"), model, "VCA",
                VoltageControlledAmplifier.class));
        Eln.sixNodeItem.addDescriptor(id + 6,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Configurable summing unit"), model, "SUM",
                SummingUnit.class, SummingUnitElement.class, SummingUnitRender.class));
        Eln.sixNodeItem.addDescriptor(id + 7,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Sample and hold"), model, "SAH",
                SampleAndHold.class));
        Eln.sixNodeItem.addDescriptor(id + 8,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Lowpass filter"), model, "LPF",
                Filter.class, FilterElement.class, FilterRender.class));
    }

    private static void registerTreeResinCollector(int id) {
        int subId;
        String name;
        TreeResinCollectorDescriptor descriptor;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Tree Resin Collector");
            descriptor = new TreeResinCollectorDescriptor(name, Eln.obj.getObj("treeresincolector"));
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }

    private static void registerBatteryCharger(int id) {
        int subId;
        String name;
        BatteryChargerDescriptor descriptor;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Weak 50V Battery Charger");
            descriptor = new BatteryChargerDescriptor(
                name, Eln.obj.getObj("batterychargera"),
                Eln.lowVoltageCableDescriptor,
                Eln.LVU, 200
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "50V Battery Charger");
            descriptor = new BatteryChargerDescriptor(
                name, Eln.obj.getObj("batterychargera"),
                Eln.lowVoltageCableDescriptor,
                Eln.LVU, 400
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Battery Charger");
            descriptor = new BatteryChargerDescriptor(
                name, Eln.obj.getObj("batterychargera"),
                Eln.meduimVoltageCableDescriptor,
                Eln.MVU, 1000
            );
            Eln.sixNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }

}

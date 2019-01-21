package mods.eln.registration;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.i18n.I18N;
import mods.eln.item.ElectricalFuseDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.series.SerieEE;
import mods.eln.signalinductor.SignalInductorDescriptor;
import mods.eln.sixnode.*;
import mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor;
import mods.eln.sixnode.batterycharger.BatteryChargerDescriptor;
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

public class SixNodeRegistration {

    public SixNodeRegistration() {

    }

    public void registerSixNode() {
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

    private void registerGround(int id) {
        int subId;
        String name;

        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Ground Cable");

            GroundCableDescriptor desc = new GroundCableDescriptor(name, Vars.obj.getObj("groundcable"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Hub");

            HubDescriptor desc = new HubDescriptor(name, Vars.obj.getObj("hub"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalSource(int id) {
        int subId;
        String name;

        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Source");

            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, Vars.obj.getObj("voltagesource"), false);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Signal Source");

            ElectricalSourceDescriptor desc = new ElectricalSourceDescriptor(
                name, Vars.obj.getObj("signalsource"), true);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalCable(int id) {
        int subId;
        String name;
        ElectricalCableDescriptor desc;
        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Signal Cable");

            Vars.stdCableRenderSignal = new CableRenderDescriptor("eln",
                "sprites/cable.png", 0.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRenderSignal,
                "For signal transmission.", true);

            Vars.signalCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.SVU, Vars.SVP, 0.02 / 50
                    * Vars.gateOutputCurrent / Vars.SVII,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.SVU * 1.3, Vars.SVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                0.5,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, 1// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            // GameRegistry.registerCustomItemStack(name, desc.newItemStack(1));

        }

        {
            subId = 4;

            name = TR_NAME(I18N.Type.NONE, "Low Voltage Cable");

            Vars.stdCableRender50V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 1.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRender50V,
                "For low voltage with high current.", false);

            Vars.lowVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.LVU, Vars.LVP, 0.2 / 20 * Vars.cableRsFactor,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.LVU * 1.3, Vars.LVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, Vars.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRender50V,
                "For low voltage with high current.", false);

            desc.setPhysicalConstantLikeNormalCable(
                Vars.LVU, Vars.LVP / 4, 0.2 / 20,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.LVU * 1.3, Vars.LVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                20,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, Vars.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Vars.batteryCableDescriptor = desc;

        }

        {
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Medium Voltage Cable");

            Vars.stdCableRender200V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 2.95f, 0.95f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRender200V,
                "miaou", false);

            Vars.meduimVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.MVU, Vars.MVP, 0.10 / 20 * Vars.cableRsFactor,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.MVU * 1.3, Vars.MVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                30,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, Vars.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }
        {
            subId = 12;

            // highVoltageCableId = subId;
            name = TR_NAME(I18N.Type.NONE, "High Voltage Cable");

            Vars.stdCableRender800V = new CableRenderDescriptor("eln",
                "sprites/cable.png", 3.95f, 1.95f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRender800V,
                "miaou2", false);

            Vars.highVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.HVU, Vars.HVP, 0.025 * 5 / 4 / 20 * Vars.cableRsFactor,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.HVU * 1.3, Vars.HVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, Vars.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }


        {
            subId = 16;

            // highVoltageCableId = subId;
            name = TR_NAME(I18N.Type.NONE, "Very High Voltage Cable");

            Vars.stdCableRender3200V = new CableRenderDescriptor("eln",
                "sprites/cableVHV.png", 3.95f, 1.95f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRender3200V,
                "miaou2", false);

            Vars.veryHighVoltageCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.VVU, Vars.VVP, 0.025 * 5 / 4 / 20 / 8 * Vars.cableRsFactor,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.VVU * 1.3, Vars.VVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                40,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, Vars.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);

        }

        {
            subId = 24;

            name = TR_NAME(I18N.Type.NONE, "Creative Cable");

            Vars.stdCableRenderCreative = new CableRenderDescriptor("eln",
                "sprites/cablecreative.png", 8.0f, 4.0f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRenderCreative,
                "Experience the power of Microresistance", false);

            Vars.creativeCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.VVU, Vars.VVU * Vars.VVP, 0.025 * 5 / 4 / 20 / 8 * Vars.cableRsFactor / 9001, //what!?
                Vars.VVU * 1.3, Vars.VVU * Vars.VVP * 1.2,
                40,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, Vars.cableThermalConductionTao// thermalNominalHeatTime,
                // thermalConductivityTao
            );
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 20;

            name = TR_NAME(I18N.Type.NONE, "Signal Bus Cable");

            Vars.stdCableRenderSignalBus = new CableRenderDescriptor("eln",
                "sprites/cable.png", 3.95f, 3.95f);

            desc = new ElectricalCableDescriptor(name, Vars.stdCableRenderSignalBus,
                "For transmitting many signals.", true);

            Vars.signalBusCableDescriptor = desc;

            desc.setPhysicalConstantLikeNormalCable(Vars.SVU, Vars.SVP, 0.02 / 50
                    * Vars.gateOutputCurrent / Vars.SVII,// electricalNominalVoltage,
                // electricalNominalPower,
                // electricalNominalPowerDrop,
                Vars.SVU * 1.3, Vars.SVP * 1.2,// electricalMaximalVoltage,
                // electricalMaximalPower,
                0.5,// electricalOverVoltageStartPowerLost,
                Vars.cableWarmLimit, -100,// thermalWarmLimit, thermalCoolLimit,
                Vars.cableHeatingTime, 1// thermalNominalHeatTime,
                // thermalConductivityTao
            );

            desc.ElementClass = ElectricalSignalBusCableElement.class;

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            // GameRegistry.registerCustomItemStack(name, desc.newItemStack(1));

        }
    }

    private void registerThermalCable(int id) {
        int subId;
        String name;

        {
            subId = 0;

            name = "Removed from mod Copper Thermal Cable";

            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
                500, 2000, // thermalStdT, thermalStdPower,
                2, 400, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");// description

            desc.addToData(false);
            desc.setDefaultIcon("empty-texture");
            Vars.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);

        }

        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Copper Thermal Cable");

            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
                500, 2000, // thermalStdT, thermalStdPower,
                2, 10, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");// description

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerLampSocket(int id) {
        int subId;
        String name;

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Lamp Socket A");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("ClassicLampSocket"), false),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Lamp Socket B Projector");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("ClassicLampSocket"), false),
                LampSocketType.Douille, // LampSocketType
                false,
                10, -90, 90, 0);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 4;

            name = TR_NAME(I18N.Type.NONE, "Robust Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("RobustLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            desc.setInitialOrientation(-90.f);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;

            name = TR_NAME(I18N.Type.NONE, "Flat Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("FlatLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;

            name = TR_NAME(I18N.Type.NONE, "Simple Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("SimpleLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;

            name = TR_NAME(I18N.Type.NONE, "Fluorescent Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("FluorescentLamp"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);


            desc.cableLeft = false;
            desc.cableRight = false;
        }
        {
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Street Light");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("StreetLight"), true),
                LampSocketType.Douille, // LampSocketType
                false,
                0, 0, 0, 0);
            desc.setPlaceDirection(Direction.YN);
            GhostGroup g = new GhostGroup();
            g.addElement(1, 0, 0);
            g.addElement(2, 0, 0);
            desc.setGhostGroup(g);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        {
            subId = 9;

            name = TR_NAME(I18N.Type.NONE, "Sconce Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name, new LampSocketStandardObjRender(Vars.obj.getObj("SconceLamp"), true),
                LampSocketType.Douille, // LampSocketType
                true,
                3, 0, 0, 0);
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            desc.setInitialOrientation(-90.f);
            desc.setUserRotationLibertyDegrees(true);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;

            name = TR_NAME(I18N.Type.NONE, "Suspended Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name,
                new LampSocketSuspendedObjRender(Vars.obj.getObj("RobustLampSuspended"), true, 3),
                LampSocketType.Douille, // LampSocketType
                false,
                3, 0, 0, 0);
            desc.setPlaceDirection(Direction.YP);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }
        {
            subId = 13;

            name = TR_NAME(I18N.Type.NONE, "Long Suspended Lamp Socket");

            LampSocketDescriptor desc = new LampSocketDescriptor(name,
                new LampSocketSuspendedObjRender(Vars.obj.getObj("RobustLampSuspended"), true, 7),
                LampSocketType.Douille, // LampSocketType
                false,
                4, 0, 0, 0);
            desc.setPlaceDirection(Direction.YP);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.cameraOpt = false;
        }

        // TODO: Modern street light.

        Vars.sixNodeItem.addDescriptor(15 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(I18N.Type.NONE, "50V Emergency Lamp"),
                Vars.lowVoltageCableDescriptor, 10 * 60 * 10, 10, 5, 6, Vars.obj.getObj("EmergencyExitLighting")));

        Vars.sixNodeItem.addDescriptor(16 + (id << 6),
            new EmergencyLampDescriptor(TR_NAME(I18N.Type.NONE, "200V Emergency Lamp"),
                Vars.meduimVoltageCableDescriptor, 10 * 60 * 20, 25, 10, 8, Vars.obj.getObj("EmergencyExitLighting")));
    }

    private void registerLampSupply(int id) {
        int subId;
        String name;

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Lamp Supply");

            LampSupplyDescriptor desc = new LampSupplyDescriptor(
                name, Vars.obj.getObj("DistributionBoard"),
                32
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerBatteryCharger(int id) {
        int subId, completId;
        String name;

        BatteryChargerDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Weak 50V Battery Charger");

            descriptor = new BatteryChargerDescriptor(
                name, Vars.obj.getObj("batterychargera"),
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                Vars.LVU, 200// double nominalVoltage,double nominalPower
            );
            Vars.sixNodeItem.addDescriptor(completId, descriptor);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "50V Battery Charger");

            descriptor = new BatteryChargerDescriptor(
                name, Vars.obj.getObj("batterychargera"),
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                Vars.LVU, 400// double nominalVoltage,double nominalPower
            );
            Vars.sixNodeItem.addDescriptor(completId, descriptor);
        }
        {
            subId = 4;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "200V Battery Charger");

            descriptor = new BatteryChargerDescriptor(
                name, Vars.obj.getObj("batterychargera"),
                Vars.meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                Vars.MVU, 1000// double nominalVoltage,double nominalPower
            );
            Vars.sixNodeItem.addDescriptor(completId, descriptor);
        }
    }

    private void registerPowerSocket(int id) {
        int subId;
        String name;
        PowerSocketDescriptor desc;
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "50V Power Socket");
            desc = new PowerSocketDescriptor(
                subId, name, Vars.obj.getObj("PowerSocket"),
                10 //Range for plugged devices (without obstacles)
            );
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "200V Power Socket");
            desc = new PowerSocketDescriptor(
                subId, name, Vars.obj.getObj("PowerSocket"),
                10 //Range for plugged devices (without obstacles)
            );
            desc.setPlaceDirection(new Direction[]{Direction.XP, Direction.XN, Direction.ZP, Direction.ZN});
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerWirelessSignal(int id) {
        int subId;
        String name;

        {
            WirelessSignalRxDescriptor desc;
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Wireless Signal Receiver");

            desc = new WirelessSignalRxDescriptor(
                name,
                Vars.obj.getObj("wirelesssignalrx")

            );
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            WirelessSignalTxDescriptor desc;
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Wireless Signal Transmitter");

            desc = new WirelessSignalTxDescriptor(
                name,
                Vars.obj.getObj("wirelesssignaltx"),
                Vars.wirelessTxRange
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            WirelessSignalRepeaterDescriptor desc;
            subId = 16;

            name = TR_NAME(I18N.Type.NONE, "Wireless Signal Repeater");

            desc = new WirelessSignalRepeaterDescriptor(
                name,
                Vars.obj.getObj("wirelesssignalrepeater"),
                Vars.wirelessTxRange
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerElectricalDataLogger(int id) {
        int subId;
        String name;
        {
            ElectricalDataLoggerDescriptor desc;
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, true,
                "DataloggerCRTFloor", 1f, 0.5f, 0f, "\u00a76");
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalDataLoggerDescriptor desc;
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Modern Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, true,
                "FlatScreenMonitor", 0.0f, 1f, 0.0f, "\u00A7a");
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalDataLoggerDescriptor desc;
            subId = 2;

            name = TR_NAME(I18N.Type.NONE, "Industrial Data Logger");

            desc = new ElectricalDataLoggerDescriptor(name, false,
                "IndustrialPanel", 0.25f, 0.5f, 1f, "\u00A7f");
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalRelay(int id) {
        int subId;
        String name;
        ElectricalRelayDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Low Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, Vars.obj.getObj("RelayBig"),
                Vars.lowVoltageCableDescriptor);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Medium Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, Vars.obj.getObj("RelayBig"),
                Vars.meduimVoltageCableDescriptor);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;

            name = TR_NAME(I18N.Type.NONE, "High Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, Vars.obj.getObj("relay800"),
                Vars.highVoltageCableDescriptor);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;

            name = TR_NAME(I18N.Type.NONE, "Very High Voltage Relay");

            desc = new ElectricalRelayDescriptor(
                name, Vars.obj.getObj("relay800"),
                Vars.veryHighVoltageCableDescriptor);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 4;

            name = TR_NAME(I18N.Type.NONE, "Signal Relay");

            desc = new ElectricalRelayDescriptor(
                name, Vars.obj.getObj("RelaySmall"),
                Vars.signalCableDescriptor);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalGateSource(int id) {
        int subId;
        String name;

        ElectricalGateSourceRenderObj signalsourcepot = new ElectricalGateSourceRenderObj(Vars.obj.getObj("signalsourcepot"));
        ElectricalGateSourceRenderObj ledswitch = new ElectricalGateSourceRenderObj(Vars.obj.getObj("ledswitch"));

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Signal Trimmer");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, signalsourcepot, false,
                "trimmer");

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Signal Switch");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true,
                Vars.noSymbols ? "signalswitch" : "switch");

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Signal Button");

            ElectricalGateSourceDescriptor desc = new ElectricalGateSourceDescriptor(name, ledswitch, true, "button");
            desc.setWithAutoReset();
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 12;

            name = TR_NAME(I18N.Type.NONE, "Wireless Button");

            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, Vars.wirelessTxRange, true);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 16;

            name = TR_NAME(I18N.Type.NONE, "Wireless Switch");

            WirelessSignalSourceDescriptor desc = new WirelessSignalSourceDescriptor(name, ledswitch, Vars.wirelessTxRange, false);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerPassiveComponent(int id) {
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
                name,// int iconId, String name,
                function,
                10, // double Imax,
                1, 10,
                Vars.sixNodeThermalLoadInitializer.copy(),
                Vars.lowVoltageCableDescriptor,
                Vars.obj.getObj("PowerElectricPrimitives"));

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "25A Diode");

            function = new FunctionTableYProtect(new double[]{0.0, 0.25,
                0.75, 2.5, 5.0, 10.0, 20.0, 30.0}, 1.0, 0, 100);

            DiodeDescriptor desc = new DiodeDescriptor(
                name,// int iconId, String name,
                function,
                25, // double Imax,
                1, 25,
                Vars.sixNodeThermalLoadInitializer.copy(),
                Vars.lowVoltageCableDescriptor,
                Vars.obj.getObj("PowerElectricPrimitives"));

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Signal Diode");

            function = baseFunction.duplicate(1.0, 0.1);

            DiodeDescriptor desc = new DiodeDescriptor(name,// int iconId,
                // String name,
                function, 0.1, // double Imax,
                1, 0.1,
                Vars.sixNodeThermalLoadInitializer.copy(), Vars.signalCableDescriptor,
                Vars.obj.getObj("PowerElectricPrimitives"));

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 16;

            name = TR_NAME(I18N.Type.NONE, "Signal 20H inductor");

            SignalInductorDescriptor desc = new SignalInductorDescriptor(
                name, 20, Vars.lowVoltageCableDescriptor
            );

            desc.setDefaultIcon("empty-texture");
            Vars.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 32;

            name = TR_NAME(I18N.Type.NONE, "Power Capacitor");

            PowerCapacitorSixDescriptor desc = new PowerCapacitorSixDescriptor(
                name, Vars.obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-1), 60 * 2000
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 34;

            name = TR_NAME(I18N.Type.NONE, "Power Inductor");

            PowerInductorSixDescriptor desc = new PowerInductorSixDescriptor(
                name, Vars.obj.getObj("PowerElectricPrimitives"), SerieEE.newE6(-1)
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 36;

            name = TR_NAME(I18N.Type.NONE, "Power Resistor");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, Vars.obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, false
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 37;
            name = TR_NAME(I18N.Type.NONE, "Rheostat");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, Vars.obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), 0, true
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 38;

            name = TR_NAME(I18N.Type.NONE, "Thermistor");

            ResistorDescriptor desc = new ResistorDescriptor(
                name, Vars.obj.getObj("PowerElectricPrimitives"), SerieEE.newE12(-2), -0.01, false
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 39;

            name = TR_NAME(I18N.Type.NONE, "Large Rheostat");

            ThermalDissipatorPassiveDescriptor dissipator = new ThermalDissipatorPassiveDescriptor(
                name,
                Vars.obj.getObj("LargeRheostat"),
                1000, -100,// double warmLimit,double coolLimit,
                4000, 800,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop
            );
            LargeRheostatDescriptor desc = new LargeRheostatDescriptor(
                name, dissipator, Vars.veryHighVoltageCableDescriptor, SerieEE.newE12(0)
            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerSwitch(int id) {
        int subId;
        String name;
        ElectricalSwitchDescriptor desc;


        {
            subId = 4;

            name = TR_NAME(I18N.Type.NONE, "Very High Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, Vars.stdCableRender3200V,
                Vars.obj.getObj("HighVoltageSwitch"), Vars.VVU, Vars.VVP, Vars.veryHighVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                Vars.VVU * 1.5, Vars.VVP * 1.2,// maximalVoltage, maximalPower
                Vars.cableThermalLoadInitializer.copy(), false);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "High Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, Vars.stdCableRender800V,
                Vars.obj.getObj("HighVoltageSwitch"), Vars.HVU, Vars.HVP, Vars.highVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                Vars.HVU * 1.5, Vars.HVP * 1.2,// maximalVoltage, maximalPower
                Vars.cableThermalLoadInitializer.copy(), false);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Low Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, Vars.stdCableRender50V,
                Vars.obj.getObj("LowVoltageSwitch"), Vars.LVU, Vars.LVP, Vars.lowVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                Vars.LVU * 1.5, Vars.LVP * 1.2,// maximalVoltage, maximalPower
                Vars.cableThermalLoadInitializer.copy(), false);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;

            name = TR_NAME(I18N.Type.NONE, "Medium Voltage Switch");

            desc = new ElectricalSwitchDescriptor(name, Vars.stdCableRender200V,
                Vars.obj.getObj("LowVoltageSwitch"), Vars.MVU, Vars.MVP, Vars.meduimVoltageCableDescriptor.electricalRs * 2,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                Vars.MVU * 1.5, Vars.MVP * 1.2,// maximalVoltage, maximalPower
                Vars.cableThermalLoadInitializer.copy(), false);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;

            name = TR_NAME(I18N.Type.NONE, "Signal Switch");

            desc = new ElectricalSwitchDescriptor(name, Vars.stdCableRenderSignal,
                Vars.obj.getObj("LowVoltageSwitch"), Vars.SVU, Vars.SVP, 0.02,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                Vars.SVU * 1.5, Vars.SVP * 1.2,// maximalVoltage, maximalPower
                Vars.cableThermalLoadInitializer.copy(), true);

            Vars.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
        // 4 taken
        {
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Signal Switch with LED");

            desc = new ElectricalSwitchDescriptor(name, Vars.stdCableRenderSignal,
                Vars.obj.getObj("ledswitch"), Vars.SVU, Vars.SVP, 0.02,// nominalVoltage,
                // nominalPower,
                // nominalDropFactor,
                Vars.SVU * 1.5, Vars.SVP * 1.2,// maximalVoltage, maximalPower
                Vars.cableThermalLoadInitializer.copy(), true);

            Vars.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

    }

    private void registerElectricalManager(int id) {
        int subId;
        String name;

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Electrical Breaker");

            ElectricalBreakerDescriptor desc = new ElectricalBreakerDescriptor(name, Vars.obj.getObj("ElectricalBreaker"));

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;

            name = TR_NAME(I18N.Type.NONE, "Energy Meter");

            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, Vars.obj.getObj("EnergyMeter"), 8, 0);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;

            name = TR_NAME(I18N.Type.NONE, "Advanced Energy Meter");

            EnergyMeterDescriptor desc = new EnergyMeterDescriptor(name, Vars.obj.getObj("AdvancedEnergyMeter"), 7, 8);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;

            name = TR_NAME(I18N.Type.NONE, "Electrical Fuse Holder");

            ElectricalFuseHolderDescriptor desc = new ElectricalFuseHolderDescriptor(name, Vars.obj.getObj("ElectricalFuse"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 7;

            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for low voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Vars.lowVoltageCableDescriptor, Vars.obj.getObj("ElectricalFuse"));
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 8;

            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for medium voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Vars.meduimVoltageCableDescriptor, Vars.obj.getObj("ElectricalFuse"));
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 9;

            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for high voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Vars.highVoltageCableDescriptor, Vars.obj.getObj("ElectricalFuse"));
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 10;

            name = TR_NAME(I18N.Type.NONE, "Lead Fuse for very high voltage cables");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, Vars.veryHighVoltageCableDescriptor, Vars.obj.getObj("ElectricalFuse"));
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 11;

            name = TR_NAME(I18N.Type.NONE, "Blown Lead Fuse");

            ElectricalFuseDescriptor desc = new ElectricalFuseDescriptor(name, null, Vars.obj.getObj("ElectricalFuse"));
            ElectricalFuseDescriptor.Companion.setBlownFuse(desc);
            Vars.sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    private void registerElectricalSensor(int id) {
        int subId;
        String name;
        ElectricalSensorDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Electrical Probe");

            desc = new ElectricalSensorDescriptor(name, "electricalsensor",
                false);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Voltage Probe");

            desc = new ElectricalSensorDescriptor(name, "voltagesensor", true);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerThermalSensor(int id) {
        int subId;
        String name;
        ThermalSensorDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Thermal Probe");

            desc = new ThermalSensorDescriptor(name,
                Vars.obj.getObj("thermalsensor"), false);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(I18N.Type.NONE, "Temperature Probe");

            desc = new ThermalSensorDescriptor(name,
                Vars.obj.getObj("temperaturesensor"), true);

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerElectricalVuMeter(int id) {
        int subId;
        String name;
        ElectricalVuMeterDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Analog vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Vumeter", false);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "LED vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Led", true);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalAlarm(int id) {
        int subId;
        String name;
        ElectricalAlarmDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Nuclear Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                Vars.obj.getObj("alarmmedium"), 7, "eln:alarma", 11, 1f);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Standard Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                Vars.obj.getObj("alarmmedium"), 7, "eln:smallalarm_critical",
                1.2, 2f);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalEnvironmentalSensor(int id) {
        int subId;
        String name;
        {
            ElectricalLightSensorDescriptor desc;
            {
                subId = 0;
                name = TR_NAME(I18N.Type.NONE, "Electrical Daylight Sensor");
                desc = new ElectricalLightSensorDescriptor(name, Vars.obj.getObj("daylightsensor"), true);
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
            {
                subId = 1;
                name = TR_NAME(I18N.Type.NONE, "Electrical Light Sensor");
                desc = new ElectricalLightSensorDescriptor(name, Vars.obj.getObj("lightsensor"), false);
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWeatherSensorDescriptor desc;
            {
                subId = 4;
                name = TR_NAME(I18N.Type.NONE, "Electrical Weather Sensor");
                desc = new ElectricalWeatherSensorDescriptor(name, Vars.obj.getObj("electricalweathersensor"));
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWindSensorDescriptor desc;
            {
                subId = 8;
                name = TR_NAME(I18N.Type.NONE, "Electrical Anemometer Sensor");
                desc = new ElectricalWindSensorDescriptor(name, Vars.obj.getObj("Anemometer"), 25);
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalEntitySensorDescriptor desc;
            {
                subId = 12;
                name = TR_NAME(I18N.Type.NONE, "Electrical Entity Sensor");
                desc = new ElectricalEntitySensorDescriptor(name, Vars.obj.getObj("ProximitySensor"), 10);
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 13;
                name = TR_NAME(I18N.Type.NONE, "Electrical Fire Detector");
                desc = new ElectricalFireDetectorDescriptor(name, Vars.obj.getObj("FireDetector"), 15, false);
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 14;
                name = TR_NAME(I18N.Type.NONE, "Electrical Fire Buzzer");
                desc = new ElectricalFireDetectorDescriptor(name, Vars.obj.getObj("FireDetector"), 15, true);
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ScannerDescriptor desc;
            {
                subId = 15;
                name = TR_NAME(I18N.Type.NONE, "Scanner");
                desc = new ScannerDescriptor(name, Vars.obj.getObj("scanner"));
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
    }

    private void registerElectricalRedstone(int id) {
        int subId;
        String name;
        {
            ElectricalRedstoneInputDescriptor desc;
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Redstone-to-Voltage Converter");
            desc = new ElectricalRedstoneInputDescriptor(name, Vars.obj.getObj("redtoele"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalRedstoneOutputDescriptor desc;
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Voltage-to-Redstone Converter");
            desc = new ElectricalRedstoneOutputDescriptor(name,
                Vars.obj.getObj("eletored"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalGate(int id) {
        int subId;
        String name;
        {
            ElectricalTimeoutDescriptor desc;
            subId = 0;

            name = TR_NAME(I18N.Type.NONE, "Electrical Timer");

            desc = new ElectricalTimeoutDescriptor(name,
                Vars.obj.getObj("electricaltimer"));
            desc.setTickSound("eln:timer", 0.01f);
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            ElectricalMathDescriptor desc;
            subId = 4;

            name = TR_NAME(I18N.Type.NONE, "Signal Processor");

            desc = new ElectricalMathDescriptor(name,
                Vars.obj.getObj("PLC"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerTreeResinCollector(int id) {
        int subId, completId;
        String name;

        TreeResinCollectorDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Tree Resin Collector");

            descriptor = new TreeResinCollectorDescriptor(name, Vars.obj.getObj("treeresincolector"));
            Vars.sixNodeItem.addDescriptor(completId, descriptor);
        }
    }

    private void registerSixNodeMisc(int id) {

        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Modbus RTU");

            ModbusRtuDescriptor desc = new ModbusRtuDescriptor(
                name,
                Vars.obj.getObj("RTU")

            );

            if (Vars.modbusEnable) {
                Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
            } else {
                Vars.sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);
            }
        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Analog Watch");

            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                Vars.obj.getObj("WallClock"),
                20000.0 / (3600 * 40)

            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Digital Watch");

            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                Vars.obj.getObj("DigitalWallClock"),
                20000.0 / (3600 * 15)

            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Digital Display");

            ElectricalDigitalDisplayDescriptor desc = new ElectricalDigitalDisplayDescriptor(
                name,
                Vars.obj.getObj("DigitalDisplay")
            );

            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Nixie Tube");

            NixieTubeDescriptor desc = new NixieTubeDescriptor(
                name,
                Vars.obj.getObj("NixieTube")
            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Tutorial Sign");

            TutorialSignDescriptor desc = new TutorialSignDescriptor(
                name, Vars.obj.getObj("TutoPlate"));
            Vars.sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerLogicalGates(int id) {
        Obj3D model = Vars.obj.getObj("LogicGates");
        Vars.sixNodeItem.addDescriptor(0 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "NOT Chip"), model, "NOT", Not.class));

        Vars.sixNodeItem.addDescriptor(1 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "AND Chip"), model, "AND", And.class));
        Vars.sixNodeItem.addDescriptor(2 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "NAND Chip"), model, "NAND", Nand.class));

        Vars.sixNodeItem.addDescriptor(3 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "OR Chip"), model, "OR", Or.class));
        Vars.sixNodeItem.addDescriptor(4 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "NOR Chip"), model, "NOR", Nor.class));

        Vars.sixNodeItem.addDescriptor(5 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "XOR Chip"), model, "XOR", Xor.class));
        Vars.sixNodeItem.addDescriptor(6 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "XNOR Chip"), model, "XNOR", XNor.class));

        Vars.sixNodeItem.addDescriptor(7 + (id << 6),
            new PalDescriptor(TR_NAME(I18N.Type.NONE, "PAL Chip"), model));

        Vars.sixNodeItem.addDescriptor(8 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "Schmitt Trigger Chip"), model, "SCHMITT",
                SchmittTrigger.class));

        Vars.sixNodeItem.addDescriptor(9 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "D Flip Flop Chip"), model, "DFF", DFlipFlop.class));

        Vars.sixNodeItem.addDescriptor(10 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "Oscillator Chip"), model, "OSC", Oscillator.class));

        Vars.sixNodeItem.addDescriptor(11 + (id << 6),
            new LogicGateDescriptor(TR_NAME(I18N.Type.NONE, "JK Flip Flop Chip"), model, "JKFF", JKFlipFlop.class));
    }

    private void registerAnalogChips(int id) {
        id <<= 6;

        Obj3D model = Vars.obj.getObj("AnalogChips");
        Vars.sixNodeItem.addDescriptor(id + 0,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "OpAmp"), model, "OP", OpAmp.class));

        Vars.sixNodeItem.addDescriptor(id + 1, new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "PID Regulator"), model, "PID",
            PIDRegulator.class, PIDRegulatorElement.class, PIDRegulatorRender.class));

        Vars.sixNodeItem.addDescriptor(id + 2,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Voltage controlled sawtooth oscillator"), model, "VCO-SAW",
                VoltageControlledSawtoothOscillator.class));

        Vars.sixNodeItem.addDescriptor(id + 3,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Voltage controlled sine oscillator"), model, "VCO-SIN",
                VoltageControlledSineOscillator.class));

        Vars.sixNodeItem.addDescriptor(id + 4,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Amplifier"), model, "AMP",
                Amplifier.class, AmplifierElement.class, AmplifierRender.class));

        Vars.sixNodeItem.addDescriptor(id + 5,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Voltage controlled amplifier"), model, "VCA",
                VoltageControlledAmplifier.class));

        Vars.sixNodeItem.addDescriptor(id + 6,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Configurable summing unit"), model, "SUM",
                SummingUnit.class, SummingUnitElement.class, SummingUnitRender.class));

        Vars.sixNodeItem.addDescriptor(id + 7,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Sample and hold"), model, "SAH",
                SampleAndHold.class));

        Vars.sixNodeItem.addDescriptor(id + 8,
            new AnalogChipDescriptor(TR_NAME(I18N.Type.NONE, "Lowpass filter"), model, "LPF",
                Filter.class, FilterElement.class, FilterRender.class));
    }

}

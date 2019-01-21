package mods.eln.registration;

import mods.eln.Vars;
import mods.eln.ghost.GhostGroup;
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor;
import mods.eln.gridnode.transformer.GridTransformerDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.mechanical.*;
import mods.eln.misc.Coordonate;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.Utils;
import mods.eln.misc.series.SerieEE;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.sound.SoundCommand;
import mods.eln.transparentnode.FuelGeneratorDescriptor;
import mods.eln.transparentnode.FuelHeatFurnaceDescriptor;
import mods.eln.transparentnode.autominer.AutoMinerDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor;
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.*;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor;
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor;
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor;
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterDescriptor;
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.transparentnode.transformer.TransformerDescriptor;
import mods.eln.transparentnode.turbine.TurbineDescriptor;
import mods.eln.transparentnode.turret.TurretDescriptor;
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor;
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import static mods.eln.i18n.I18N.TR_NAME;

public class TransparentNodeRegistration {

    public TransparentNodeRegistration() {

    }

    public void registerTransparentNode() {
        //TRANSPARENT NODE REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerPowerComponent(1);
        registerTransformer(2);
        registerHeatFurnace(3);
        registerTurbine(4);
        registerElectricalAntenna(7);
        registerBattery(16);
        registerElectricalFurnace(32);
        registerMacerator(33);
        registerArcFurnace(34);
        registerCompressor(35);
        registerMagnetizer(36);
        registerPlateMachine(37);
        registerEggIncubator(41);
        registerAutoMiner(42);
        registerSolarPanel(48);
        registerWindTurbine(49);
        registerThermalDissipatorPassiveAndActive(64);
        registerTransparentNodeMisc(65);
        registerTurret(66);
        registerFuelGenerator(67);
        registerGridDevices(123);
        registerGridDevices(123);
    }

    private void registerGridDevices(int id) {
        int subId;
        {
//          subId = 2;
//			DownlinkDescriptor descriptor =
//					new DownlinkDescriptor("Downlink", Vars.obj.getObj("DownLink"), "textures/wire.png", Vars.highVoltageCableDescriptor);
//			Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 3;
            GridTransformerDescriptor descriptor =
                new GridTransformerDescriptor("Grid DC-DC Converter", Vars.obj.getObj("GridConverter"), "textures/wire.png", Vars.highVoltageCableDescriptor);
            GhostGroup g = new GhostGroup();
            g.addElement(1, 0, 0);
            g.addElement(0, 0, -1);
            g.addElement(1, 0, -1);
            g.addElement(1, 1, 0);
            g.addElement(0, 1, 0);
            g.addElement(1, 1, -1);
            g.addElement(0, 1, -1);
            descriptor.setGhostGroup(g);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 4;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor(
                    "Utility Pole",
                    Vars.obj.getObj("UtilityPole"),
                    "textures/wire.png",
                    Vars.highVoltageCableDescriptor,
                    false,
                    24,
                    12800);
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, 0);
            g.addElement(0, 3, 0);
            //g.addRectangle(-1, 1, 3, 4, -1, 1);
            descriptor.setGhostGroup(g);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 5;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor(
                    "Utility Pole w/DC-DC Converter",
                    Vars.obj.getObj("UtilityPole"),
                    "textures/wire.png",
                    Vars.highVoltageCableDescriptor,
                    true,
                    24,
                    12800);
            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, 0);
            g.addElement(0, 3, 0);
            //g.addRectangle(-1, 1, 3, 4, -1, 1);
            descriptor.setGhostGroup(g);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 6;
            ElectricalPoleDescriptor descriptor =
                new ElectricalPoleDescriptor("Transmission Tower",
                    Vars.obj.getObj("TransmissionTower"),
                    "textures/wire.png",
                    Vars.highVoltageCableDescriptor,
                    false,
                    96,
                    51200);
            GhostGroup g = new GhostGroup();
            g.addRectangle(-1, 1, 0, 0, -1, 1);
            g.addRectangle(0, 0, 1, 8, 0, 0);
            g.removeElement(0, 0, 0);
            descriptor.setGhostGroup(g);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 7;
            // Reserved for T2.5 poles.
        }
    }

    private void registerPowerComponent(int id) {
        int subId;
        String name;

        {
            subId = 16;

            name = TR_NAME(I18N.Type.NONE, "Power inductor");

            PowerInductorDescriptor desc = new PowerInductorDescriptor(
                name, null, SerieEE.newE12(-1)
            );

            Vars.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 20;

            name = TR_NAME(I18N.Type.NONE, "Power capacitor");

            PowerCapacitorDescriptor desc = new PowerCapacitorDescriptor(
                name, null, SerieEE.newE6(-2), 300
            );

            Vars.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    private void registerTransformer(int id) {
        int subId;
        String name;

        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "DC-DC Converter");

            TransformerDescriptor desc = new TransformerDescriptor(name, Vars.obj.getObj("transformator"),
                Vars.obj.getObj("feromagneticcorea"), Vars.obj.getObj("transformatorCase"), 0.5f);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerHeatFurnace(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Stone Heat Furnace");

            HeatFurnaceDescriptor desc = new HeatFurnaceDescriptor(name,
                "stonefurnace", 4000,
                Utils.getCoalEnergyReference() * 2 / 3,// double
                // nominalPower,
                // double
                // nominalCombustibleEnergy,
                8, 500,// int combustionChamberMax,double
                // combustionChamberPower,
                new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2) // thermal
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Fuel Heat Furnace");

            FuelHeatFurnaceDescriptor desc = new FuelHeatFurnaceDescriptor(name,
                Vars.obj.getObj("FuelHeater"), new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2));
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerTurbine(int id) {
        int subId;
        String name;

        FunctionTable TtoU = new FunctionTable(new double[]{0, 0.1, 0.85,
            1.0, 1.1, 1.15, 1.18, 1.19, 1.25}, 8.0 / 5.0);
        FunctionTable PoutToPin = new FunctionTable(new double[]{0.0, 0.2,
            0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7}, 8.0 / 5.0);

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "50V Turbine");
            double RsFactor = 0.1;
            double nominalU = Vars.LVU;
            double nominalP = 1000 * Vars.heatTurbinePowerFactor; // it was 300 before
            double nominalDeltaT = 250;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbineb", Vars.lowVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, Vars.lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v");
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "200V Turbine");
            double RsFactor = 0.10;
            double nominalU = Vars.MVU;
            double nominalP = 2000 * Vars.heatTurbinePowerFactor;
            double nominalDeltaT = 350;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbinebblue", Vars.meduimVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, Vars.meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v");
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 9;
            SteamTurbineDescriptor desc = new SteamTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Steam Turbine"),
                Vars.obj.getObj("Turbine")
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 10;
            float nominalRads = 800, nominalU = 3200;
            float nominalP = 4000;
            GeneratorDescriptor desc = new GeneratorDescriptor(
                TR_NAME(I18N.Type.NONE, "Generator"),
                Vars.obj.getObj("Generator"),
                Vars.highVoltageCableDescriptor,
                nominalRads, nominalU,
                nominalP / (nominalU / 25),
                nominalP,
                Vars.sixNodeThermalLoadInitializer.copy()
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 11;
            GasTurbineDescriptor desc = new GasTurbineDescriptor(
                TR_NAME(I18N.Type.NONE, "Gas Turbine"),
                Vars.obj.getObj("GasTurbine")
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);

        }

        {
            subId = 12;

            StraightJointDescriptor desc = new StraightJointDescriptor(
                TR_NAME(I18N.Type.NONE, "Joint"),
                Vars.obj.getObj("StraightJoint"));
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 13;

            VerticalHubDescriptor desc = new VerticalHubDescriptor(
                TR_NAME(I18N.Type.NONE, "Joint hub"),
                Vars.obj.getObj("VerticalHub"));
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 14;

            FlywheelDescriptor desc = new FlywheelDescriptor(
                TR_NAME(I18N.Type.NONE, "Flywheel"),
                Vars.obj.getObj("Flywheel"));
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 15;

            TachometerDescriptor desc = new TachometerDescriptor(
                TR_NAME(I18N.Type.NONE, "Tachometer"),
                Vars.obj.getObj("Tachometer"));
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 16;

            float nominalRads = 800, nominalU = 3200;
            float nominalP = 1200;

            MotorDescriptor desc = new MotorDescriptor(
                TR_NAME(I18N.Type.NONE, "Shaft Motor"),
                Vars.obj.getObj("Motor"),
                Vars.veryHighVoltageCableDescriptor,
                nominalRads,
                nominalU,
                nominalP,
                25.0f * nominalP / nominalU,
                25.0f * nominalP / nominalU,
                Vars.sixNodeThermalLoadInitializer.copy()
            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 17;
            ClutchDescriptor desc = new ClutchDescriptor(
                TR_NAME(I18N.Type.NONE, "Clutch"),
                Vars.obj.getObj("Clutch")
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 18;
            FixedShaftDescriptor desc = new FixedShaftDescriptor(
                TR_NAME(I18N.Type.NONE, "Fixed Shaft"),
                Vars.obj.getObj("FixedShaft")
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerElectricalAntenna(int id) {
        int subId;
        String name;
        {

            subId = 0;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Low Power Transmitter Antenna");
            double P = 250;
            desc = new ElectricalAntennaTxDescriptor(name,
                Vars.obj.getObj("lowpowertransmitterantenna"), 200,// int
                // rangeMax,
                0.9, 0.7,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                Vars.LVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                Vars.LVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                Vars.lowVoltageCableDescriptor);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 1;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Low Power Receiver Antenna");
            double P = 250;
            desc = new ElectricalAntennaRxDescriptor(name,
                Vars.obj.getObj("lowpowerreceiverantenna"), Vars.LVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                Vars.LVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                Vars.lowVoltageCableDescriptor);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {

            subId = 2;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Medium Power Transmitter Antenna");
            double P = 1000;
            desc = new ElectricalAntennaTxDescriptor(name,
                Vars.obj.getObj("lowpowertransmitterantenna"), 250,// int
                // rangeMax,
                0.9, 0.75,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                Vars.MVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                Vars.MVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                Vars.meduimVoltageCableDescriptor);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 3;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "Medium Power Receiver Antenna");
            double P = 1000;
            desc = new ElectricalAntennaRxDescriptor(name,
                Vars.obj.getObj("lowpowerreceiverantenna"), Vars.MVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                Vars.MVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                Vars.meduimVoltageCableDescriptor);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {

            subId = 4;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "High Power Transmitter Antenna");
            double P = 2000;
            desc = new ElectricalAntennaTxDescriptor(name,
                Vars.obj.getObj("lowpowertransmitterantenna"), 300,// int
                // rangeMax,
                0.95, 0.8,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                Vars.HVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                Vars.HVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                Vars.highVoltageCableDescriptor);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 5;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(I18N.Type.NONE, "High Power Receiver Antenna");
            double P = 2000;
            desc = new ElectricalAntennaRxDescriptor(name,
                Vars.obj.getObj("lowpowerreceiverantenna"), Vars.HVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                Vars.HVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                Vars.highVoltageCableDescriptor);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerBattery(int id) {
        int subId;
        String name;
        double heatTIme = 30;
        double[] voltageFunctionTable = {0.000, 0.9, 1.0, 1.025, 1.04, 1.05,
            2.0};
        FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,
            6.0 / 5);
        double[] condoVoltageFunctionTable = {0.000, 0.89, 0.90, 0.905, 0.91, 1.1,
            1.5};
        FunctionTable condoVoltageFunction = new FunctionTable(condoVoltageFunctionTable,
            6.0 / 5);

        Utils.printFunction(voltageFunction, -0.2, 1.2, 0.1);

        double stdDischargeTime = 60 * 16;
        double stdU = Vars.LVU;
        double stdP = Vars.LVP / 4 / Vars.cablePace; //you need 4 to support a full cable
        double stdEfficiency = 1.0 - 2.0 / 50.0; //96%
        double condoEfficiency = 1.0 - 2.0 / 50.0; //96%

        Vars.batteryVoltageFunctionTable = voltageFunction;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Cost Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name, "BatteryBig", Vars.batteryCableDescriptor,
                0.5, //what % of charge it starts out with
                true, true,  //is rechargable?, Uses Life Mechanic?
                voltageFunction,
                stdU, //battery nominal voltage
                stdP * 1.2, //how much power it can handle at max,
                0.00,  //precentage of its total output to self-discharge. Should probably be 0
                stdP, //no idea
                stdDischargeTime * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife,

                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit, // thermalCoolLimit,
                "Cheap battery" // name, description)
            );
            desc.setRenderSpec("lowcost");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Capacity Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", Vars.batteryCableDescriptor, 0.5, true, true, voltageFunction,
                stdU / 4, stdP / 2 * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP / 2, stdDischargeTime * 8 * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("capacity");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Voltage Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", Vars.meduimVoltageCableDescriptor, 0.5, true, true, voltageFunction, stdU * 4,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("highvoltage");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Current Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", Vars.batteryCableDescriptor, 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2 * 4, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 4, stdDischargeTime / 6 * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("current");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Life Oriented Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", Vars.batteryCableDescriptor, 0.5, true, false, voltageFunction, stdU,
                stdP * 1.2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP, stdDischargeTime * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("life");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 5;
            name = TR_NAME(I18N.Type.NONE, "Single-use Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", Vars.batteryCableDescriptor, 1.0, false, false, voltageFunction, stdU,
                stdP * 1.2 * 2, 0.000, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 2, stdDischargeTime / 4 * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setRenderSpec("coal");
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Experimental Battery");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "BatteryBig", Vars.batteryCableDescriptor, 0.5, true, false, voltageFunction, stdU * 2,
                stdP * 1.2 * 8, 0.025, // electricalU,
                // electricalPMax,electricalDischargeRate
                stdP * 8, stdDischargeTime / 4 * Vars.batteryCapacityFactor, stdEfficiency, Vars.stdBatteryHalfLife * 8, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "You were unable to fix the power leaking problem, though." // name, description)
            );
            desc.setRenderSpec("highvoltage");
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        /*{
            subId = 32;
            name = TR_NAME(Type.NONE, "50V Condensator");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "condo200", batteryCableDescriptor, 0.0, true, false,
                condoVoltageFunction,
                stdU, stdP * 1.2 * 8, 0.005, // electricalU,//
                // electricalPMax,electricalDischargeRate
                stdP * 8, 4, condoEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "Obselete, must be deleted" // name, description)
            );
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
            desc.setDefaultIcon("empty-texture");
            Vars.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 36;
            name = TR_NAME(I18N.Type.NONE, "200V Condensator");

            BatteryDescriptor desc = new BatteryDescriptor(name,
                "condo200", Vars.highVoltageCableDescriptor, 0.0, true, false,
                condoVoltageFunction,
                Vars.MVU, MVP * 1.5, 0.005, // electricalU,//
                // electricalPMax,electricalDischargeRate
                MVP, 4, condoEfficiency, stdBatteryHalfLife, // electricalStdP,
                // electricalStdDischargeTime,
                // electricalStdEfficiency,
                // electricalStdHalfLife,
                heatTIme, 60, -100, // thermalHeatTime, thermalWarmLimit,
                // thermalCoolLimit,
                "the battery" // name, description)
            );
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 2.0);
            desc.setDefaultIcon("empty-texture");
            Vars.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        } */
    }

    private void registerElectricalFurnace(int id) {
        int subId;
        String name;
        Vars.furnaceList.add(new ItemStack(Blocks.furnace));
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Electrical Furnace");
            double[] PfTTable = new double[]{0, 20, 40, 80, 160, 240, 360,
                540, 756, 1058.4, 1481.76};

            double[] thermalPlostfTTable = new double[PfTTable.length];
            for (int idx = 0; idx < thermalPlostfTTable.length; idx++) {
                thermalPlostfTTable[idx] = PfTTable[idx]
                    * Math.pow((idx + 1.0) / thermalPlostfTTable.length, 2)
                    * 2;
            }

            FunctionTableYProtect PfT = new FunctionTableYProtect(PfTTable,
                800.0, 0, 100000.0);

            FunctionTableYProtect thermalPlostfT = new FunctionTableYProtect(
                thermalPlostfTTable, 800.0, 0.001, 10000000.0);

            ElectricalFurnaceDescriptor desc = new ElectricalFurnaceDescriptor(
                name, PfT, thermalPlostfT,// thermalPlostfT;
                40// thermalC;
            );
            Vars.electricalFurnace = desc;
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            Vars.furnaceList.add(desc.newItemStack());

            // Utils.smeltRecipeList.addMachine(desc.newItemStack());
        }
        // Utils.smeltRecipeList.addMachine(new ItemStack(Blocks.furnace));
    }

    private void registerMacerator(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Macerator");

            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratora", Vars.LVU, 200,// double nominalU,double nominalP,
                Vars.LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                Vars.maceratorRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Macerator");

            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratorb", Vars.MVU, 400,// double nominalU,double nominalP,
                Vars.MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                Vars.maceratorRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }
    }

    private void registerArcFurnace(int id) {

        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "800V Arc Furnace");

            ArcFurnaceDescriptor desc = new ArcFurnaceDescriptor(
                name,// String name,
                Vars.obj.getObj("arcfurnace"),
                Vars.HVU, 10000,// double nominalU,double nominalP,
                Vars.HVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.highVoltageCableDescriptor,// ElectricalCableDescriptor cable
                Vars.arcFurnaceRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:arc_furnace");

        }
    }

    private void registerCompressor(int id) {

        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Compressor");

            CompressorDescriptor desc = new CompressorDescriptor(
                name,// String name,
                Vars.obj.getObj("compressora"),
                Vars.LVU, 200,// double nominalU,double nominalP,
                Vars.LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                Vars.compressorRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Compressor");

            CompressorDescriptor desc = new CompressorDescriptor(
                name,// String name,
                Vars.obj.getObj("compressorb"),
                Vars.MVU, 400,// double nominalU,double nominalP,
                Vars.MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                Vars.compressorRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }
    }
    private void registerMagnetizer(int id) {

        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Magnetizer");

            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,// String name,
                Vars.obj.getObj("magnetizera"),
                Vars.LVU, 200,// double nominalU,double nominalP,
                Vars.LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                Vars.magnetiserRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:Motor");
        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Magnetizer");

            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,// String name,
                Vars.obj.getObj("magnetizerb"),
                Vars.MVU, 400,// double nominalU,double nominalP,
                Vars.MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                Vars.magnetiserRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:Motor");
        }
    }

    private void registerPlateMachine(int id) {

        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Plate Machine");

            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,// String name,
                Vars.obj.getObj("platemachinea"),
                Vars.LVU, 200,// double nominalU,double nominalP,
                Vars.LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                Vars.plateMachineRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");

        }

        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "200V Plate Machine");

            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,// String name,
                Vars.obj.getObj("platemachineb"),
                Vars.MVU, 400,// double nominalU,double nominalP,
                Vars.MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                Vars.meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                Vars.plateMachineRecipes);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");

        }
    }

    private void registerEggIncubator(int id) {

        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "50V Egg Incubator");

            EggIncubatorDescriptor desc = new EggIncubatorDescriptor(
                name, Vars.obj.getObj("eggincubator"),
                Vars.lowVoltageCableDescriptor,
                Vars.LVU, 50);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerAutoMiner(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Auto Miner");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-2, -1, 1, 0);
            powerLoad[1] = new Coordonate(-2, -1, -1, 0);

            Coordonate lightCoord = new Coordonate(-3, 0, 0, 0);

            Coordonate miningCoord = new Coordonate(-1, 0, 1, 0);

            AutoMinerDescriptor desc = new AutoMinerDescriptor(name,
                Vars.obj.getObj("AutoMiner"),
                powerLoad, lightCoord, miningCoord,
                2, 1, 0,
                Vars.highVoltageCableDescriptor,
                1, 50// double pipeRemoveTime,double pipeRemoveEnergy
            );

            GhostGroup ghostGroup = new GhostGroup();

            ghostGroup.addRectangle(-2, -1, -1, 0, -1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, 1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, -1, -1);
            ghostGroup.addElement(1, 0, 0);
            ghostGroup.addElement(0, 0, 1);
            ghostGroup.addElement(0, 1, 0);
            ghostGroup.addElement(0, 0, -1);
            ghostGroup.removeElement(-1, -1, 0);

            desc.setGhostGroup(ghostGroup);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerSolarPanel(int id) {
        int subId;
        GhostGroup ghostGroup;
        String name;

        FunctionTable diodeIfUBase;
        diodeIfUBase = new FunctionTableYProtect(new double[]{0.0, 0.002,
            0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.035, 0.04, 0.045,
            0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 1.0},
            1.0, 0, 1.0);

        FunctionTable solarIfSBase;
        solarIfSBase = new FunctionTable(new double[]{0.0, 0.1, 0.4, 0.6,
            0.8, 1.0}, 1);

        double LVSolarU = 59;

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Small Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                Vars.obj.getObj("smallsolarpannel"), null,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, 65.0 * Vars.solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 2, Math.PI / 2 // alphaMin alphaMax
            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Small Rotating Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                Vars.obj.getObj("smallsolarpannelrot"), Vars.lowVoltageCableDescriptor.render,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, Vars.solarPanelBasePower * Vars.solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 4, Math.PI / 4 * 3 // alphaMin alphaMax
            );
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "2x3 Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                Vars.obj.getObj("bigSolarPanel"), Vars.meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 0,
                groundCoordinate,
                LVSolarU * 2, Vars.solarPanelBasePower * Vars.solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 2, Math.PI / 2
            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "2x3 Rotating Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                Vars.obj.getObj("bigSolarPanelrot"), Vars.meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 1,
                groundCoordinate,
                LVSolarU * 2, Vars.solarPanelBasePower * Vars.solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 8 * 3, Math.PI / 8 * 5
            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerWindTurbine(int id) {
        int subId;
        String name;

        FunctionTable PfW = new FunctionTable(
            new double[]{0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2},
            8.0 / 5.0);
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Wind Turbine");

            WindTurbineDescriptor desc = new WindTurbineDescriptor(
                name, Vars.obj.getObj("WindTurbineMini"), // name,Vars.obj3D Vars.obj,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                PfW,// PfW
                160 * Vars.windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
                Vars.LVU * 1.18, 22,// double maxVoltage, double maxWind,
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
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        /*{ //TODO Work on the large wind turbine
            subId = 1;
            name = TR_NAME(Type.NONE, "Large Wind Turbine");

            WindTurbineDescriptor desc = new WindTurbineDescriptor(
                name, Vars.obj.getObj("WindTurbineMini"), // name,Vars.obj3D Vars.obj,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                PfW,// PfW
                160 * windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
                Vars.LVU * 1.18, 22,// double maxVoltage, double maxWind,
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
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        } */

        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Water Turbine");

            Coordonate waterCoord = new Coordonate(1, -1, 0, 0);

            WaterTurbineDescriptor desc = new WaterTurbineDescriptor(
                name, Vars.obj.getObj("SmallWaterWheel"), // name,Vars.obj3D Vars.obj,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor
                30 * Vars.waterTurbinePowerFactor,
                Vars.LVU * 1.18,
                waterCoord,
                "eln:water_turbine", 1f
            );

            GhostGroup g = new GhostGroup();

            g.addRectangle(1, 1, 0, 1, -1, 1);
            desc.setGhostGroup(g);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    private void registerThermalDissipatorPassiveAndActive(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Small Passive Thermal Dissipator");

            ThermalDissipatorPassiveDescriptor desc = new ThermalDissipatorPassiveDescriptor(
                name,
                Vars.obj.getObj("passivethermaldissipatora"),
                200, -100,// double warmLimit,double coolLimit,
                250, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "Small Active Thermal Dissipator");

            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                Vars.obj.getObj("activethermaldissipatora"),
                Vars.LVU, 50,// double nominalElectricalU,double
                // electricalNominalP,
                800,// double nominalElectricalCoolingPower,
                Vars.lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cableDescriptor,
                130, -100,// double warmLimit,double coolLimit,
                200, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 34;
            name = TR_NAME(I18N.Type.NONE, "200V Active Thermal Dissipator");

            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                Vars.obj.getObj("200vactivethermaldissipatora"),
                Vars.MVU, 60,// double nominalElectricalU,double
                // electricalNominalP,
                1200,// double nominalElectricalCoolingPower,
                Vars.meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cableDescriptor,
                130, -100,// double warmLimit,double coolLimit,
                200, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerTransparentNodeMisc(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Experimental Transporter");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-1, 0, 1, 0);
            powerLoad[1] = new Coordonate(-1, 0, -1, 0);

            GhostGroup doorOpen = new GhostGroup();
            doorOpen.addRectangle(-4, -3, 2, 2, 0, 0);

            GhostGroup doorClose = new GhostGroup();
            doorClose.addRectangle(-2, -2, 0, 1, 0, 0);

            TeleporterDescriptor desc = new TeleporterDescriptor(
                name, Vars.obj.getObj("Transporter"),
                Vars.highVoltageCableDescriptor,
                new Coordonate(-1, 0, 0, 0), new Coordonate(-1, 1, 0, 0),
                2,// int areaH
                powerLoad,
                doorOpen, doorClose

            );
            desc.setChargeSound("eln:transporter", 0.5f);
            GhostGroup g = new GhostGroup();
            g.addRectangle(-2, 0, 0, 1, -1, -1);
            g.addRectangle(-2, 0, 0, 1, 1, 1);
            g.addRectangle(-4, -1, 2, 2, 0, 0);
            g.addElement(0, 1, 0);
            //g.addElement(0, 2, 0);
            g.addElement(-1, 0, 0, Vars.ghostBlock, Vars.ghostBlock.tFloor);
		/*	g.addElement(1, 0, 0,ghostBlock,ghostBlock.tLadder);
			g.addElement(1, 1, 0,ghostBlock,ghostBlock.tLadder);
			g.addElement(1, 2, 0,ghostBlock,ghostBlock.tLadder);*/
            g.addRectangle(-3, -3, 0, 1, -1, -1);
            g.addRectangle(-3, -3, 0, 1, 1, 1);
            // g.addElement(-4, 0, -1);
            // g.addElement(-4, 0, 1);

            desc.setGhostGroup(g);

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

		/*if (Other.ccLoaded && ComputerProbeEnable) {
			subId = 4;
			name = "ComputerCraft Probe";

			ComputerCraftIoDescriptor desc = new ComputerCraftIoDescriptor(
					name,
					Vars.obj.getObj("passivethermaldissipatora")

					);

			Vars.transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}*/

    }

    private void registerTurret(int id) {
        {
            int subId = 0;
            String name = TR_NAME(I18N.Type.NONE, "800V Defence Turret");

            TurretDescriptor desc = new TurretDescriptor(name, "Turret");

            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    private void registerFuelGenerator(int id) {
        int subId;
        {
            subId = 1;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(I18N.Type.NONE, "50V Fuel Generator"), Vars.obj.getObj("FuelGenerator50V"),
                    Vars.lowVoltageCableDescriptor, Vars.fuelGeneratorPowerFactor * 1200, Vars.LVU * 1.25, Vars.fuelGeneratorTankCapacity);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 2;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(I18N.Type.NONE, "200V Fuel Generator"), Vars.obj.getObj("FuelGenerator200V"),
                    Vars.meduimVoltageCableDescriptor, Vars.fuelGeneratorPowerFactor * 6000, Vars.MVU * 1.25,
                    Vars.fuelGeneratorTankCapacity);
            Vars.transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }
}

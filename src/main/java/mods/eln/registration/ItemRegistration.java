package mods.eln.registration;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.generic.genericArmorItem;
import mods.eln.i18n.I18N;
import mods.eln.item.*;
import mods.eln.item.electricalitem.*;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.RegulatorAnalogDescriptor;
import mods.eln.item.regulator.RegulatorOnOffDescriptor;
import mods.eln.mechanical.ClutchPinItem;
import mods.eln.mechanical.ClutchPlateItem;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketType;
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraftforge.oredict.OreDictionary;

import static mods.eln.i18n.I18N.TR;
import static mods.eln.i18n.I18N.TR_NAME;

public class ItemRegistration {

    public ItemRegistration() {

    }

    public void registerItems() {
        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerHeatingCorp(1);
        // registerThermalIsolator(2);
        registerRegulatorItem(3);
        registerLampItem(4);
        registerProtection(5);
        registerCombustionChamber(6);
        registerFerromagneticCore(7);
        registerIngot(8);
        registerDust(9);
        registerElectricalMotor(10);
        registerSolarTracker(11);
        //
        registerMeter(14);
        registerElectricalDrill(15);
        registerOreScanner(16);
        registerMiningPipe(17);
        registerTreeResinAndRubber(64);
        registerRawCable(65);
        registerArc(69);
        registerBrush(119);
        registerMiscItem(120);
        registerElectricalTool(121);
        registerPortableItem(122);
        registerFuelBurnerItem(124);

        registerArmor();
        registerTool();
    }

    private void registerHeatingCorp(int id) {
        int subId, completId;

        HeatingCorpElement element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 50V Copper Heating Corp"),// iconId,
                // name,
                Vars.LVU, 150,// electricalNominalU, electricalNominalP,
                190,// electricalMaximalP)
                Vars.lowVoltageCableDescriptor// ElectricalCableDescriptor
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "50V Copper Heating Corp"),// iconId,
                // name,
                Vars.LVU, 250,// electricalNominalU, electricalNominalP,
                320,// electricalMaximalP)
                Vars.lowVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 200V Copper Heating Corp"),// iconId,
                // name,
                Vars.MVU, 400,// electricalNominalU, electricalNominalP,
                500,// electricalMaximalP)
                Vars.meduimVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 3;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "200V Copper Heating Corp"),// iconId,
                // name,
                Vars.MVU, 600,// electricalNominalU, electricalNominalP,
                750,// electricalMaximalP)
                Vars.highVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 4;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 50V Iron Heating Corp"),// iconId,
                // name,
                Vars.LVU, 180,// electricalNominalU, electricalNominalP,
                225,// electricalMaximalP)
                Vars.lowVoltageCableDescriptor// ElectricalCableDescriptor
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 5;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "50V Iron Heating Corp"),// iconId,
                // name,
                Vars.LVU, 375,// electricalNominalU, electricalNominalP,
                480,// electricalMaximalP)
                Vars.lowVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 6;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 200V Iron Heating Corp"),// iconId,
                // name,
                Vars.MVU, 600,// electricalNominalU, electricalNominalP,
                750,// electricalMaximalP)
                Vars.meduimVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 7;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "200V Iron Heating Corp"),// iconId,
                // name,
                Vars.MVU, 900,// electricalNominalU, electricalNominalP,
                1050,// electricalMaximalP)
                Vars.highVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 8;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 50V Tungsten Heating Corp"),// iconId,
                // name,
                Vars.LVU, 240,// electricalNominalU, electricalNominalP,
                300,// electricalMaximalP)
                Vars.lowVoltageCableDescriptor// ElectricalCableDescriptor
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 9;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "50V Tungsten Heating Corp"),// iconId,
                // name,
                Vars.LVU, 500,// electricalNominalU, electricalNominalP,
                640,// electricalMaximalP)
                Vars.lowVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 10;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(
                TR_NAME(I18N.Type.NONE, "Small 200V Tungsten Heating Corp"),// iconId, name,
                Vars.MVU, 800,// electricalNominalU, electricalNominalP,
                1000,// electricalMaximalP)
                Vars.meduimVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 11;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "200V Tungsten Heating Corp"),// iconId,
                // name,
                Vars.MVU, 1200,// electricalNominalU, electricalNominalP,
                1500,// electricalMaximalP)
                Vars.highVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 12;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 800V Tungsten Heating Corp"),// iconId,
                // name,
                Vars.HVU, 3600,// electricalNominalU, electricalNominalP,
                4800,// electricalMaximalP)
                Vars.veryHighVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 13;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "800V Tungsten Heating Corp"),// iconId,
                // name,
                Vars.HVU, 4812,// electricalNominalU, electricalNominalP,
                6015,// electricalMaximalP)
                Vars.veryHighVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 14;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "Small 3.2kV Tungsten Heating Corp"),// iconId,
                // name,
                Vars.VVU, 4000,// electricalNominalU, electricalNominalP,
                6000,// electricalMaximalP)
                Vars.veryHighVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 15;
            completId = subId + (id << 6);
            element = new HeatingCorpElement(TR_NAME(I18N.Type.NONE, "3.2kV Tungsten Heating Corp"),// iconId,
                // name,
                Vars.VVU, 12000,// electricalNominalU, electricalNominalP,
                15000,// electricalMaximalP)
                Vars.veryHighVoltageCableDescriptor);
            Vars.sharedItem.addElement(completId, element);
        }

    }

    private void registerRegulatorItem(int id) {
        int subId, completId;
        IRegulatorDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new RegulatorOnOffDescriptor(TR_NAME(I18N.Type.NONE, "On/OFF Regulator 1 Percent"),
                "onoffregulator", 0.01);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new RegulatorOnOffDescriptor(TR_NAME(I18N.Type.NONE, "On/OFF Regulator 10 Percent"),
                "onoffregulator", 0.1);
            Vars.sharedItem.addElement(completId, element);
        }

        {
            subId = 8;
            completId = subId + (id << 6);
            element = new RegulatorAnalogDescriptor(TR_NAME(I18N.Type.NONE, "Analogic Regulator"),
                "Analogicregulator");
            Vars.sharedItem.addElement(completId, element);
        }

    }

    private void registerLampItem(int id) {
        int subId, completId;
        double[] lightPower = new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            15, 20, 25, 30, 40};
        double[] lightLevel = new double[16];
        double economicPowerFactor = 0.5;
        double standardGrowRate = 0.0;
        for (int idx = 0; idx < 16; idx++) {
            lightLevel[idx] = (idx + 0.49) / 15.0;
        }
        LampDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "Small 50V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.LVU, lightPower[12], // nominalU,
                // nominalP
                lightLevel[12], Vars.incandescentLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.LVU, lightPower[14], // nominalU,
                // nominalP
                lightLevel[14], Vars.incandescentLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V Incandescent Light Bulb"),
                "incandescentironlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.MVU, lightPower[14], // nominalU,
                // nominalP
                lightLevel[14], Vars.incandescentLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }

        {
            subId = 4;
            completId = subId + (id << 6);
            element = new LampDescriptor(
                TR_NAME(I18N.Type.NONE, "Small 50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.LVU, lightPower[11], // nominalU,
                // nominalP
                lightLevel[11], Vars.carbonLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 5;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.LVU, lightPower[13], // nominalU,
                // nominalP
                lightLevel[13], Vars.carbonLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }

        {
            subId = 16;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "Small 50V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, Vars.LVU, lightPower[12]
                * economicPowerFactor, // nominalU, nominalP
                lightLevel[12], Vars.economicLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 17;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, Vars.LVU, lightPower[14]
                * economicPowerFactor, // nominalU, nominalP
                lightLevel[14], Vars.economicLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 18;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V Economic Light Bulb"),
                "fluorescentlamp", LampDescriptor.Type.eco,
                LampSocketType.Douille, Vars.MVU, lightPower[14]
                * economicPowerFactor, // nominalU, nominalP
                lightLevel[14], Vars.economicLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }

        {
            subId = 32;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V Farming Lamp"),
                "farminglamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.LVU, 120, // nominalU, nominalP
                lightLevel[15], Vars.incandescentLampLife, 0.50 // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 36;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V Farming Lamp"),
                "farminglamp", LampDescriptor.Type.Incandescent,
                LampSocketType.Douille, Vars.MVU, 120, // nominalU, nominalP
                lightLevel[15], Vars.incandescentLampLife, 0.50 // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 37;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "50V LED Bulb"),
                "ledlamp", LampDescriptor.Type.LED,
                LampSocketType.Douille, Vars.LVU, lightPower[14] / 2, // nominalU, nominalP
                lightLevel[14], Vars.ledLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 38;
            completId = subId + (id << 6);
            element = new LampDescriptor(TR_NAME(I18N.Type.NONE, "200V LED Bulb"),
                "ledlamp", LampDescriptor.Type.LED,
                LampSocketType.Douille, Vars.MVU, lightPower[14] / 2, // nominalU, nominalP
                lightLevel[14], Vars.ledLampLife, standardGrowRate // nominalLight,
                // nominalLife
            );
            Vars.sharedItem.addElement(completId, element);
        }

    }

    private void registerProtection(int id) {
        int subId, completId;

        {
            OverHeatingProtectionDescriptor element;
            subId = 0;
            completId = subId + (id << 6);
            element = new OverHeatingProtectionDescriptor(
                TR_NAME(I18N.Type.NONE, "Overheating Protection"));
            Vars.sharedItem.addElement(completId, element);
        }
        {
            OverVoltageProtectionDescriptor element;
            subId = 1;
            completId = subId + (id << 6);
            element = new OverVoltageProtectionDescriptor(
                TR_NAME(I18N.Type.NONE, "Overvoltage Protection"));
            Vars.sharedItem.addElement(completId, element);
        }

    }

    private void registerCombustionChamber(int id) {
        int subId, completId;
        {
            CombustionChamber element;
            subId = 0;
            completId = subId + (id << 6);
            element = new CombustionChamber(TR_NAME(I18N.Type.NONE, "Combustion Chamber"));
            Vars.sharedItem.addElement(completId, element);
        }

    }

    private void registerFerromagneticCore(int id) {
        int subId, completId;

        FerromagneticCoreDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new FerromagneticCoreDescriptor(
                TR_NAME(I18N.Type.NONE, "Cheap Ferromagnetic Core"), Vars.obj.getObj("feromagneticcorea"),// iconId,
                // name,
                100);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new FerromagneticCoreDescriptor(
                TR_NAME(I18N.Type.NONE, "Average Ferromagnetic Core"), Vars.obj.getObj("feromagneticcorea"),// iconId,
                // name,
                50);
            Vars.sharedItem.addElement(completId, element);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new FerromagneticCoreDescriptor(
                TR_NAME(I18N.Type.NONE, "Optimal Ferromagnetic Core"), Vars.obj.getObj("feromagneticcorea"),// iconId,
                // name,
                1);
            Vars.sharedItem.addElement(completId, element);
        }
    }

    private void registerIngot(int id) {
        int subId, completId;
        String name;

        GenericItemUsingDamageDescriptorWithComment element;

        {
            subId = 1;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Copper Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));
            Vars.copperIngot = element;
            Data.addResource(element.newItemStack());
            Vars.addToOre("ingotCopper", element.newItemStack());
        }

        {
            subId = 4;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Lead Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));
            Vars.plumbIngot = element;
            Data.addResource(element.newItemStack());
            Vars.addToOre("ingotLead", element.newItemStack());

        }

        {
            subId = 5;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Tungsten Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));
            Vars.tungstenIngot = element;
            Data.addResource(element.newItemStack());
            Vars.addToOre(Vars.dictTungstenIngot, element.newItemStack());
        }

        {
            subId = 6;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Ferrite Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{"useless", "Really useless"});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));

            Data.addResource(element.newItemStack());
            Vars.addToOre("ingotFerrite", element.newItemStack());
        }

        {
            subId = 7;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Alloy Ingot");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));

            Data.addResource(element.newItemStack());
            Vars.addToOre("ingotAlloy", element.newItemStack());
        }

        {
            subId = 8;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Mercury");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{"useless", "miaou"});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));

            Data.addResource(element.newItemStack());
            Vars.addToOre("quicksilver", element.newItemStack());
        }
    }

    private void registerDust(int id) {
        int subId, completId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;

        {
            subId = 1;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Copper Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.dustCopper = element;
            Vars.sharedItem.addElement(completId, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustCopper", element.newItemStack());
        }
        {
            subId = 2;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Iron Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.dustCopper = element;
            Vars.sharedItem.addElement(completId, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustIron", element.newItemStack());
        }
        {
            subId = 3;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Lapis Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.dustCopper = element;
            Vars.sharedItem.addElement(completId, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustLapis", element.newItemStack());
        }
        {
            subId = 4;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Diamond Dust");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.dustCopper = element;
            Vars.sharedItem.addElement(completId, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustDiamond", element.newItemStack());
        }

        {
            id = 5;

            name = TR_NAME(I18N.Type.NONE, "Lead Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustLead", element.newItemStack());
        }
        {
            id = 6;

            name = TR_NAME(I18N.Type.NONE, "Tungsten Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre(Vars.dictTungstenDust, element.newItemStack());
        }

        {
            id = 7;

            name = TR_NAME(I18N.Type.NONE, "Gold Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustGold", element.newItemStack());
        }

        {
            id = 8;

            name = TR_NAME(I18N.Type.NONE, "Coal Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustCoal", element.newItemStack());
        }
        {
            id = 9;

            name = TR_NAME(I18N.Type.NONE, "Alloy Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustAlloy", element.newItemStack());
        }

        {
            id = 10;

            name = TR_NAME(I18N.Type.NONE, "Cinnabar Dust");

            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(id, element);
            Data.addResource(element.newItemStack());
            Vars.addToOre("dustCinnabar", element.newItemStack());
        }

    }

    private void registerElectricalMotor(int id) {

        int subId, completId;
        String name;
        GenericItemUsingDamageDescriptorWithComment element;

        {
            subId = 0;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Electrical Motor");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));

            Data.addResource(element.newItemStack());

        }
        {
            subId = 1;
            completId = subId + (id << 6);

            name = TR_NAME(I18N.Type.NONE, "Advanced Electrical Motor");
            element = new GenericItemUsingDamageDescriptorWithComment(name,// iconId,
                // name,
                new String[]{});
            Vars.sharedItem.addElement(completId, element);
            // GameRegistry.registerCustomItemStack(name,
            // element.newItemStack(1));
            Data.addResource(element.newItemStack());

        }

    }

    private void registerSolarTracker(int id) {
        int subId, completId;

        SolarTrackerDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new SolarTrackerDescriptor(TR_NAME(I18N.Type.NONE, "Solar Tracker") // iconId, name,

            );
            Vars.sharedItem.addElement(completId, element);
        }

    }

    private void registerMeter(int id) {
        int subId, completId;

        GenericItemUsingDamageDescriptor element;
        {
            subId = 0;
            completId = subId + (id << 6);
            element = new GenericItemUsingDamageDescriptor(TR_NAME(I18N.Type.NONE, "MultiMeter"));
            Vars.sharedItem.addElement(completId, element);
            Vars.multiMeterElement = element;
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            element = new GenericItemUsingDamageDescriptor(TR_NAME(I18N.Type.NONE, "Thermometer"));
            Vars.sharedItem.addElement(completId, element);
            Vars.thermometerElement = element;
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            element = new GenericItemUsingDamageDescriptor(TR_NAME(I18N.Type.NONE, "AllMeter"));
            Vars.sharedItem.addElement(completId, element);
            Vars.allMeterElement = element;
        }
        {
            subId = 8;
            completId = subId + (id << 6);
            element = new WirelessSignalAnalyserItemDescriptor(TR_NAME(I18N.Type.NONE, "Wireless Analyser"));
            Vars.sharedItem.addElement(completId, element);

        }
        {
            subId = 16;
            completId = subId + (id << 6);
            element = new ConfigCopyToolDescriptor(TR_NAME(I18N.Type.NONE, "Config Copy Tool"));
            Vars.sharedItem.addElement(completId, element);
            Vars.configCopyToolElement = element;
        }

    }

    private void registerElectricalDrill(int id) {
        int subId, completId;
        String name;

        ElectricalDrillDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Cheap Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                8, 4000 // double operationTime,double operationEnergy
            );
            Vars.sharedItem.addElement(completId, descriptor);
        }
        {
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Average Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                5, 5000 // double operationTime,double operationEnergy
            );
            Vars.sharedItem.addElement(completId, descriptor);
        }
        {
            subId = 2;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Fast Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                3, 6000 // double operationTime,double operationEnergy
            );
            Vars.sharedItem.addElement(completId, descriptor);
        }
        {
            subId = 3;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Turbo Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                1, 10000 // double operationTime,double operationEnergy
            );
            Vars.sharedItem.addElement(completId, descriptor);
        }
        {
            subId = 4;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Irresponsible Electrical Drill");

            descriptor = new ElectricalDrillDescriptor(name,// iconId, name,
                0.1, 20000 // double operationTime,double operationEnergy
            );
            Vars.sharedItem.addElement(completId, descriptor);
        }

    }

    private void registerOreScanner(int id) {
        int subId, completId;
        String name;

        OreScanner descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Ore Scanner");

            descriptor = new OreScanner(name

            );
            Vars.sharedItem.addElement(completId, descriptor);
        }

    }

    private void registerMiningPipe(int id) {
        int subId, completId;
        String name;

        MiningPipeDescriptor descriptor;
        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Mining Pipe");

            descriptor = new MiningPipeDescriptor(name// iconId, name
            );
            Vars.sharedItem.addElement(completId, descriptor);

            Vars.miningPipeDescriptor = descriptor;
        }

    }

    private void registerTreeResinAndRubber(int id) {
        int subId, completId;
        String name;

        {
            TreeResin descriptor;
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Tree Resin");

            descriptor = new TreeResin(name);

            Vars.sharedItem.addElement(completId, descriptor);
            Vars.treeResin = descriptor;
            Vars.addToOre("materialResin", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Rubber");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Vars.addToOre("itemRubber", descriptor.newItemStack());
        }
    }
    private void registerRawCable(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Copper Cable");

            Vars.copperCableDescriptor = new CopperCableDescriptor(name);
            Vars.sharedItem.addElement(completId, Vars.copperCableDescriptor);
            Data.addResource(Vars.copperCableDescriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Iron Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 2;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Tungsten Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
    }

    private void registerArc(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Graphite Rod");

            GraphiteDescriptor descriptor = new GraphiteDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 1;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "2x Graphite Rods");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 2;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "3x Graphite Rods");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 3;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "4x Graphite Rods");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 4;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Synthetic Diamond");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 5;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "unreleasedium");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 6;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Arc Clay Ingot");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
            OreDictionary.registerOre("ingotAluminum", descriptor.newItemStack());
            OreDictionary.registerOre("ingotAluminium", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 7;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Arc Metal Ingot");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
            OreDictionary.registerOre("ingotSteel", descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 8;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Inert Canister");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        /*{
            GenericItemUsingDamageDescriptor descriptor;
            subId = 9;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "T1 Transmission Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 10;
            completId = subId + (id << 6);
            name = TR_NAME(Type.NONE, "T2 Transmission Cable");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }*/
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 11;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Canister of Water");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
        {
            GenericItemUsingDamageDescriptor descriptor;
            subId = 12;
            completId = subId + (id << 6);
            name = TR_NAME(I18N.Type.NONE, "Canister of Arc Water");

            descriptor = new GenericItemUsingDamageDescriptor(name);
            Vars.sharedItem.addElement(completId, descriptor);
            Data.addResource(descriptor.newItemStack());
        }
    }

    private void registerBrush(int id) {

        int subId;
        BrushDescriptor whiteDesc = null;
        String name;
        String[] subNames = {
            TR_NAME(I18N.Type.NONE, "Black Brush"),
            TR_NAME(I18N.Type.NONE, "Red Brush"),
            TR_NAME(I18N.Type.NONE, "Green Brush"),
            TR_NAME(I18N.Type.NONE, "Brown Brush"),
            TR_NAME(I18N.Type.NONE, "Blue Brush"),
            TR_NAME(I18N.Type.NONE, "Purple Brush"),
            TR_NAME(I18N.Type.NONE, "Cyan Brush"),
            TR_NAME(I18N.Type.NONE, "Silver Brush"),
            TR_NAME(I18N.Type.NONE, "Gray Brush"),
            TR_NAME(I18N.Type.NONE, "Pink Brush"),
            TR_NAME(I18N.Type.NONE, "Lime Brush"),
            TR_NAME(I18N.Type.NONE, "Yellow Brush"),
            TR_NAME(I18N.Type.NONE, "Light Blue Brush"),
            TR_NAME(I18N.Type.NONE, "Magenta Brush"),
            TR_NAME(I18N.Type.NONE, "Orange Brush"),
            TR_NAME(I18N.Type.NONE, "White Brush")};
        for (int idx = 0; idx < 16; idx++) {
            subId = idx;
            name = subNames[idx];
            BrushDescriptor desc = new BrushDescriptor(name);
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            whiteDesc = desc;
        }

        ItemStack emptyStack = Vars.findItemStack("White Brush");
        whiteDesc.setLife(emptyStack, 0);

        for (int idx = 0; idx < 16; idx++) {

            Eln.RECIPE_REGISTRATION.addShapelessRecipe(emptyStack.copy(),
                new ItemStack(Blocks.wool, 1, idx),
                Vars.findItemStack("Iron Cable"));
        }

        for (int idx = 0; idx < 16; idx++) {
            name = subNames[idx];
            Eln.RECIPE_REGISTRATION.addShapelessRecipe(Vars.findItemStack(name, 1),
                new ItemStack(Items.dye, 1, idx),
                emptyStack.copy());
        }

    }

    private void registerMiscItem(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Cheap Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            OreDictionary.registerOre(Vars.dictCheapChip, desc.newItemStack());
        }
        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Advanced Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            OreDictionary.registerOre(Vars.dictAdvancedChip, desc.newItemStack());
        }
        {
            subId = 2;
            name = TR_NAME(I18N.Type.NONE, "Machine Block");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("casingMachine", desc.newItemStack());
        }
        {
            subId = 3;
            name = TR_NAME(I18N.Type.NONE, "Electrical Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }
        {
            subId = 4;
            name = TR_NAME(I18N.Type.NONE, "Thermal Probe Chip");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }

        {
            subId = 6;
            name = TR_NAME(I18N.Type.NONE, "Copper Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateCopper", desc.newItemStack());
        }
        {
            subId = 7;
            name = TR_NAME(I18N.Type.NONE, "Iron Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateIron", desc.newItemStack());
        }
        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Gold Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateGold", desc.newItemStack());
        }
        {
            subId = 9;
            name = TR_NAME(I18N.Type.NONE, "Lead Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateLead", desc.newItemStack());
        }
        {
            subId = 10;
            name = TR_NAME(I18N.Type.NONE, "Silicon Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateSilicon", desc.newItemStack());
        }

        {
            subId = 11;
            name = TR_NAME(I18N.Type.NONE, "Alloy Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateAlloy", desc.newItemStack());
        }
        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "Coal Plate");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("plateCoal", desc.newItemStack());
        }

        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Silicon Dust");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("dustSilicon", desc.newItemStack());
        }
        {
            subId = 17;
            name = TR_NAME(I18N.Type.NONE, "Silicon Ingot");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("ingotSilicon", desc.newItemStack());
        }

        {
            subId = 22;
            name = TR_NAME(I18N.Type.NONE, "Machine Booster");
            MachineBoosterDescriptor desc = new MachineBoosterDescriptor(name);
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 23;
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                TR_NAME(I18N.Type.NONE, "Advanced Machine Block"), new String[]{}); // TODO: Description.
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
            Vars.addToOre("casingMachineAdvanced", desc.newItemStack());
        }
        {
            subId = 28;
            name = TR_NAME(I18N.Type.NONE, "Basic Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }
        {
            subId = 29;
            name = TR_NAME(I18N.Type.NONE, "Advanced Magnet");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }
        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "Data Logger Print");
            DataLogsPrintDescriptor desc = new DataLogsPrintDescriptor(name);
            Vars.dataLogsPrintDescriptor = desc;
            desc.setDefaultIcon("empty-texture");
            Vars.sharedItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 33;
            name = TR_NAME(I18N.Type.NONE, "Signal Antenna");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, new String[]{});
            Vars.sharedItem.addElement(subId + (id << 6), desc);
            Data.addResource(desc.newItemStack());
        }

        {
            subId = 40;
            name = TR_NAME(I18N.Type.NONE, "Player Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityPlayer.class, 0f, 1f, 0f);
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 41;
            name = TR_NAME(I18N.Type.NONE, "Monster Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, IMob.class, 1f, 0f, 0f);
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }
        {
            subId = 42;
            name = TR_NAME(I18N.Type.NONE, "Animal Filter");
            EntitySensorFilterDescriptor desc = new EntitySensorFilterDescriptor(name, EntityAnimal.class, .3f, .3f, 1f);
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }

        {
            subId = 48;
            name = TR_NAME(I18N.Type.NONE, "Wrench");
            GenericItemUsingDamageDescriptorWithComment desc = new GenericItemUsingDamageDescriptorWithComment(
                name, TR("Electrical age wrench,\nCan be used to turn\nsmall wall blocks").split("\n"));
            Vars.sharedItem.addElement(subId + (id << 6), desc);

            Vars.wrenchItemStack = desc.newItemStack();
        }

        {
            subId = 52;
            name = TR_NAME(I18N.Type.NONE, "Dielectric");
            DielectricItem desc = new DielectricItem(name, Vars.LVU);
            Vars.sharedItem.addElement(subId + (id << 6), desc);
        }

        Vars.sharedItem.addElement(53 + (id << 6), new CaseItemDescriptor(TR_NAME(I18N.Type.NONE, "Casing")));

        Vars.sharedItem.addElement(54 + (id << 6), new ClutchPlateItem("Iron Clutch Plate", 20480f, 1280f, 2560f, 640f, 0.0001f, false));
        Vars.sharedItem.addElement(55 + (id << 6), new ClutchPinItem("Clutch Pin"));
        Vars.sharedItem.addElement(56 + (id << 6), new ClutchPlateItem("Gold Clutch Plate", 4096f, 2048f, 512f, 128f, 0.01f, false));
        Vars.sharedItem.addElement(57 + (id << 6), new ClutchPlateItem("Copper Clutch Plate", 8192f, 4096f, 1024f, 512f, 0.003f, false));
        Vars.sharedItem.addElement(58 + (id << 6), new ClutchPlateItem("Lead Clutch Plate", 4096f, 1024f, 512f, 128f, 0.01f, false));
        Vars.sharedItem.addElement(59 + (id << 6), new ClutchPlateItem("Coal Clutch Plate", 1024f, 128f, 128f, 32f, 0.1f, true));
    }

    private void registerElectricalTool(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Small Flashlight");

            ElectricalLampItem desc = new ElectricalLampItem(
                name,
                //10, 8, 20, 15, 5, 50, old
                10, 8, 20, 15, 5, 50,// int light,int range
                6000, 100// , energyStorage,discharg, charge
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(I18N.Type.NONE, "Portable Electrical Mining Drill");

            ElectricalPickaxe desc = new ElectricalPickaxe(
                name,
                22, 1,// float strengthOn,float strengthOff, - Haxorian note: buffed this from 8,3 putting it around eff 4
                40000, 200, 10000// double energyStorage,double
                // energyPerBlock,double chargePower
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 12;
            name = TR_NAME(I18N.Type.NONE, "Portable Electrical Axe");

            ElectricalAxe desc = new ElectricalAxe(
                name,
                22, 1,// float strengthOn,float strengthOff, - Haxorian note: buffed this too
                40000, 200, 10000// double energyStorage,double energyPerBlock,double chargePower
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

    }

    private void registerPortableItem(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(I18N.Type.NONE, "Portable Battery");

            BatteryItem desc = new BatteryItem(
                name,
                40000, 125, 250,// double energyStorage,double - Haxorian note: doubled storage halved throughput.
                // chargePower,double dischargePower,
                2// int priority
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(I18N.Type.NONE, "Portable Battery Pack");

            BatteryItem desc = new BatteryItem(
                name,
                160000, 500, 1000,// double energyStorage,double - Haxorian note: Packs are in 4s now
                // chargePower,double dischargePower,
                2// int priority
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 16;
            name = TR_NAME(I18N.Type.NONE, "Portable Condensator");

            BatteryItem desc = new BatteryItem(
                name,
                4000, 2000, 2000,// double energyStorage,double - H: Slightly less power way more throughput
                // chargePower,double dischargePower,
                1// int priority
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 17;
            name = TR_NAME(I18N.Type.NONE, "Portable Condensator Pack");

            BatteryItem desc = new BatteryItem(
                name,
                16000, 8000, 8000,// double energyStorage,double
                // chargePower,double dischargePower,
                1// int priority
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 32;
            name = TR_NAME(I18N.Type.NONE, "X-Ray Scanner");

            PortableOreScannerItem desc = new PortableOreScannerItem(
                name, Vars.obj.getObj("XRayScanner"),
                100000, 400, 300,// double energyStorage,double - That's right, more buffs!
                // chargePower,double dischargePower,
                Vars.xRayScannerRange, (float) (Math.PI / 2),// float
                // viewRange,float
                // viewYAlpha,
                32, 20// int resWidth,int resHeight
            );
            Vars.sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
    }

    private void registerFuelBurnerItem(int id) {
        Vars.sharedItemStackOne.addElement(0 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(I18N.Type.NONE, "Small Fuel Burner"), 5000 * Vars.fuelHeatFurnacePowerFactor, 2, 1.6f));
        Vars.sharedItemStackOne.addElement(1 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(I18N.Type.NONE, "Medium Fuel Burner"), 10000 * Vars.fuelHeatFurnacePowerFactor, 1, 1.4f));
        Vars.sharedItemStackOne.addElement(2 + (id << 6),
            new FuelBurnerDescriptor(TR_NAME(I18N.Type.NONE, "Big Fuel Burner"), 25000 * Vars.fuelHeatFurnacePowerFactor, 0, 1f));
    }

    private void registerArmor() {
        String name;

        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Helmet");
            Vars.helmetCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Helmet, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_helmet").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.helmetCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.helmetCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Chestplate");
            Vars.plateCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Chestplate, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_chestplate").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.plateCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.plateCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Leggings");
            Vars.legsCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Leggings, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_leggings").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.legsCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.legsCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Boots");
            Vars.bootsCopper = (ItemArmor) (new genericArmorItem(ItemArmor.ArmorMaterial.IRON, 2, genericArmorItem.ArmourType.Boots, "eln:textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png")).setUnlocalizedName(name).setTextureName("eln:copper_boots").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.bootsCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.bootsCopper));
        }

        String t1, t2;
        t1 = "eln:textures/armor/ecoal_layer_1.png";
        t2 = "eln:textures/armor/ecoal_layer_2.png";
        double energyPerDamage = 500;
        int armor, armorMarge;
        ItemArmor.ArmorMaterial eCoalMaterial = net.minecraftforge.common.util.EnumHelper.addArmorMaterial("ECoal", 10, new int[]{3, 8, 6, 3}, 9);
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Helmet");
            armor = 3;
            armorMarge = 2; //getting rid of this. Safe to delete.
            Vars.helmetECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Helmet, t1, t2,
                //(armor + armorMarge) * energyPerDamage * 10
                8000, 2000.0,// double energyStorage,double chargePower
                armor / 20.0, armor * energyPerDamage,// double ratioMax,double ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_helmet").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.helmetECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.helmetECoal));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Chestplate");
            armor = 8;
            armorMarge = 4;
            Vars.plateECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Chestplate, t1, t2,
                //(armor + armorMarge) * energyPerDamage * 10
                8000, 2000.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_chestplate").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.plateECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.plateECoal));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Leggings");
            armor = 6;
            armorMarge = 3;
            Vars.legsECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Leggings, t1, t2,
                //(armor + armorMarge) * energyPerDamage * 10
                8000, 2000.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_leggings").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.legsECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.legsECoal));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "E-Coal Boots");
            armor = 3;
            armorMarge = 2;
            Vars.bootsECoal = (ItemArmor) (new ElectricalArmor(eCoalMaterial, 2, genericArmorItem.ArmourType.Boots, t1, t2,
                //(armor + armorMarge) * energyPerDamage * 10
                8000, 2000.0,// double
                // energyStorage,double
                // chargePower
                armor / 20.0, armor * energyPerDamage,// double
                // ratioMax,double
                // ratioMaxEnergy,
                energyPerDamage// double energyPerDamage
            )).setUnlocalizedName(name).setTextureName("eln:ecoal_boots").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.bootsECoal, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.bootsECoal));
        }
    }

    private void registerTool() {
        String name;
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Sword");
            Vars.swordCopper = (new ItemSword(Item.ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_sword").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.swordCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.swordCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Hoe");
            Vars.hoeCopper = (new ItemHoe(Item.ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_hoe").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.hoeCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.hoeCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Shovel");
            Vars.shovelCopper = (new ItemSpade(Item.ToolMaterial.IRON)).setUnlocalizedName(name).setTextureName("eln:copper_shovel").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.shovelCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.shovelCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Pickaxe");
            Vars.pickaxeCopper = new ItemPickaxeEln(Item.ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_pickaxe").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.pickaxeCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.pickaxeCopper));
        }
        {
            name = TR_NAME(I18N.Type.ITEM, "Copper Axe");
            Vars.axeCopper = new ItemAxeEln(Item.ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_axe").setCreativeTab(Vars.creativeTab);
            GameRegistry.registerItem(Vars.axeCopper, "Eln." + name);
            GameRegistry.registerCustomItemStack(name, new ItemStack(Vars.axeCopper));
        }

    }
}

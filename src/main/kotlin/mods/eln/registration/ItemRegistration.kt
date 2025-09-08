package mods.eln.registration

import cpw.mods.fml.common.registry.GameRegistry
import mods.eln.Eln
import mods.eln.Eln.instance
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment
import mods.eln.generic.genericArmorItem
import mods.eln.generic.genericArmorItem.ArmourType
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.item.*
import mods.eln.item.electricalitem.*
import mods.eln.item.regulator.IRegulatorDescriptor
import mods.eln.item.regulator.RegulatorAnalogDescriptor
import mods.eln.item.regulator.RegulatorOnOffDescriptor
import mods.eln.mechanical.ClutchPinItem
import mods.eln.mechanical.ClutchPlateItem
import mods.eln.ore.OreDescriptor
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor
import mods.eln.sixnode.lampsocket.LampSocketType
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.monster.IMob
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.*
import net.minecraft.item.Item.ToolMaterial
import net.minecraft.item.ItemArmor.ArmorMaterial
import net.minecraftforge.common.util.EnumHelper
import net.minecraftforge.oredict.OreDictionary

object ItemRegistration {
    fun registerItem() {
        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerHeatingCorp(1)

        // registerThermalIsolator(2);
        registerRegulatorItem(3)
        registerLampItem(4)
        registerProtection(5)
        registerCombustionChamber(6)
        registerFerromagneticCore(7)
        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerIngot(8)
        registerDust(9)
        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerElectricalMotor(10)

        //ITEM REGISTRATION
        //Sub-UID must be unique in this section only.
        //============================================
        registerSolarTracker(11)
        registerMeter(14)
        registerElectricalDrill(15)
        registerOreScanner(16)
        registerMiningPipe(17)
        registerTreeResinAndRubber(64)
        registerRawCable(65)
        registerArc(69)
        registerBrush(119)
        registerMiscItem(120)
        registerElectricalTool(121)
        registerPortableItem(122)
        registerFuelBurnerItem(124)


        registerBasicItems(126)

        registerArmor()
        registerTool()
        registerOre()
    }

    private fun registerHeatingCorp(id: Int) {
        var subId: Int
        var completId: Int

        var element: HeatingCorpElement
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 50V Copper Heating Corp"),
                Eln.LVU,
                150.0,
                190.0,
                instance.lowVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "50V Copper Heating Corp"),
                Eln.LVU,
                250.0,
                320.0,
                instance.lowVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 200V Copper Heating Corp"),
                Eln.MVU,
                400.0,
                500.0,
                instance.meduimVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "200V Copper Heating Corp"),
                Eln.MVU,
                600.0,
                750.0,
                instance.highVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 50V Iron Heating Corp"),
                Eln.LVU,
                180.0,
                225.0,
                instance.lowVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 5
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "50V Iron Heating Corp"),
                Eln.LVU,
                375.0,
                480.0,
                instance.lowVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 6
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 200V Iron Heating Corp"),
                Eln.MVU,
                600.0,
                750.0,
                instance.meduimVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 7
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "200V Iron Heating Corp"),
                Eln.MVU,
                900.0,
                1050.0,
                instance.highVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 8
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 50V Tungsten Heating Corp"),
                Eln.LVU,
                240.0,
                300.0,
                instance.lowVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 9
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "50V Tungsten Heating Corp"),
                Eln.LVU,
                500.0,
                640.0,
                instance.lowVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 10
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 200V Tungsten Heating Corp"),
                Eln.MVU,
                800.0,
                1000.0,
                instance.meduimVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 11
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "200V Tungsten Heating Corp"),
                Eln.MVU,
                1200.0,
                1500.0,
                instance.highVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 12
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 800V Tungsten Heating Corp"),
                Eln.HVU,
                3600.0,
                4800.0,
                instance.veryHighVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 13
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "800V Tungsten Heating Corp"),
                Eln.HVU,
                4812.0,
                6015.0,
                instance.veryHighVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 14
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 3.2kV Tungsten Heating Corp"),
                Eln.VVU,
                4000.0,
                6000.0,
                instance.veryHighVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 15
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "3.2kV Tungsten Heating Corp"),
                Eln.VVU,
                12000.0,
                15000.0,
                instance.veryHighVoltageCableDescriptor
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerRegulatorItem(id: Int) {
        var subId: Int
        var completId: Int
        var element: IRegulatorDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = RegulatorOnOffDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "On/OFF Regulator 1 Percent"), "onoffregulator",
                0.01
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = RegulatorOnOffDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "On/OFF Regulator 10 Percent"),
                "onoffregulator",
                0.1
            )
            Eln.sharedItem.addElement(completId, element)
        }

        run {
            subId = 8
            completId = subId + (id shl 6)
            element = RegulatorAnalogDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Analogic Regulator"
                ), "Analogicregulator"
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerLampItem(id: Int) {
        var subId: Int
        var completId: Int
        val lightPower =
            doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 20.0, 25.0, 30.0, 40.0)
        val lightLevel = DoubleArray(16)
        val economicPowerFactor = 0.5
        val standardGrowRate = 0.0
        for (idx in 0..15) {
            lightLevel[idx] = (idx + 0.49) / 15.0
        }
        var element: LampDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Small 50V Incandescent Light Bulb"),
                "incandescentironlamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.LVU,
                lightPower[12],
                lightLevel[12],
                instance.incandescentLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "50V Incandescent Light Bulb"),
                "incandescentironlamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.LVU,
                lightPower[14],
                lightLevel[14],
                instance.incandescentLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "200V Incandescent Light Bulb"),
                "incandescentironlamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.MVU,
                lightPower[14],
                lightLevel[14],
                instance.incandescentLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Small 50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.LVU,
                lightPower[11],
                lightLevel[11],
                instance.carbonLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 5
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "50V Carbon Incandescent Light Bulb"),
                "incandescentcarbonlamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.LVU,
                lightPower[13],
                lightLevel[13],
                instance.carbonLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 16
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Small 50V Economic Light Bulb"),
                "fluorescentlamp",
                LampDescriptor.Type.ECO,
                LampSocketType.Douille,
                Eln.LVU,
                lightPower[12] * economicPowerFactor,
                lightLevel[12],
                instance.economicLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 17
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "50V Economic Light Bulb"), "fluorescentlamp",
                LampDescriptor.Type.ECO, LampSocketType.Douille, Eln.LVU, lightPower[14] * economicPowerFactor,
                lightLevel[14], instance.economicLampLife, standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 18
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "200V Economic Light Bulb"), "fluorescentlamp",
                LampDescriptor.Type.ECO, LampSocketType.Douille, Eln.MVU, lightPower[14] * economicPowerFactor,
                lightLevel[14], instance.economicLampLife, standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 32
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "50V Farming Lamp"),
                "farminglamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.LVU,
                120.0,
                lightLevel[15],
                instance.incandescentLampLife,
                0.50
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 36
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "200V Farming Lamp"),
                "farminglamp",
                LampDescriptor.Type.INCANDESCENT,
                LampSocketType.Douille,
                Eln.MVU,
                120.0,
                lightLevel[15],
                instance.incandescentLampLife,
                0.50
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 37
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "50V LED Bulb"),
                "ledlamp",
                LampDescriptor.Type.LED,
                LampSocketType.Douille,
                Eln.LVU,
                lightPower[14] / 2,
                lightLevel[14],
                instance.ledLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 38
            completId = subId + (id shl 6)
            element = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "200V LED Bulb"),
                "ledlamp",
                LampDescriptor.Type.LED,
                LampSocketType.Douille,
                Eln.MVU,
                lightPower[14] / 2,
                lightLevel[14],
                instance.ledLampLife,
                standardGrowRate
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerProtection(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            val element = OverHeatingProtectionDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Overheating Protection"
                )
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            val element = OverVoltageProtectionDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Overvoltage Protection"
                )
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerCombustionChamber(id: Int) {
        var subId: Int
        var completId: Int
        run {
            subId = 0
            completId = subId + (id shl 6)
            val element = CombustionChamber(I18N.TR_NAME(I18N.Type.NONE, "Combustion Chamber"))
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            val element = ThermalIsolatorElement(
                I18N.TR_NAME(I18N.Type.NONE, "Thermal Insulation"),
                0.5,
                500.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerFerromagneticCore(id: Int) {
        var subId: Int
        var completId: Int

        var element: FerromagneticCoreDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = FerromagneticCoreDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Cheap Ferromagnetic Core"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), 100.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = FerromagneticCoreDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Average Ferromagnetic Core"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), 50.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element = FerromagneticCoreDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Optimal Ferromagnetic Core"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), 1.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun addToOre(name: String, ore: ItemStack) {
        OreDictionary.registerOre(name, ore)
        Eln.dictionnaryOreFromMod[name] = ore
    }

    private fun registerDust(id: Int) {
        var id = id
        var subId: Int
        var completId: Int
        var name: String?
        var element: GenericItemUsingDamageDescriptorWithComment

        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("dustCopper", element.newItemStack())
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Iron Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("dustIron", element.newItemStack())
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Lapis Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("dustLapis", element.newItemStack())
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Diamond Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("dustDiamond", element.newItemStack())
        }

        run {
            id = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(id, element)
            Data.addResource(element.newItemStack())
            addToOre("dustLead", element.newItemStack())
        }
        run {
            id = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(id, element)
            Data.addResource(element.newItemStack())
            addToOre(Eln.dictTungstenDust, element.newItemStack())
        }

        run {
            id = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "Gold Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(id, element)
            Data.addResource(element.newItemStack())
            addToOre("dustGold", element.newItemStack())
        }

        run {
            id = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Coal Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(id, element)
            Data.addResource(element.newItemStack())
            addToOre("dustCoal", element.newItemStack())
        }
        run {
            id = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Alloy Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(id, element)
            Data.addResource(element.newItemStack())
            addToOre("dustAlloy", element.newItemStack())
        }

        run {
            id = 10
            name = I18N.TR_NAME(I18N.Type.NONE, "Cinnabar Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(id, element)
            Data.addResource(element.newItemStack())
            addToOre("dustCinnabar", element.newItemStack())
        }
    }

    private fun registerIngot(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var element: GenericItemUsingDamageDescriptorWithComment

        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            instance.copperIngot = element
            Data.addResource(element.newItemStack())
            addToOre("ingotCopper", element.newItemStack())
        }

        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            instance.plumbIngot = element
            Data.addResource(element.newItemStack())
            addToOre("ingotLead", element.newItemStack())
        }

        run {
            subId = 5
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            instance.tungstenIngot = element
            Data.addResource(element.newItemStack())
            addToOre(Eln.dictTungstenIngot, element.newItemStack())
        }

        run {
            subId = 6
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Ferrite Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf(tr("useless"), tr("Really useless"))
            )
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("ingotFerrite", element.newItemStack())
        }

        run {
            subId = 7
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Alloy Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("ingotAlloy", element.newItemStack())
        }

        run {
            subId = 8
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Mercury")
            element =
                GenericItemUsingDamageDescriptorWithComment(name, arrayOf(tr("useless"), tr("miaou")))
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
            addToOre("quicksilver", element.newItemStack())
        }
    }

    private fun registerBasicItems(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Wafer")
            Eln.siliconWafer = SiliconWafer(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.siliconWafer)
            OreDictionary.registerOre(Eln.dictSiliconWafer, Eln.siliconWafer.newItemStack())
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Transistor")
            Eln.transistor = Transistor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.transistor)
            OreDictionary.registerOre(Eln.dictTransistor, Eln.transistor.newItemStack())
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "NTC Thermistor")
            Eln.thermistor = Thermistor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.thermistor)
            OreDictionary.registerOre(Eln.dictThermistor, Eln.thermistor.newItemStack())
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "Nibble Memory Chip")
            Eln.nibbleMemory = NibbleMemory(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.nibbleMemory)
            OreDictionary.registerOre(Eln.dictNibbleMemory, Eln.nibbleMemory.newItemStack())
        }
        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Arithmetic Logic Unit")
            Eln.alu = ArithmeticLogicUnit(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.alu)
            OreDictionary.registerOre(Eln.dictALU, Eln.alu.newItemStack())
        }
    }

    private fun registerElectricalMotor(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?
        var element: GenericItemUsingDamageDescriptorWithComment

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Motor")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Electrical Motor")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            Data.addResource(element.newItemStack())
        }
    }

    private fun registerSolarTracker(id: Int) {
        var subId: Int
        var completId: Int

        var element: SolarTrackerDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = SolarTrackerDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Solar Tracker"))
            Eln.sharedItem.addElement(completId, element)
        }
    }


    private fun registerMeter(id: Int) {
        var subId: Int
        var completId: Int

        var element: GenericItemUsingDamageDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element =
                GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE, "MultiMeter"))
            Eln.sharedItem.addElement(completId, element)
            Eln.multiMeterElement = element
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = GenericItemUsingDamageDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Thermometer"
                )
            )
            Eln.sharedItem.addElement(completId, element)
            Eln.thermometerElement = element
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element =
                GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE, "AllMeter"))
            Eln.sharedItem.addElement(completId, element)
            Eln.allMeterElement = element
        }
        run {
            subId = 8
            completId = subId + (id shl 6)
            element = WirelessSignalAnalyserItemDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Wireless Analyser"
                )
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 16
            completId = subId + (id shl 6)
            element =
                ConfigCopyToolDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Config Copy Tool"))
            Eln.sharedItem.addElement(completId, element)
            Eln.configCopyToolElement = element
        }
    }

    private fun registerTreeResinAndRubber(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tree Resin")
            val descriptor = TreeResin(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.treeResin = descriptor
            addToOre("materialResin", descriptor.newItemStack())
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Rubber")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            addToOre("itemRubber", descriptor.newItemStack())
        }
    }


    private fun registerElectricalDrill(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: ElectricalDrillDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Cheap Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 8.0, 4000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Average Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 5.0, 5000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Fast Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 3.0, 6000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Turbo Electrical Drill")

            descriptor = ElectricalDrillDescriptor(name, 1.0, 10000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Irresponsible Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 0.1, 20000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
    }

    private fun registerOreScanner(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: OreScanner
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Ore Scanner")
            descriptor = OreScanner(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
    }

    private fun registerMiningPipe(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: MiningPipeDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Mining Pipe")
            descriptor = MiningPipeDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.miningPipeDescriptor = descriptor
        }
    }


    private fun registerRawCable(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Cable")
            instance.copperCableDescriptor = CopperCableDescriptor(name)
            Eln.sharedItem.addElement(completId, instance.copperCableDescriptor)
            Data.addResource(instance.copperCableDescriptor.newItemStack())
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Iron Cable")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Cable")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
    }

    private fun registerArc(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Graphite Rod")
            val graphiteDescriptor = GraphiteDescriptor(name)
            Eln.sharedItem.addElement(completId, graphiteDescriptor)
            Data.addResource(graphiteDescriptor.newItemStack())
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "2x Graphite Rods")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "3x Graphite Rods")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "4x Graphite Rods")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Synthetic Diamond")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 5
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "unreleasedium")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 6
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Arc Clay Ingot")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
            OreDictionary.registerOre("ingotAluminum", descriptor.newItemStack())
            OreDictionary.registerOre("ingotAluminium", descriptor.newItemStack())
        }
        run {
            subId = 7
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Arc Metal Ingot")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
            OreDictionary.registerOre("ingotSteel", descriptor.newItemStack())
        }
        run {
            subId = 8
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Inert Canister")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        /*{
        GenericItemUsingDamageDescriptor descriptor;
        subId = 9;
        completId = subId + (id << 6);
        name = TR_NAME(Type.NONE, "T1 Transmission Cable");

        descriptor = new GenericItemUsingDamageDescriptor(name);
        sharedItem.addElement(completId, descriptor);
        Data.addResource(descriptor.newItemStack());
    }
    {
        GenericItemUsingDamageDescriptor descriptor;
        subId = 10;
        completId = subId + (id << 6);
        name = TR_NAME(Type.NONE, "T2 Transmission Cable");

        descriptor = new GenericItemUsingDamageDescriptor(name);
        sharedItem.addElement(completId, descriptor);
        Data.addResource(descriptor.newItemStack());
    }*/
        run {
            subId = 11
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Canister of Water")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
        run {
            subId = 12
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Canister of Arc Water")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Data.addResource(descriptor.newItemStack())
        }
    }

    private fun registerElectricalTool(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Small Flashlight")
            val desc = ElectricalLampItem(name, 10, 6, 20.0, 12, 8, 50.0, 6000.0, 100.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Improved Flashlight")
            val desc = ElectricalLampItem(name, 15, 8, 20.0, 15, 12, 50.0, 24000.0, 400.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Electrical Mining Drill")
            // About the power of Efficiency 4
            val desc = ElectricalPickaxe(name, 22f, 1f, 120000.0, 200.0, 10000.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Electrical Axe")
            val desc = ElectricalAxe(name, 22f, 1f, 40000.0, 200.0, 10000.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
    }

    private fun registerPortableItem(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Battery")
            val desc = BatteryItem(name, 40000.0, 125.0, 250.0, 2)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Battery Pack")
            val desc = BatteryItem(name, 160000.0, 500.0, 1000.0, 2)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Condensator")
            val desc = BatteryItem(name, 4000.0, 2000.0, 2000.0, 1)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 17
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Condensator Pack")
            val desc = BatteryItem(name, 16000.0, 8000.0, 8000.0, 1)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 32
            name = I18N.TR_NAME(I18N.Type.NONE, "X-Ray Scanner")
            val desc = PortableOreScannerItem(
                name, Eln.obj.getObj("XRayScanner"), 100000.0, 400.0,
                300.0, instance.xRayScannerRange, (Math.PI / 2).toFloat(), 32, 20
            )
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
    }

    private fun registerFuelBurnerItem(id: Int) {
        Eln.sharedItemStackOne.addElement(
            0 + (id shl 6), FuelBurnerDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Small Fuel Burner"),
                5000 * instance.fuelHeatFurnacePowerFactor, 2, 1.6f
            )
        )
        Eln.sharedItemStackOne.addElement(
            1 + (id shl 6), FuelBurnerDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Medium Fuel Burner"
                ), 10000 * instance.fuelHeatFurnacePowerFactor, 1, 1.4f
            )
        )
        Eln.sharedItemStackOne.addElement(
            2 + (id shl 6), FuelBurnerDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Big Fuel Burner"),
                25000 * instance.fuelHeatFurnacePowerFactor, 0, 1f
            )
        )
    }

    private fun registerMiscItem(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Cheap Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            OreDictionary.registerOre(Eln.dictCheapChip, desc.newItemStack())
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            OreDictionary.registerOre(Eln.dictAdvancedChip, desc.newItemStack())
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "Machine Block")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("casingMachine", desc.newItemStack())
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Probe Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
        }
        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Thermal Probe Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
        }

        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            Eln.plateCopper = desc
            addToOre("plateCopper", desc.newItemStack())
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "Iron Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("plateIron", desc.newItemStack())
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Gold Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("plateGold", desc.newItemStack())
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("plateLead", desc.newItemStack())
        }
        run {
            subId = 10
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("plateSilicon", desc.newItemStack())
        }

        run {
            subId = 11
            name = I18N.TR_NAME(I18N.Type.NONE, "Alloy Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("plateAlloy", desc.newItemStack())
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "Coal Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("plateCoal", desc.newItemStack())
        }

        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Dust")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("dustSilicon", desc.newItemStack())
        }
        run {
            subId = 17
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Ingot")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("ingotSilicon", desc.newItemStack())
        }

        run {
            subId = 22
            name = I18N.TR_NAME(I18N.Type.NONE, "Machine Booster")
            val desc = MachineBoosterDescriptor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 23
            val desc =
                GenericItemUsingDamageDescriptorWithComment(
                    I18N.TR_NAME(I18N.Type.NONE, "Advanced Machine Block"),
                    arrayOf()
                ) // TODO: Description.
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
            addToOre("casingMachineAdvanced", desc.newItemStack())
        }
        run {
            subId = 28
            name = I18N.TR_NAME(I18N.Type.NONE, "Basic Magnet")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
        }
        run {
            subId = 29
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Magnet")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
        }
        run {
            subId = 32
            name = I18N.TR_NAME(I18N.Type.NONE, "Data Logger Print")
            val desc = DataLogsPrintDescriptor(name)
            instance.dataLogsPrintDescriptor = desc
            desc.setDefaultIcon("empty-texture")
            Eln.sharedItem.addWithoutRegistry(subId + (id shl 6), desc)
        }

        run {
            subId = 33
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Antenna")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Data.addResource(desc.newItemStack())
        }

        run {
            subId = 40
            name = I18N.TR_NAME(I18N.Type.NONE, "Player Filter")
            val desc =
                EntitySensorFilterDescriptor(name, EntityPlayer::class.java, 0f, 1f, 0f)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 41
            name = I18N.TR_NAME(I18N.Type.NONE, "Monster Filter")
            val desc = EntitySensorFilterDescriptor(name, IMob::class.java, 1f, 0f, 0f)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 42
            name = I18N.TR_NAME(I18N.Type.NONE, "Animal Filter")
            val desc = EntitySensorFilterDescriptor(
                name, EntityAnimal::class.java, .3f, .3f,
                1f
            )
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 48
            name = I18N.TR_NAME(I18N.Type.NONE, "Wrench")
            val desc = GenericItemUsingDamageDescriptorWithComment(name,
                tr("Electrical age wrench,\nCan be used to turn\nsmall wall blocks")
                    .split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.wrenchItemStack = desc.newItemStack()
        }

        run {
            subId = 52
            name = I18N.TR_NAME(I18N.Type.NONE, "Dielectric")
            val desc = DielectricItem(name, Eln.LVU)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        Eln.sharedItem.addElement(53 + (id shl 6), CaseItemDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Casing")))
        Eln.sharedItem.addElement(
            54 + (id shl 6), ClutchPlateItem(
                "Iron Clutch Plate", 5120f, 640f, 640f, 160f,
                0.0001f, false
            )
        )
        Eln.sharedItem.addElement(55 + (id shl 6), ClutchPinItem("Clutch Pin"))
        Eln.sharedItem.addElement(
            56 + (id shl 6), ClutchPlateItem(
                "Gold Clutch Plate", 10240f, 2048f, 1024f, 512f,
                0.001f, false
            )
        )
        Eln.sharedItem.addElement(
            57 + (id shl 6), ClutchPlateItem(
                "Copper Clutch Plate", 8192f, 4096f, 1024f, 512f,
                0.0003f, false
            )
        )
        Eln.sharedItem.addElement(
            58 + (id shl 6), ClutchPlateItem(
                "Lead Clutch Plate", 15360f, 1024f, 1536f, 768f,
                0.0015f, false
            )
        )
        Eln.sharedItem.addElement(
            59 + (id shl 6), ClutchPlateItem(
                "Coal Clutch Plate", 1024f, 128f, 128f, 32f, 0.1f,
                true
            )
        )
    }

    private fun registerBrush(id: Int) {
        var subId: Int

        var name: String
        val subNames = arrayOf(
            I18N.TR_NAME(I18N.Type.NONE, "Black Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Red Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Green Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Brown Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Blue Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Purple Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Cyan Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Silver Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Gray Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Pink Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Lime Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE, "Yellow" +
                        " Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Light Blue Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Magenta Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Orange Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "White Brush")
        )
        for (idx in 0..15) {
            subId = idx
            name = subNames[idx]
            val desc = BrushDescriptor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.whiteDesc = desc
            Eln.brushSubNames = subNames.toList()
        }
    }

    public fun registerOre() {
        var id: Int
        var name: String?

        run {
            id = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Ore")
            val desc =
                OreDescriptor(name, id, 30 * (if (Eln.genCopper) 1 else 0), 6, 10, 0, 80)
            Eln.oreCopper = desc
            Eln.oreItem.addDescriptor(id, desc)
            addToOre("oreCopper", desc.newItemStack())
        }

        run {
            id = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Ore")
            val desc =
                OreDescriptor(name, id, 8 * (if (Eln.genLead) 1 else 0), 3, 9, 0, 24)
            Eln.oreItem.addDescriptor(id, desc)
            addToOre("oreLead", desc.newItemStack())
        }
        run {
            id = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Ore")
            val desc =
                OreDescriptor(name, id, 6 * (if (Eln.genTungsten) 1 else 0), 3, 9, 0, 32)
            Eln.oreItem.addDescriptor(id, desc)
            addToOre(Eln.dictTungstenOre, desc.newItemStack())
        }
        run {
            id = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Cinnabar Ore")
            val desc =
                OreDescriptor(name, id, 3 * (if (Eln.genCinnabar) 1 else 0), 3, 9, 0, 32)
            Eln.oreItem.addDescriptor(id, desc)
            addToOre("oreCinnabar", desc.newItemStack())
        }
    }

    public fun registerArmor() {
        var name: String

        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Helmet")
            Eln.helmetCopper = genericArmorItem(
                ArmorMaterial.IRON, 2, ArmourType.Helmet, "eln:textures" +
                        "/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png"
            ).setUnlocalizedName(name).setTextureName("eln:copper_helmet").setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.helmetCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.helmetCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Chestplate")
            Eln.chestplateCopper = genericArmorItem(
                ArmorMaterial.IRON, 2, ArmourType.Chestplate, "eln" +
                        ":textures/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png"
            ).setUnlocalizedName(name).setTextureName("eln:copper_chestplate")
                .setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.chestplateCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.chestplateCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Leggings")
            Eln.legsCopper = genericArmorItem(
                ArmorMaterial.IRON, 2, ArmourType.Leggings, "eln:textures" +
                        "/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png"
            ).setUnlocalizedName(name).setTextureName("eln:copper_leggings")
                .setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.legsCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.legsCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Boots")
            Eln.bootsCopper = genericArmorItem(
                ArmorMaterial.IRON, 2, ArmourType.Boots, "eln:textures" +
                        "/armor/copper_layer_1.png", "eln:textures/armor/copper_layer_2.png"
            ).setUnlocalizedName(name).setTextureName("eln:copper_boots").setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.bootsCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.bootsCopper))
        }
        val t1 = "eln:textures/armor/ecoal_layer_1.png"
        val t2 = "eln:textures/armor/ecoal_layer_2.png"
        val energyPerDamage = 500.0
        var armor: Int
        val eCoalMaterial = EnumHelper.addArmorMaterial(
            "ECoal", 10,
            intArrayOf(3, 8, 6, 3), 9
        )
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "E-Coal Helmet")
            armor = 3
            Eln.helmetECoal = ElectricalArmor(
                eCoalMaterial, 2, ArmourType.Helmet, t1, t2, 8000.0, 2000.0,
                armor / 20.0, armor * energyPerDamage, energyPerDamage
            ).setUnlocalizedName(name).setTextureName(
                "eln" +
                        ":ecoal_helmet"
            ).setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.helmetECoal, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.helmetECoal))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "E-Coal Chestplate")
            armor = 8
            Eln.plateECoal = ElectricalArmor(
                eCoalMaterial, 2, ArmourType.Chestplate, t1, t2, 8000.0,
                2000.0, armor / 20.0, armor * energyPerDamage, energyPerDamage
            ).setUnlocalizedName(name).setTextureName("eln:ecoal_chestplate")
                .setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.plateECoal, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.plateECoal))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "E-Coal Leggings")
            armor = 6
            Eln.legsECoal = ElectricalArmor(
                eCoalMaterial, 2, ArmourType.Leggings, t1, t2, 8000.0, 2000.0,
                armor / 20.0, armor * energyPerDamage, energyPerDamage
            ).setUnlocalizedName(name).setTextureName(
                "eln" +
                        ":ecoal_leggings"
            ).setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.legsECoal, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.legsECoal))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "E-Coal Boots")
            armor = 3
            Eln.bootsECoal = ElectricalArmor(
                eCoalMaterial, 2, ArmourType.Boots, t1, t2, 8000.0, 2000.0,
                armor / 20.0, armor * energyPerDamage, energyPerDamage
            ).setUnlocalizedName(name).setTextureName(
                "eln" +
                        ":ecoal_boots"
            ).setCreativeTab(Eln.creativeTab) as ItemArmor
            GameRegistry.registerItem(Eln.bootsECoal, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.bootsECoal))
        }
    }

    public fun registerTool() {
        var name: String
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Sword")
            Eln.swordCopper = ItemSword(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName(
                "eln" +
                        ":copper_sword"
            ).setCreativeTab(Eln.creativeTab)
            GameRegistry.registerItem(Eln.swordCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.swordCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Hoe")
            Eln.hoeCopper =
                ItemHoe(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_hoe")
                    .setCreativeTab(Eln.creativeTab)
            GameRegistry.registerItem(Eln.hoeCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.hoeCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Shovel")
            Eln.shovelCopper = ItemSpade(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName(
                "eln" +
                        ":copper_shovel"
            ).setCreativeTab(Eln.creativeTab)
            GameRegistry.registerItem(Eln.shovelCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.shovelCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Pickaxe")
            Eln.pickaxeCopper = ItemPickaxeEln(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName(
                "eln" +
                        ":copper_pickaxe"
            ).setCreativeTab(Eln.creativeTab)
            GameRegistry.registerItem(Eln.pickaxeCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.pickaxeCopper))
        }
        run {
            name = I18N.TR_NAME(I18N.Type.ITEM, "Copper Axe")
            Eln.axeCopper =
                ItemAxeEln(ToolMaterial.IRON).setUnlocalizedName(name).setTextureName("eln:copper_axe")
                    .setCreativeTab(Eln.creativeTab)
            GameRegistry.registerItem(Eln.axeCopper, "Eln.$name")
            GameRegistry.registerCustomItemStack(name, ItemStack(Eln.axeCopper))
        }
    }

}
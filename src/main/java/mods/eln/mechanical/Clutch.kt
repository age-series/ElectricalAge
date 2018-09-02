package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericItemUsingDamage
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.HelperStdContainer
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sound.LoopedSound
import mods.eln.sound.SoundCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class ClutchPlateItem(name: String) : GenericItemUsingDamageDescriptor(name) {
    override fun getDefaultNBT() = NBTTagCompound()

    fun setWear(stack: ItemStack, wear: Double) {
        if (!stack.hasTagCompound()) {
            stack.tagCompound = defaultNBT
        }
        stack.tagCompound.setDouble("wear", wear)
    }

    fun getWear(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) return 0.0
        return stack.tagCompound.getDouble("wear")
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<Any?>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if(itemStack != null) {
            val wear = getWear(itemStack)
            if(wear < 0.2) {
                list?.add(tr("Condition:") + " " + tr("New"))
            } else if(wear < 0.5) {
                list?.add(tr("Condition:") + " " + tr("Good"))
            } else if(wear < 0.8) {
                list?.add(tr("Condition:") + " " + tr("Used"))
            } else if(wear < 0.9) {
                list?.add(tr("Condition:") + " " + tr("End of life"))
            } else {
                list?.add(tr("Condition:") + " " + tr("Bad"))
            }
        }
    }
}

class ClutchDescriptor(name: String, override val obj: Obj3D) : SimpleShaftDescriptor(name, ClutchElement::class, ClutchRender::class, EntityMetaTag.Basic) {
    companion object {
        val degToRad = 360.0 / (2 * Math.PI)
    }

    val slipSound = "eln:clutch"
    val slipStopSound = "eln:click"

    override val static: Array<out Obj3D.Obj3DPart> = arrayOf(
        obj.getPart("Stand"),
        obj.getPart("Cowl")
    )
    override val rotating: Array<out Obj3D.Obj3DPart> = emptyArray()

    val leftShaftPart = obj.getPart("ShaftXN")
    val rightShaftPart = obj.getPart("ShaftXP")

    override fun draw(angle: Double) {
        draw(angle, angle)
    }

    fun draw(leftAngle: Double, rightAngle: Double) {
        static.forEach { it.draw() }

        preserveMatrix {
            GL11.glRotated(leftAngle * degToRad, 0.0, 0.0, 1.0)
            leftShaftPart.draw()
        }
        preserveMatrix {
            GL11.glRotated(rightAngle * degToRad, 0.0, 0.0, 1.0)
            rightShaftPart.draw()
        }
    }
}

class ClutchElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    companion object {
        // rads -> rads
        val staticMarginF = LinearFunction(0f, 1f, 1000f, 5f)
        // wear -> Joules
        val maxStaticEnergyF = LinearFunction(0f, 20480f, 1f, 0f)
        // wear -> N*m
        val dynamicMaxTransferF = LinearFunction(0f, 2560f, 1f, 640f)
        // rads -> wear
        val slipWearF = LinearFunction(0f, 0f, 1000f, 0.0001f)
    }

    override val shaftMass: Double = 0.5
    val connectedSides = DirectionSet()
    var leftShaft = ShaftNetwork()
    var rightShaft = ShaftNetwork()
    override fun initialize() {
        reconnect()
        // Carry over loaded rads, if any
        val lRads = leftShaft.rads
        val rRads = rightShaft.rads
        leftShaft = ShaftNetwork(this, front.left())
        rightShaft = ShaftNetwork(this, front.right())
        leftShaft.rads = lRads
        rightShaft.rads = rRads
        // These calls can still change the speed via mergeShaft
        leftShaft.connectShaft(this, front.left())
        rightShaft.connectShaft(this, front.right())
        if(getShaft(front.left()) != leftShaft)
            Utils.println("CE.init ERROR: getShaft(left) != leftShaft")
        if(getShaft(front.right()) != rightShaft)
            Utils.println("CE.init ERROR: getShaft(right) != rightShaft")
        // Utils.println(String.format("CE.i: new left %s r=%f, right %s r=%f", leftShaft, leftShaft.rads, rightShaft, rightShaft.rads))
    }

    override fun onBreakElement() {
        destructing = true
        leftShaft.disconnectShaft(this)
        rightShaft.disconnectShaft(this)
    }

    override fun isDestructing() = destructing

    override fun getShaft(dir: Direction) = when(dir) {
        front.left() -> leftShaft
        front.right() -> rightShaft
        else -> null
    }
    override fun setShaft(dir: Direction, net: ShaftNetwork?) {
        if(net == null) return
        when(dir) {
            front.left() -> leftShaft = net
            front.right() -> rightShaft = net
            else -> Unit
        }
    }

    override fun isInternallyConnected(a: Direction, b: Direction) = false

    val inv = TransparentNodeElementInventory(1, 1, this)
    override fun getInventory() = inv
    override fun newContainer(side: Direction?, player: EntityPlayer?) = ClutchContainer(player, inv)
    override fun hasGui() = true

    val inputGate = NbtElectricalGateInput("clutchIn")
    var slipping = true
    var lastSlipping = false
    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = inputGate
    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = NodeBase.maskElectricalInputGate

    val clutchPlateStack: ItemStack?
        get() {
            return inv.getStackInSlot(0)
        }
    @Suppress("UNCHECKED_CAST")
    val clutchPlateDescriptor: ClutchPlateItem?
        get() {
            val stack = clutchPlateStack
            if(stack == null) return null
            return (stack.item!! as GenericItemUsingDamage<GenericItemUsingDamageDescriptor>).getDescriptor(stack) as ClutchPlateItem
        }

    val LEFT = 0
    val RIGHT = 1

    var preRads = arrayOf(0.0, 0.0)
    var preEnergy = arrayOf(0.0, 0.0)

    inner class ClutchPreProcess : IProcess {
        override fun process(time: Double) {
            preRads[LEFT] = leftShaft.rads
            preRads[RIGHT] = rightShaft.rads
            preEnergy[LEFT] = leftShaft.energy
            preEnergy[RIGHT] = rightShaft.energy
        }
    }

    init {
        slowPreProcessList.add(ClutchPreProcess())
    }

    inner class ClutchPostProcess : IProcess {
        override fun process(time: Double) {
            val clutching = inputGate.normalized
            if (clutching == 0.0) {
                // Utils.println("CP.p: stop: no input")
                slipping = true
                return
            }

            val plateDescriptor = clutchPlateDescriptor
            val stack = clutchPlateStack
            if(plateDescriptor == null || stack == null) {
                // Utils.println("CP.p: stop: no inventory")
                slipping = true
                return
            }
            val wear = plateDescriptor.getWear(stack)
            if(wear >= 1.0) {
                // Utils.println("CP.p: stop: wear too high")
                slipping = true
                return
            }

            if(leftShaft == rightShaft) Utils.println("WARN (ClutchProcess): Networks are the same!")

            val mass = leftShaft.mass + rightShaft.mass
            val slower: ShaftNetwork
            val faster: ShaftNetwork
            val slowerIdx: Int
            val fasterIdx: Int
            if(preRads[LEFT] > preRads[RIGHT]) {
                slower = rightShaft
                faster = leftShaft
                slowerIdx = RIGHT
                fasterIdx = LEFT
            } else {
                slower = leftShaft
                faster = rightShaft
                slowerIdx = LEFT
                fasterIdx = RIGHT
            }

            if(slipping) {
                // Dynamic friction; transfer energy proportional to the max torque times delta v
                val torque = clutching * dynamicMaxTransferF.getValue(wear)
                val deltaR = faster.rads - slower.rads
                val avgR = (faster.rads + slower.rads) / 2.0
                val power = torque * deltaR
                slower.energy += power
                faster.energy -= power
                // Add a small margin to account for numerical inaccuracies
                val margin = staticMarginF.getValue(Math.max(faster.rads, slower.rads))
                //if(slower.rads > faster.rads - margin) {
                if (slower.rads > faster.rads - margin)
                {
                    // Sign change
                    val flow = leftShaft.energy - rightShaft.energy - preEnergy[LEFT] + preEnergy[RIGHT]
                    val maxE = clutching * maxStaticEnergyF.getValue(wear)
                    if(Math.abs(flow) <= maxE) {
                        slipping = false
                        needPublish()
                        Utils.println("CPP.p: stopped slipping")
                        val dEFast = faster.energy - preEnergy[fasterIdx]
                        val dESlow = slower.energy - preEnergy[slowerIdx]
                        val tnumerator = (preRads[fasterIdx] - preRads[slowerIdx]) * faster.mass * slower.mass * preRads[fasterIdx] * preRads[slowerIdx]
                        var tdenominator = dESlow * faster.mass * faster.rads - dEFast * slower.mass * slower.rads
                        if (tdenominator == 0.0) {
                            Utils.println("CPP.p: tdenom was 0")
                            tdenominator = 1.0
                        }
                        val t = tnumerator / tdenominator
                        if (t <= 1 && t >= 0) {
                            val dWFast = faster.rads - preRads[fasterIdx]
                            val finalW = preRads[fasterIdx] + t * dWFast
                            faster.rads = finalW
                            slower.rads = finalW
                        }
                    }
                } else {
                    clutchPlateDescriptor!!.setWear(clutchPlateStack!!, wear + clutching * slipWearF.getValue(Math.abs(deltaR)))
                }
            }

            /*
            if(!slipping) {
                val maxE = maxStaticEnergyF.getValue(wear)
                // Calculate the differences in energies using the delta over the tick
                val deltaEL = leftShaft.energy - preEnergy[LEFT]
                val deltaER = rightShaft.energy - preEnergy[RIGHT]
                var deltaE = deltaEL + deltaER
                // ...this calculation is done from the perspective of the left shaft--it can be done from the right wolog
                // Clamp energy based on max transfer, slip if needed
                if(deltaE < -maxE) {
                    slipping = true
                    deltaE = -maxE
                }
                if(deltaE > maxE) {
                    slipping = true
                    deltaE = maxE
                }
                val chgEL = (leftShaft.mass / mass) * deltaE
                val chgER = (rightShaft.mass / mass) * deltaE
                leftShaft.energy = preEnergy[LEFT] + chgEL
                rightShaft.energy = preEnergy[RIGHT] + chgER
            }
            */
            /*
             if(!slipping) {
                 val maxE = maxStaticEnergyF.getValue(wear)
                 // Calculate the differences in energies using the delta over the tick
                 val deltaEL = leftShaft.energy - preEnergy[LEFT]
                 val deltaER = rightShaft.energy - preEnergy[RIGHT]
                 val deltaET = deltaEL + deltaER
                 val deltaELS = (leftShaft.mass / mass) * deltaET
                 val deltaERS = (rightShaft.mass / mass) * deltaET
                 val deltaED = deltaELS - deltaEL
                 // ...this calculation is done from the perspective of the left shaft--it can be done from the right wolog
                 // Clamp energy based on max transfer, slip if needed
                 if (deltaED < -maxE) {
                     Utils.println(String.format("CPP.p: returning to slipping (deltaED=%f)", deltaED))
                     slipping = true
                 }
                 if (deltaED > maxE) {
                     Utils.println(String.format("CPP.p: returning to slipping (deltaED=%f)", deltaED))
                     slipping = true
                 }
                // val chgEL = (leftShaft.mass / mass) * deltaE
                // val chgER = (rightShaft.mass / mass) * deltaE
                 Utils.println(String.format("CPP.p: not slipping, deltaELS=%f, deltaERS=%f", deltaELS, deltaERS))
                 leftShaft.energy += deltaELS
                 rightShaft.energy += deltaERS
             }
             */
            if(!slipping) {
                val maxE = clutching * maxStaticEnergyF.getValue(wear)
                val energy = leftShaft.energy + rightShaft.energy
                val leftEnergy = energy
                val rightEnergy = energy
                val flow = leftShaft.energy - rightShaft.energy - preEnergy[LEFT] + preEnergy[RIGHT]
                Utils.println(String.format("CPP.p: flow=%f", flow))
                if(Math.abs(flow) > maxE) {
                    leftShaft.energy -= Math.signum(flow) * maxE
                    rightShaft.energy += Math.signum(flow) * maxE
                    Utils.println(String.format("CPP.p: started slipping (maxE=%f)", maxE))
                    slipping = true
                    needPublish()
                } else {
                    leftShaft.rads = Math.sqrt(2 * leftEnergy / mass)
                    rightShaft.rads = Math.sqrt(2 * rightEnergy / mass)
                }
            }
            /*
            if (!slipping) {
                val totalEnergy = leftShaft.energy + rightShaft.energy
                val preTotalEnergy = preEnergy[LEFT] + preEnergy[RIGHT]
                val preEnergyDifferential = preEnergy[LEFT] - preEnergy[RIGHT]
                var energyDifferential = leftShaft.energy - rightShaft.energy
                val differentialDifferential = energyDifferential - preEnergyDifferential
                val combinedSystemW = Math.sqrt(2 * totalEnergy / mass)
                leftShaft.rads = combinedSystemW
                rightShaft.rads = combinedSystemW
                Utils.println(String.format("CPP.p: combinedSystemW=%f", combinedSystemW))
                val thresholdEnergy =maxStaticEnergyF.getValue(wear)
                if (Math.abs(thresholdEnergy) < Math.abs(differentialDifferential))
                    slipping = true
            }
            */
        }
    }

    init {
        electricalLoadList.add(inputGate)
        slowPostProcessList.add(ClutchPostProcess())
    }

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(front.left(), front.right())

    override fun connectedOnSide(direction: Direction, net: ShaftNetwork) {
        connectedSides.add(direction)
        needPublish()
    }

    override fun disconnectedOnSide(direction: Direction, net: ShaftNetwork?) {
        connectedSides.remove(direction)
        needPublish()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        connectedSides.serialize(stream)
        stream.writeFloat(leftShaft.rads.toFloat())
        stream.writeFloat(rightShaft.rads.toFloat())
        stream.writeFloat(inputGate.normalized.toFloat())
        stream.writeBoolean(slipping)
    }

    /*
    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        connectedNetworks.forEach {
            var shaftTag = NBTTagCompound()
            it.value.writeToNBT(shaftTag, "shaft")
            nbt.setTag("side" + it.key.toSideValue().toString(), shaftTag)
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        connectedNetworks.clear()
        nbt.func_150296_c().forEach {
            val str = it as String
            if(str.startsWith("side")) {
                val shaftTag = nbt.getCompoundTag(str)
                val net = ShaftNetwork()
                net.readFromNBT(shaftTag, "shaft")
                net.rebuildNetwork()
                connectedNetworks.put(
                    Direction.fromInt(str.substring(4).toInt()),
                    net
                )
            }
        }
    }
    */

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        connectedSides.writeToNBT(nbt, "sides")
        leftShaft.writeToNBT(nbt, "leftShaft")
        rightShaft.writeToNBT(nbt, "rightShaft")
        nbt.setBoolean("slipping", slipping)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        connectedSides.readFromNBT(nbt, "sides")
        leftShaft.readFromNBT(nbt, "leftShaft")
        rightShaft.readFromNBT(nbt, "rightShaft")
        slipping = nbt.getBoolean("slipping")
        // Utils.println(String.format("CE.rFN: left %s r=%f, right %s r=%f", leftShaft, leftShaft.rads, rightShaft, rightShaft.rads))
    }

    override fun getWaila(): MutableMap<String, String> {
        val info = mutableMapOf<String, String>()
        val entries = mapOf(Pair(front.left(), leftShaft), Pair(front.right(), rightShaft)).entries
        info.put("Speeds", entries.map {
            Utils.plotRads("", it.value.rads)
        }.joinToString(", "))
        info.put("Energies", entries.map {
            Utils.plotEnergy("", it.value.energy)
        }.joinToString(", "))
        info.put("Masses", entries.map {
            Utils.plotValue(it.value.mass * 1000, "g")
        }.joinToString(", "))
        val desc = clutchPlateDescriptor
        val stack = clutchPlateStack
        if(desc != null && stack != null)
            info.put("Wear", String.format("%.6f", desc.getWear(stack)))
        info.put("Clutching", Utils.plotVolt(inputGate.bornedU))
        info.put("Slipping", if(slipping) { "YES" } else { "NO" })
        return info
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun thermoMeterString(side: Direction?): String? = null
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean = false
}

class ClutchRender(entity: TransparentNodeEntity, desc_: TransparentNodeDescriptor) : ShaftRender(entity, desc_) {
    val desc = desc_ as ClutchDescriptor
    val connectedSides = DirectionSet()
    override val cableRender = Eln.instance.stdCableRenderSignal
    val inv = TransparentNodeElementInventory(1, 1, this)
    override fun getInventory() = inv

    var lRads = 0.0
    var rRads = 0.0
    val lLogRads: Double
        get() {
            return Math.log(lRads + 1) / Math.log(1.2)
        }
    val rLogRads: Double
        get() {
            return Math.log(rRads + 1) / Math.log(1.2)
        }
    var lAngle = 0.0
    var rAngle = 0.0

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        lAngle += deltaT * lLogRads
        rAngle += deltaT * rLogRads
        volumeSetting.step(deltaT)
    }

    override fun draw() {
        preserveMatrix {
            front.glRotateXnRef()
            val angSign = when (front) {
                Direction.XP, Direction.ZP -> 1.0
                else -> -1.0
            }
            desc.draw(lAngle * angSign, rAngle * angSign)
        }

        preserveMatrix {
            if (cableRefresh) {
                cableRefresh = false;
                connectionType = CableRender.connectionType(tileEntity, eConn, front.down())
            }

            glCableTransforme(front.down());
            cableRender!!.bindCableTexture();

            for (lrdu in LRDU.values()) {
                Utils.setGlColorFromDye(connectionType!!.otherdry[lrdu.toInt()])
                if (!eConn.get(lrdu)) continue
                mask.set(1.shl(lrdu.ordinal))
                CableRender.drawCable(cableRender, mask, connectionType)
            }
        }
    }

    inner class ClutchLoopedSound(sound: String, coord: Coordonate) : LoopedSound(sound, coord) {
        override fun getPitch() = Math.max(0.1, Math.min(1.5, Math.abs(lRads - rRads) / 200.0)).toFloat()
        override fun getVolume() = volumeSetting.position
    }

    init {
        addLoopedSound(ClutchLoopedSound(desc.slipSound, coordonate()))
        LRDU.values().forEach { eConn.set(it, true); mask.set(it, true) }
        volumeSetting.target = 0f
    }

    var clutching = 0.0
    var slipping = false
    var lastSlipping = false

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        connectedSides.deserialize(stream)
        lRads = stream.readFloat().toDouble()
        rRads = stream.readFloat().toDouble()
        clutching = stream.readFloat().toDouble()
        slipping = stream.readBoolean()
        if(slipping) {
            volumeSetting.target = (Math.min(1.0, Math.abs(lRads - rRads) / 20.0) * clutching * 0.5).toFloat()
        } else {
            volumeSetting.position = 0f
        }
        if(lastSlipping && !slipping) play(SoundCommand(desc.slipStopSound))
        lastSlipping = slipping
        Utils.println(String.format("CR.nU: l=%f,r=%f c=%f s=%s ls=%s", lRads, rRads, clutching, slipping, lastSlipping))
    }

    override fun newGuiDraw(side: Direction?, player: EntityPlayer?): GuiScreen? = ClutchGui(player, inv, this)
}

class ClutchContainer(player: EntityPlayer?, inv: IInventory) : BasicContainer(
    player, inv, arrayOf(
        GenericItemUsingDamageSlot(inv, 0, 176 / 2 - 16 / 2 + 4, 42 - 16 / 2, 1, ClutchPlateItem::class.java, ISlotSkin.SlotSkin.medium, arrayOf(tr("Clutch Plate")))
    )
)

class ClutchGui(player: EntityPlayer?, inv: IInventory, val render: ClutchRender) : GuiContainerEln(ClutchContainer(player, inv)) {
    override fun newHelper() = HelperStdContainer(this)
}

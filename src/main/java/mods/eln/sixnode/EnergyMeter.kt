package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.gui.*
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.ResistorSwitch
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import mods.eln.sound.SoundCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.lwjgl.opengl.GL11
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.IllegalStateException
import java.text.NumberFormat
import java.text.ParseException
import java.util.HashMap
import java.util.concurrent.ConcurrentLinkedQueue

class EnergyMeterDescriptor(name: String, private val obj: Obj3D?, energyWheelCount: Int, timeWheelCount: Int) : SixNodeDescriptor(name, EnergyMeterElement::class.java, EnergyMeterRender::class.java) {
    var base: Obj3DPart? = null
    var powerDisk: Obj3DPart? = null
    var energySignWheel: Obj3DPart? = null
    var timeUnitWheel: Obj3DPart? = null
    var energyUnitWheel: Obj3DPart? = null
    var energyNumberWheel: Array<Obj3DPart?>? = null
    var timeNumberWheel: Array<Obj3DPart?>? = null
    var pinDistance: FloatArray

    init {
        if (obj != null) {
            base = obj.getPart("Base")
            powerDisk = obj.getPart("PowerDisk")
            energySignWheel = obj.getPart("EnergySignWheel")
            timeUnitWheel = obj.getPart("TimeUnitWheel")
            energyUnitWheel = obj.getPart("EnergyUnitWheel")

            energyNumberWheel = arrayOfNulls(energyWheelCount)
            for (idx in energyNumberWheel!!.indices) {
                energyNumberWheel!![idx] = obj.getPart("EnergyNumberWheel$idx")
            }
            timeNumberWheel = arrayOfNulls(timeWheelCount)
            for (idx in timeNumberWheel!!.indices) {
                timeNumberWheel!![idx] = obj.getPart("TimeNumberWheel$idx")
            }
        }

        pinDistance = Utils.getSixNodePinDistance(base)

        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper?): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper?): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw(13896.0, 1511.0, 1, 0, true)
        }
    }

    fun draw(energy: Double, time: Double, energyUnit: Int, timeUnit: Int, drawAll: Boolean) {
        var energy = energy
        var time = time
        // UtilsClient.disableCulling();
        base!!.draw()
        powerDisk!!.draw(-energy.toFloat(), 0f, 1f, 0f)

        run {
            // render energy
            val ox = 0.20859f
            val oy = 0.15625f
            val oz = 0f
            var delta = 0.0
            var propagate = true

            if (drawAll) {
                run {
                    var rot: Double
                    if (energy > 0.5)
                        rot = 0.0
                    else if (energy < -0.5)
                        rot = 1.0
                    else
                        rot = 0.5 - energy
                    rot *= 36.0
                    GL11.glPushMatrix()
                    GL11.glTranslatef(ox, oy, oz)
                    GL11.glRotatef(rot.toFloat(), 0f, 0f, 1f)
                    GL11.glTranslatef(-ox, -oy, -oz)
                    energySignWheel!!.draw()
                    GL11.glPopMatrix()
                }
                if (energyUnitWheel != null) {
                    val rot = (energyUnit * 36).toDouble()
                    GL11.glPushMatrix()
                    GL11.glTranslatef(ox, oy, oz)
                    GL11.glRotatef(rot.toFloat(), 0f, 0f, 1f)
                    GL11.glTranslatef(-ox, -oy, -oz)
                    energyUnitWheel!!.draw()
                    GL11.glPopMatrix()
                }

                energy = Math.max(0.0, Math.abs(energy))
                if (energy < 5) propagate = false

                for (idx in energyNumberWheel!!.indices) {
                    var rot = energy % 10 + 0.0
                    rot += 0.00

                    if (idx == 1) {
                        delta = rot % 1 * 2 - 1
                        delta *= delta * delta
                        delta *= 0.5
                    }
                    if (idx != 0) {
                        if (propagate) {
                            if (rot < 9.5 && rot > 0.5) {
                                propagate = false
                            }
                            rot = rot.toInt() + delta
                        } else
                            rot = rot.toInt().toDouble()
                    }

                    rot *= 36.0
                    GL11.glPushMatrix()
                    GL11.glTranslatef(ox, oy, oz)
                    GL11.glRotatef(rot.toFloat(), 0f, 0f, 1f)
                    GL11.glTranslatef(-ox, -oy, -oz)
                    energyNumberWheel!![idx]!!.draw()
                    GL11.glPopMatrix()

                    energy /= 10.0
                }
            }
        }

        if (energyNumberWheel!!.size != 0) { // Render Times
            val ox = 0.20859f
            val oy = 0.03125f
            val oz = 0f
            var delta = 0.0
            var propagate = true

            if (drawAll) {
                if (timeUnitWheel != null) {
                    val rot = (timeUnit * 36).toDouble()
                    GL11.glPushMatrix()
                    GL11.glTranslatef(ox, oy, oz)
                    GL11.glRotatef(rot.toFloat(), 0f, 0f, 1f)
                    GL11.glTranslatef(-ox, -oy, -oz)
                    timeUnitWheel!!.draw()
                    GL11.glPopMatrix()
                }

                time = Math.max(0.0, Math.abs(time))
                if (time < 5) propagate = false

                for (idx in timeNumberWheel!!.indices) {
                    var rot = time % 10 + 0.0
                    rot += 0.00

                    if (idx == 1) {
                        delta = rot % 1 * 2 - 1
                        delta *= delta * delta
                        delta *= delta * delta
                        delta *= delta * delta
                        delta *= 0.5
                    }
                    if (idx != 0) {
                        if (propagate) {
                            if (rot < 9.5 && rot > 0.5) {
                                propagate = false
                            }
                            rot = rot.toInt() + delta
                        } else
                            rot = rot.toInt().toDouble()
                    }

                    rot *= 36.0
                    GL11.glPushMatrix()
                    GL11.glTranslatef(ox, oy, oz)
                    GL11.glRotatef(rot.toFloat(), 0f, 0f, 1f)
                    GL11.glTranslatef(-ox, -oy, -oz)
                    timeNumberWheel!![idx]!!.draw()
                    GL11.glPopMatrix()

                    time /= 10.0
                }
            }
        }
        // UtilsClient.enableCulling();
    }
}

data class Webhook (val value: Double, val name: String, val webhook: String, val type: String)

class EnergyMeterElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {

    internal var voltageWatchDogA = VoltageStateWatchDog()
    internal var voltageWatchDogB = VoltageStateWatchDog()
    // ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    internal val switchStateQueue = ConcurrentLinkedQueue<Boolean>()

    internal var slowProcess = SlowProcess()
    var descriptor: EnergyMeterDescriptor
    var aLoad = NbtElectricalLoad("aLoad")
    var bLoad = NbtElectricalLoad("bLoad")
    var shunt = ResistorSwitch("shunt", aLoad, bLoad)

    internal var inventory = SixNodeElementInventory(1, 64, this)

    internal var energyUnit = 1
    internal var timeUnit = 0

    var cableDescriptor: GenericCableDescriptor? = null

    internal var password = ""
    internal var meterName = ""
    internal var meterWebhook = ""
    internal var energyStack = 0.0
    internal var timeCounter = 0.0

    internal var mod = Mod.ModCounter

    internal enum class Mod {
        ModCounter, ModPrepay
    }

    internal val webhookThread: Thread

    init {
        shunt.mustUseUltraImpedance()

        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(shunt)
        val ra = Resistor(aLoad, null)
        ra.pullDown()
        val rb = Resistor(bLoad, null)
        rb.pullDown()
        electricalComponentList.add(ra)
        electricalComponentList.add(rb)

        slowProcessList.add(slowProcess)

        val exp = WorldExplosion(this).cableExplosion()

        // slowProcessList.add(currentWatchDog);
        slowProcessList.add(voltageWatchDogA)
        slowProcessList.add(voltageWatchDogB)

        // currentWatchDog.set(shunt).set(exp);
        voltageWatchDogA.set(aLoad).set(exp)
        voltageWatchDogB.set(bLoad).set(exp)
        this.descriptor = descriptor as EnergyMeterDescriptor

        class WebhookThreadClass: Runnable {
            override fun run() {
                // wait a random amount of time into the frequency to start (prevent lag)
                val delay = Math.round(Math.random() * 1000 * Eln.energyMeterWebhookFrequency)
                DP.println(DPType.NETWORK,"Started webhook thread, waiting " + delay + "ms")
                Thread.sleep(delay)
                while(Eln.simulator.isRunning) {
                    try {
                        // sends the energy usage at a specific rate. Disabled if name or webhook isn't specified..
                        if (meterWebhook.isNotEmpty() && meterName.isNotEmpty()) {
                            DP.println(DPType.NETWORK, "Sending webhook from Energy Meter to " + meterWebhook)
                            sendEnergy(timeCounter, meterName, meterWebhook)
                            if (descriptor.timeNumberWheel!!.isNotEmpty()) {
                                sendTime(energyStack, meterName, meterWebhook)
                            }
                        }
                        Thread.sleep(Integer.toUnsignedLong(Eln.energyMeterWebhookFrequency * 1000))
                    } catch (ie: InterruptedException) {}
                }
                DP.println(DPType.NETWORK, "Stopping webhook thread")
            }
        }

        // new thread spawns to handle the TCP connection
        // Should be thread-safe.
        webhookThread = Thread(WebhookThreadClass())

        if (Eln.energyMeterWebhookFrequency >= 15)
            DP.println(DPType.NETWORK, "Enabling Energy Meter Webhook Service")
            webhookThread.start()
    }

    /**
     * sendEnergy(energy, name, webhook) - Send the energy data to the server, and also change state if codes request it
     * @param energy the current energy reading from the meter
     * @param name the name of the meter
     * @param webhook the server the meter wants to contact
     */
    private fun sendEnergy(energy: Double, name: String, webhook: String) {
        try {
            val url = webhook + "?name=" + name.replace(" ", "_").replace("&", "") + "&energy=" + java.lang.Double.toString(energy)
            // probe the server with the most recent energy information.
            val client = HttpClientBuilder.create().build()
            val resp = client.execute(HttpGet(url))
            val code = resp.statusLine.statusCode
            if (code == 202) {
                switchStateQueue.offer(true)
            } else if (code == 402) {
                switchStateQueue.offer(false)
            } else if (code == 205) {
                energyStack = 0.0
                timeCounter = 0.0
                needPublish()
            }
            resp.close()
            client.close()
        } catch (ise: IllegalStateException) {
            DP.println(DPType.NETWORK,"Webhook URL is invalid!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * sendTime(time, name, webhook) - Sends the time of the meter to a server.
     * @param time The time the meter has been on
     * @param name The name of the meter
     * @param webhook The webhook the meter wants things sent to
     */
    private fun sendTime(time: Double, name: String, webhook: String) {
        try {
            val url = webhook + "?name=" + name.replace(" ", "_").replace("&", "") + "&time=" + java.lang.Double.toString(time)
            // probe the server with the most recent time information.
            val client = HttpClientBuilder.create().build()
            val resp = client.execute(HttpGet(url))
            val code = resp.statusLine.statusCode
            if (code == 202) {
                switchStateQueue.offer(true)
            }else if(code == 402) {
                switchStateQueue.offer(false)
            }else if(code == 205) {
                energyStack = 0.0
                timeCounter = 0.0
                needPublish()
            }
            resp.close()
            client.close()
        } catch (ise: IllegalStateException) {
            DP.println(DPType.NETWORK,"Webhook URL is invalid!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getInventory(): SixNodeElementInventory? {
        return inventory
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (front == lrdu) return aLoad
        return if (front.inverse() == lrdu) bLoad else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (inventory.getStackInSlot(EnergyMeterContainer.cableSlotId) == null) return 0
        if (front == lrdu) return NodeBase.MASK_ELECTRICAL_ALL
        return if (front.inverse() == lrdu) NodeBase.MASK_ELECTRICAL_ALL else 0

    }

    override fun multiMeterString(): String {
        return Utils.plotVolt("Ua:", aLoad.u) + Utils.plotVolt("Ub:", bLoad.u) + Utils.plotVolt("I:", aLoad.current)
    }

    override fun getWaila(): Map<String, String>? {
        val info = HashMap<String, String>()
        info[I18N.tr("Power")] = Utils.plotPower("", aLoad.u * aLoad.i)
        when (mod) {
            Mod.ModCounter -> {
                info[I18N.tr("Mode")] = I18N.tr("Counter")
                info[I18N.tr("Energy")] = Utils.plotEnergy("", energyStack)
                if (descriptor.timeNumberWheel!!.isNotEmpty()) {
                    var time: Double
                    var unit: String
                    if (timeUnit == 0) {
                        time = timeCounter / 360
                        unit = "Hours"
                    }else{
                        time = timeCounter / 8640
                        unit = "Days"
                    }
                    info[I18N.tr("Time")] = String.format("%.1f %s", time, unit)
                }
            }

            Mod.ModPrepay -> {
                info[I18N.tr("Mode")] = I18N.tr("Prepay")
                info[I18N.tr("Energy left")] = Utils.plotEnergy("", energyStack)
            }
        }

        return info
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(shunt.state)
            stream.writeUTF(password)
            stream.writeUTF(mod.toString())
            // stream.writeDouble(timeCounter)
            // stream.writeDouble(energyStack)
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(EnergyMeterContainer.cableSlotId))

            stream.writeByte(energyUnit)
            stream.writeByte(timeUnit)
            stream.writeUTF(meterName)
            stream.writeUTF(meterWebhook)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun setSwitchState(state: Boolean) {
        if (state == shunt.state) return
        //	if (energyStack <= 0 && mod == Mod.ModPrepay) return;
        shunt.state = state
        play(SoundCommand("random.click").mulVolume(0.3f, 0.6f).smallRange())
        needPublish()
    }

    override fun initialize() {
        computeElectricalLoad()
    }

    override fun inventoryChanged() {
        computeElectricalLoad()
        reconnect()
    }

    fun computeElectricalLoad() {
        val cable = inventory.getStackInSlot(EnergyMeterContainer.cableSlotId)

        if (Eln.sixNodeItem.getDescriptor(cable) != null) {
            cableDescriptor = Eln.sixNodeItem.getDescriptor(cable) as GenericCableDescriptor
        }
        if (cableDescriptor == null) {
            aLoad.highImpedance()
            bLoad.highImpedance()

            voltageWatchDogA.disable()
            voltageWatchDogB.disable()
            // currentWatchDog.disable();
        } else {
            cableDescriptor!!.applyTo(aLoad)
            cableDescriptor!!.applyTo(bLoad)

            voltageWatchDogA.setUNominalMirror(cableDescriptor!!.electricalNominalVoltage)
            voltageWatchDogB.setUNominalMirror(cableDescriptor!!.electricalNominalVoltage)
            // currentWatchDog.setIAbsMax(cableDescriptor.electricalMaximalCurrent);
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            when (stream.readByte()) {
                clientEnergyStackId -> {
                    energyStack = stream.readDouble()
                    slowProcess.publishTimeout = -1.0
                }
                clientTimeCounterId -> {
                    timeCounter = 0.0
                    needPublish()
                }
                clientModId -> {
                    mod = Mod.valueOf(stream.readUTF())
                    needPublish()
                }
                clientPasswordId -> {
                    password = stream.readUTF()
                    needPublish()
                }
                clientToggleStateId -> setSwitchState(!shunt.state)
                clientEnergyUnitId -> {
                    energyUnit++
                    if (energyUnit > 3) energyUnit = 0
                    needPublish()
                }
                clientTimeUnitId -> {
                    timeUnit++
                    if (timeUnit > 1) timeUnit = 0
                    needPublish()
                }
                clientNameId -> {
                    meterName = stream.readUTF()
                    needPublish()
                }
                clientWebhookId -> {
                    meterWebhook = stream.readUTF()
                    needPublish()
                }
            }// needPublish();
        } catch (e: IOException) {
            // e.printStackTrace();
        }

    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return EnergyMeterContainer(player, inventory)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)

        try {
            mod = Mod.valueOf(nbt.getString("mode"))
        } catch (e: Exception) {
            mod = Mod.ModCounter
        }

        energyStack = nbt.getDouble("energyStack")
        timeCounter = nbt.getDouble("timeCounter")
        password = nbt.getString("password")
        slowProcess.oldEnergyPublish = energyStack
        energyUnit = nbt.getByte("energyUnit").toInt()
        timeUnit = nbt.getByte("timeUnit").toInt()
        meterName = nbt.getString("meterName")
        meterWebhook = nbt.getString("webhook")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)

        nbt.setString("mode", mod.toString())
        nbt.setDouble("energyStack", energyStack)
        nbt.setDouble("timeCounter", timeCounter)
        nbt.setString("password", password)
        nbt.setByte("energyUnit", energyUnit.toByte())
        nbt.setByte("timeUnit", timeUnit.toByte())
        nbt.setString("meterName", meterName)
        nbt.setString("webhook", meterWebhook)
    }

    internal inner class SlowProcess : IProcess {
        var publishTimeout = Math.random() * publishTimeoutReset
        var oldEnergyPublish: Double = 0.toDouble()

        /**
         * process() This effectively happens once per tick in an MNA step. Take as little time here as possible.
         */
        override fun process(time: Double) {
            while (!switchStateQueue.isEmpty()) {
                val state = switchStateQueue.poll()
                if (state != null) {
                    setSwitchState(state)
                }
            }
            timeCounter += time * 72.0
            val p = aLoad.current * aLoad.u * if (aLoad.u > bLoad.u) 1.0 else -1.0
            var highImp = false
            when (mod) {
                Mod.ModCounter -> energyStack += p * time
                Mod.ModPrepay -> {
                    energyStack -= p * time
                    if (energyStack < 0) {
                        // energyStack = 0;
                        // setSwitchState(false);
                        if (p > 0) {
                            highImp = true
                        }
                    }
                }
            }

            if (highImp)
                shunt.ultraImpedance()
            else
                Eln.applySmallRs(shunt)

            publishTimeout -= time
            if (publishTimeout < 0) {
                publishTimeout += publishTimeoutReset
                val bos = ByteArrayOutputStream(64)
                val packet = DataOutputStream(bos)

                preparePacketForClient(packet)

                try {
                    packet.writeByte(serverPowerId.toInt())
                    packet.writeDouble(oldEnergyPublish)
                    packet.writeDouble((energyStack - oldEnergyPublish) / publishTimeoutReset)

                    sendPacketToAllClient(bos, 10.0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val bos2 = ByteArrayOutputStream(64)
                val packet2 = DataOutputStream(bos2)

                preparePacketForClient(packet2)

                try {
                    packet2.writeByte(serverHoursId.toInt())
                    packet2.writeDouble(timeCounter)

                    sendPacketToAllClient(bos2, 10.0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                oldEnergyPublish = energyStack
            }
        }
    }

    companion object {
        const val publishTimeoutReset = 1.0

        // These variables are for the client/server communication over the network.

        const val clientEnergyStackId: Byte = 1
        const val clientModId: Byte = 2
        const val clientPasswordId: Byte = 3
        const val clientToggleStateId: Byte = 4
        const val clientTimeCounterId: Byte = 5
        const val clientEnergyUnitId: Byte = 6
        const val clientTimeUnitId: Byte = 7
        const val clientNameId: Byte = 8
        const val clientWebhookId: Byte = 9

        const val serverPowerId: Byte = 1
        const val serverHoursId: Byte = 2
    }
}

class EnergyMeterRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {

    internal var inventory = SixNodeElementInventory(1, 64, this)
    internal var descriptor: EnergyMeterDescriptor

    internal var timerCounter: Double = 0.toDouble()
    internal var energyStack: Double = 0.toDouble()
    internal var switchState: Boolean = false
    internal var password: String = ""
    internal var meterName = ""
    internal var meterWebhook = ""
    internal var mod: EnergyMeterElement.Mod = EnergyMeterElement.Mod.ModCounter

    internal var energyUnit: Int = 0
    internal var timeUnit: Int = 0

    internal var cableRender: CableRenderDescriptor? = null

    internal var power: Double = 0.toDouble()
    internal var error: Double = 0.toDouble()
    internal var serverPowerIdTimer = EnergyMeterElement.publishTimeoutReset * 34

    init {
        this.descriptor = descriptor as EnergyMeterDescriptor
    }

    override fun draw() {
        super.draw()

        GL11.glPushMatrix()

        var pinDistances = descriptor.pinDistance
        if (side.isY) {
            pinDistances = front.rotate4PinDistances(pinDistances)
            front.left().glRotateOnX()
        }

        descriptor.draw(energyStack / Math.pow(10.0, (energyUnit * 3 - 1).toDouble()), timerCounter / if (timeUnit == 0) 360 else 8640,
            energyUnit, timeUnit,
            UtilsClient.distanceFromClientPlayer(tileEntity) < 20)

        GL11.glPopMatrix()

        GL11.glColor3f(0.9f, 0f, 0f)
        drawPowerPinWhite(front, pinDistances)
        GL11.glColor3f(0f, 0f, 0.9f)
        drawPowerPinWhite(front.inverse(), pinDistances)
        GL11.glColor3f(1f, 1f, 1f)
    }

    override fun refresh(deltaT: Float) {
        val errorComp = error * 1.0 * deltaT.toDouble()
        energyStack += power * deltaT + errorComp
        error -= errorComp
        timerCounter += (deltaT * 72).toDouble()
        serverPowerIdTimer += deltaT.toDouble()
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return cableRender
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)

        try {
            switchState = stream.readBoolean()
            password = stream.readUTF()
            mod = EnergyMeterElement.Mod.valueOf(stream.readUTF())
            // timerCounter = stream.readDouble()
            // energyStack = stream.readDouble();
            val rdesc = GenericCableDescriptor.getDescriptor(Utils.unserialiseItemStack(stream), GenericCableDescriptor::class.java)
            if (rdesc != null) {
                val desc =  rdesc as GenericCableDescriptor
                cableRender = desc.render
            }

            energyUnit = stream.readByte().toInt()
            timeUnit = stream.readByte().toInt()
            meterName = stream.readUTF()
            meterWebhook = stream.readUTF()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen? {
        return EnergyMeterGui(player, inventory, this)
    }

    @Throws(IOException::class)
    override fun serverPacketUnserialize(stream: DataInputStream) {
        super.serverPacketUnserialize(stream)
        when (stream.readByte()) {
            EnergyMeterElement.serverPowerId -> {
                if (serverPowerIdTimer > EnergyMeterElement.publishTimeoutReset * 3) {
                    energyStack = stream.readDouble()
                    error = 0.0
                } else {
                    error = stream.readDouble() - energyStack
                }
                power = stream.readDouble()
                serverPowerIdTimer = 0.0
            }
            EnergyMeterElement.serverHoursId -> {
                timerCounter = stream.readDouble()
            }
            else -> {
            }
        }
    }
}

class EnergyMeterGui(player: EntityPlayer, inventory: IInventory, internal var render: EnergyMeterRender) : GuiContainerEln(EnergyMeterContainer(player, inventory)) {

    // I would love to set these to not be null, but the initialization step has to happen in initGui() after super.initGui()
    internal var stateBt: GuiButtonEln? = null
    internal var passwordBt: GuiButtonEln? = null
    internal var modeBt: GuiButtonEln? = null
    internal var setEnergyBt: GuiButtonEln? = null
    internal var resetTimeBt: GuiButtonEln? = null
    internal var energyUnitBt: GuiButtonEln? = null
    internal var timeUnitBt: GuiButtonEln? = null
    internal var passwordField: GuiTextFieldEln? = null
    internal var energyField: GuiTextFieldEln? = null
    internal var webhookField: GuiTextFieldEln? = null
    internal var nameField: GuiTextFieldEln? = null

    internal var isLogged: Boolean = false

    /**
     * initGui() - prepares the GUI fields placement, sets initial values, comments, etc.
     */
    override fun initGui() {
        super.initGui()

        // Left side of GUI
        passwordField = newGuiTextField(6, 7, 70)
        stateBt = newGuiButton(6, 25, 70, 14, "")
        energyField = newGuiTextField(6, 40, 70)
        energyUnitBt = newGuiButton(6, 53, 34, 14,"")
        timeUnitBt = newGuiButton(42, 53, 34, 14,"")

        // Right side of GUI
        passwordBt = newGuiButton(80, 6, 106, 14, "")
        modeBt = newGuiButton(80, 25, 106, 14, "")
        setEnergyBt = newGuiButton(80, 39, 106, 14, I18N.tr("Set energy counter"))
        resetTimeBt = newGuiButton(80, 53, 106, 14, I18N.tr("Reset time counter"))

        // Bottom segment of GUI
        nameField = newGuiTextField(6, 70, 176)
        webhookField = newGuiTextField(6, 84, 176)


        isLogged = render.password == ""
        passwordField!!.setComment(0, I18N.tr("Enter password"))
        energyField!!.setComment(0, I18N.tr("Enter new energy"))
        energyField!!.setComment(1, I18N.tr("value in kJ"))
        nameField!!.setComment(0, I18N.tr("Name of the meter"))
        webhookField!!.setComment(0, I18N.tr("Webhook URL to send meter info to"))
        energyField!!.text = "0"

        if (render.descriptor.timeNumberWheel!!.size == 0) {
            energyUnitBt!!.enabled = false
            timeUnitBt!!.enabled = false
        }

        nameField!!.text = render.meterName
        webhookField!!.text = render.meterWebhook
    }

    /**
     * guiObjectEvent() - every time an action (click, press enter, etc) happens on the GUI, this function triggers.
     */
    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)

        if (`object` === stateBt) {
            render.clientSend(EnergyMeterElement.clientToggleStateId.toInt())
        }
        if (`object` === passwordBt) {
            if (isLogged) {
                render.clientSetString(EnergyMeterElement.clientPasswordId, passwordField!!.text)
            } else {
                if (passwordField!!.text == render.password) {
                    isLogged = true
                }
            }
        }

        if (`object` === modeBt) {
            when (render.mod) {
                EnergyMeterElement.Mod.ModCounter -> render.clientSetString(EnergyMeterElement.clientModId, EnergyMeterElement.Mod.ModPrepay.name)
                EnergyMeterElement.Mod.ModPrepay -> render.clientSetString(EnergyMeterElement.clientModId, EnergyMeterElement.Mod.ModCounter.name)
            }
        }

        if (`object` === setEnergyBt) {
            val newVoltage: Double
            try {
                newVoltage = NumberFormat.getInstance().parse(energyField!!.text).toDouble()
            } catch (e: ParseException) {
                return
            }

            render.clientSetDouble(EnergyMeterElement.clientEnergyStackId, newVoltage * 1000)
        }

        if (`object` === resetTimeBt) {
            render.clientSend(EnergyMeterElement.clientTimeCounterId.toInt())
        }
        if (`object` === energyUnitBt) {
            render.clientSend(EnergyMeterElement.clientEnergyUnitId.toInt())
        }
        if (`object` === timeUnitBt) {
            render.clientSend(EnergyMeterElement.clientTimeUnitId.toInt())
        }
        if (`object` == nameField) {
            render.meterName = nameField!!.text
            render.clientSetString(EnergyMeterElement.clientNameId, render.meterName)
        }
        if (`object` == webhookField) {
            render.meterWebhook = webhookField!!.text
            render.clientSetString(EnergyMeterElement.clientWebhookId, render.meterWebhook)
        }
    }

    /**
     * preDraw() - this prepares the state of the buttons if they unlocked the passowrd, changes state if on/off,
     *  reflects changes in mode, etc.
     */
    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (!render.switchState)
            stateBt!!.displayString = I18N.tr("is off")
        else
            stateBt!!.displayString = I18N.tr("is on")

        if (isLogged)
            passwordBt!!.displayString = I18N.tr("Change password")
        else
            passwordBt!!.displayString = I18N.tr("Try password")

        when (render.mod) {
            EnergyMeterElement.Mod.ModCounter -> {
                modeBt!!.displayString = I18N.tr("Counter Mode")

                modeBt!!.clearComment()
                var lineNumber = 0
                for (line in I18N.tr("Counts the energy conducted from\n\u00a74red\u00a7f to \u00a71blue\u00a7f.")!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    modeBt!!.setComment(lineNumber++, line)
            }
            EnergyMeterElement.Mod.ModPrepay -> {
                modeBt!!.displayString = I18N.tr("Prepay Mode")

                modeBt!!.clearComment()
                var lineNumber = 0
                for (line in I18N.tr("Counts the energy conducted from\n\u00a74red\u00a7f to \u00a71blue\u00a7f.")!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    modeBt!!.setComment(lineNumber++, line)
                modeBt!!.setComment(lineNumber++, "")
                for (line in I18N.tr("You can set an initial\namount of available energy.\nWhen the counter arrives at 0\nthe contact will be opened.")!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    modeBt!!.setComment(lineNumber++, line)
            }
        }

        if (energyUnitBt != null)
            when (render.energyUnit) {
                0 -> energyUnitBt!!.displayString = "J"
                1 -> energyUnitBt!!.displayString = "KJ"
                2 -> energyUnitBt!!.displayString = "MJ"
                3 -> energyUnitBt!!.displayString = "GJ"
                else -> energyUnitBt!!.displayString = "??"
            }

        if (timeUnitBt != null)
            when (render.timeUnit) {
                0 -> timeUnitBt!!.displayString = "H"
                1 -> timeUnitBt!!.displayString = "D"
                else -> timeUnitBt!!.displayString = "??"
            }

        modeBt!!.enabled = isLogged
        stateBt!!.enabled = isLogged
        resetTimeBt!!.enabled = isLogged
        setEnergyBt!!.enabled = isLogged
        energyUnitBt!!.enabled = isLogged && render.descriptor.timeNumberWheel!!.size != 0
        timeUnitBt!!.enabled = isLogged && render.descriptor.timeNumberWheel!!.size != 0
        nameField!!.enabled = isLogged
        webhookField!!.enabled = isLogged
    }

    /**
     * postDraw - draws the lines in the GUI and adds the one or two counters at the bottom (depending on type)
     */
    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        helper.drawRect(6, 21, helper.xSize - 6, 22, -0xbfbfc0)
        helper.drawRect(6, 99, helper.xSize - 6, 100, -0xbfbfc0)
        helper.drawString(6 + 16 / 2, 103, -0x1000000, I18N.tr("Energy counter: %1\$J", render.energyStack.toInt()))
        if (render.descriptor.timeNumberWheel!!.size > 0) {
            if (render.timeUnit == 0) {
                helper.drawString(6 + 16 / 2, 113, -0x1000000, I18N.tr("Time counter: %1\$ Hours", String.format("%.1f", render.timerCounter / 360)))
            } else {
                helper.drawString(6 + 16 / 2, 113, -0x1000000, I18N.tr("Time counter: %1\$ Days", String.format("%.2f", render.timerCounter / 8640)))
            }
        }
    }

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 192, 208, 16, 126)
    }
}


class EnergyMeterContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf<Slot>(
    SixNodeItemSlot(
        inventory, cableSlotId, 160, 106, 1,
        arrayOf<Class<*>>(ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java),
        ISlotSkin.SlotSkin.medium,
        arrayOf(I18N.tr("Electrical cable slot"))))) {
    companion object {
        val cableSlotId = 0
    }
}


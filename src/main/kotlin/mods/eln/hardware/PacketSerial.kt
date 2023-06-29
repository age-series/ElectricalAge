package mods.eln.hardware

import com.fazecast.jSerialComm.SerialPort
import java.io.BufferedWriter
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

private val allocationsList = ConcurrentHashMap<String, ISerial>()
private val readerList = ConcurrentHashMap<ISerial, InputStream>()
private val writerList = ConcurrentHashMap<ISerial, BufferedWriter>()
private val portList = ConcurrentHashMap<ISerial, SerialPort>()

class PacketSerial {

    /**
     * Use this to get the list of available ports.
     */
    val availablePorts: List<String>
        get() {
            return SerialPort.getCommPorts().map { it.systemPortName }
        }

    /**
     * openSerialPort: Open a serial port
     *
     * Call this to gain access to a serial port
     * @param instance the ISerial instance
     */
    @Synchronized fun openSerialPort(instance: ISerial) {
        if (instance.portName in availablePorts) {
            if (instance.portName !in allocationsList.keys) {
                allocationsList[instance.portName] = instance
                setUpSerial(instance)
            }
        }
    }

    /**
     * setUpSerial: Configures the serial port
     *
     * NOTICE - ON A SYNCHRONIZED HOT PATH
     * @param instance the ISerial instance
     */
    private fun setUpSerial(instance: ISerial) {
        val port = SerialPort.getCommPort(instance.portName)
        port.baudRate = instance.baudRate
        portList[instance] = port
        readerList[instance] = port.inputStream
        writerList[instance] = port.outputStream.bufferedWriter(Charsets.UTF_8)
    }

    /**
     * releaseSerialPort: Releases a serial port
     *
     * NOTICE: YOU MUST CALL THIS WHEN YOU ARE DONE WITH A SERIAL PORT!
     * @param instance the ISerial instance
     */
    fun releaseSerialPort(instance: ISerial) {
        allocationsList.remove(instance.portName)
    }

    /**
     * Tick: This should be called by the simulator or Forge
     */
    fun tick() {
        // If the port is closed, give up.
        allocationsList.forEach {
            if (!portList[it.value]!!.isOpen) {
                releaseSerialPort(it.value)
            }
        }
        // Get those packets flowing!
        allocationsList.forEach {
            val t = Thread {
                val start = System.currentTimeMillis()
                val instance = it.value
                val reader = readerList[instance]
                val writer = writerList[instance]
                try {
                    if (reader != null) {
                        // Strictly speaking, this could run forever... We hope not.
                        var run = false
                        do {
                            var packetData = ""
                            if (reader.available() > 0) {
                                val char = reader.read().toChar()
                                if (char != '\n') {
                                    packetData += char
                                } else {
                                    packetData = ""
                                }
                            } else {
                                run = false
                            }

                            if (packetData.length >= 2) {
                                val opcode = packetData[0]
                                val length = packetData[1].code
                                if (length == packetData.length - 2) {
                                    val p = Packet(opcode, packetData.substring(2))
                                    instance.fromDevice.add(p)
                                    // We've had a successful packet decode.
                                    // This is a reasonable spot to pause the packet stream, for this tick.
                                    // I'm setting this to 5ms so that stuff has time to settle in the sim.
                                    if (System.currentTimeMillis() - start > 5) {
                                        run = false
                                    }
                                }
                            }

                        } while (run)
                    }

                } catch (e: Exception) {}
                try {
                    instance.toDevice.forEach {
                        writer?.write(it.serialize())
                    }
                    writer?.flush()
                } catch (e: Exception) {}
            }
            t.start()
        }
    }
}

interface ISerial {
    val portName: String
    val baudRate: Int
    val toDevice: ConcurrentLinkedQueue<Packet>
    val fromDevice: ConcurrentLinkedQueue<Packet>
}

data class Packet(val opcode: Char, val data: String) {
    fun serialize(): String {
        if (data.length > 9) {
            println("Error! Packet data is malformed, data payload was over 9 characters long!")
            println("Data payload: {$data}")
            return ""
        }
        return "$opcode${data.length}$data"
    }
}

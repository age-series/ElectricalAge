package mods.eln.mqtt

import mods.eln.Eln
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.or
import kotlin.math.max

private data class TopicListener(val id: UUID, val callback: (String, ByteArray) -> Unit)

/**
 * Very small MQTT 3.1.1 client implementation tailored for the energy meter use case.
 */
class SimpleMqttClient(private val config: MqttServerConfig) {
    private val logger = Eln.logger
    private val running = AtomicBoolean(true)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "eln-mqtt-${config.name}").apply { isDaemon = true }
    }
    private val topicSubscribers = mutableMapOf<String, CopyOnWriteArrayList<TopicListener>>()
    private val writeLock = Any()
    private val packetId = AtomicInteger(1)

    @Volatile
    private var socket: Socket? = null
    @Volatile
    private var input: DataInputStream? = null
    @Volatile
    private var output: DataOutputStream? = null
    @Volatile
    private var connected = false
    private val keepAliveSeconds = 30
    private var lastPacketSent = System.currentTimeMillis()
    private var lastPacketReceived = System.currentTimeMillis()
    private var clientId = buildClientId()

    init {
        executor.execute { loop() }
    }

    fun publish(topic: String, payload: ByteArray, retain: Boolean = false) {
        if (!running.get()) return
        if (!connected) return
        val topicBytes = encodeString(topic)
        val packet = ByteArray(topicBytes.size + payload.size)
        System.arraycopy(topicBytes, 0, packet, 0, topicBytes.size)
        System.arraycopy(payload, 0, packet, topicBytes.size, payload.size)
        val header = if (retain) 0x31 else 0x30
        sendPacket(header, packet)
    }

    fun subscribe(topic: String, callback: (String, ByteArray) -> Unit): MqttSubscriptionHandle {
        val listener = TopicListener(UUID.randomUUID(), callback)
        synchronized(topicSubscribers) {
            val list = topicSubscribers.getOrPut(topic) { CopyOnWriteArrayList() }
            list.add(listener)
        }
        if (connected) {
            sendSubscribe(topic)
        }
        return MqttSubscriptionHandle { removeSubscription(topic, listener) }
    }

    fun shutdown() {
        if (!running.compareAndSet(true, false)) return
        try {
            sendPacket(0xE0, ByteArray(0))
        } catch (_: Exception) {
        }
        closeSocket()
        executor.shutdownNow()
        synchronized(topicSubscribers) {
            topicSubscribers.clear()
        }
    }

    private fun removeSubscription(topic: String, listener: TopicListener) {
        synchronized(topicSubscribers) {
            val list = topicSubscribers[topic] ?: return
            list.removeIf { it.id == listener.id }
            if (list.isEmpty()) {
                topicSubscribers.remove(topic)
                if (connected) {
                    sendUnsubscribe(topic)
                }
            }
        }
    }

    private fun loop() {
        while (running.get()) {
            try {
                connect()
                processIncoming()
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                logger.warn("[MQTT:${config.name}] ${e.message}")
            } finally {
                connected = false
                closeSocket()
            }
            if (running.get()) {
                try {
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun connect() {
        val uri = URI(config.uri)
        if (uri.scheme == null || uri.scheme.lowercase(Locale.US) != "tcp") {
            throw IOException("Only tcp:// URIs are supported for MQTT meters")
        }
        val host = uri.host ?: throw IOException("Missing host in MQTT URI")
        val port = if (uri.port > 0) uri.port else 1883
        val socket = Socket(host, port)
        socket.keepAlive = true
        socket.soTimeout = 1000
        this.socket = socket
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
        sendConnect()
        lastPacketSent = System.currentTimeMillis()
        lastPacketReceived = System.currentTimeMillis()
    }

    @Throws(IOException::class)
    private fun processIncoming() {
        while (running.get()) {
            try {
                val header = input?.readUnsignedByte() ?: throw IOException("Socket closed")
                val packetType = header shr 4
                val remainingLength = readRemainingLength(input!!)
                val payload = ByteArray(remainingLength)
                input!!.readFully(payload)
                handlePacket(packetType, payload, header)
                lastPacketReceived = System.currentTimeMillis()
            } catch (e: SocketTimeoutException) {
                checkKeepAlive()
            }
        }
    }

    private fun handlePacket(packetType: Int, payload: ByteArray, header: Int) {
        when (packetType) {
            2 -> handleConnAck(payload)
            3 -> handlePublish(header, payload)
            9 -> { /* SUBACK */ }
            13 -> { /* PINGRESP */ }
        }
    }

    private fun handleConnAck(payload: ByteArray) {
        if (payload.size < 2) return
        val returnCode = payload[1].toInt()
        if (returnCode == 0) {
            connected = true
            val topics = synchronized(topicSubscribers) { topicSubscribers.keys.toList() }
            topics.forEach { sendSubscribe(it) }
        } else {
            throw IOException("MQTT broker rejected connection (code=$returnCode)")
        }
    }

    private fun handlePublish(header: Int, payload: ByteArray) {
        val qos = header shr 1 and 0x03
        if (qos > 0) {
            // We only handle QoS 0 inbound messages.
            return
        }
        if (payload.size < 2) return
        val topicLength = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
        if (topicLength + 2 > payload.size) return
        val topic = String(payload, 2, topicLength, StandardCharsets.UTF_8)
        val offset = topicLength + 2
        val message = ByteArray(payload.size - offset)
        if (message.isNotEmpty()) {
            System.arraycopy(payload, offset, message, 0, message.size)
        }
        val listeners = synchronized(topicSubscribers) { topicSubscribers[topic]?.toList() }
        listeners?.forEach { listener ->
            try {
                listener.callback(topic, message)
            } catch (ignored: Exception) {
                logger.warn("[MQTT:${config.name}] Listener threw: ${ignored.message}")
            }
        }
    }

    private fun checkKeepAlive() {
        if (!connected) return
        val now = System.currentTimeMillis()
        if (now - lastPacketSent > keepAliveSeconds * 500L) {
            sendPacket(0xC0, ByteArray(0))
            lastPacketSent = now
        }
        if (now - lastPacketReceived > keepAliveSeconds * 2000L) {
            throw IOException("MQTT keep alive timeout")
        }
    }

    private fun sendConnect() {
        val protocolName = encodeString("MQTT")
        val protocolLevel = byteArrayOf(0x04)
        var connectFlags = 0x02 // Clean session
        val usernameBytes = config.username?.takeIf { it.isNotBlank() }?.let { encodeString(it) }
        val passwordBytes = config.password?.takeIf { it.isNotBlank() }?.let { encodeString(it) }
        if (usernameBytes != null) connectFlags = connectFlags or 0x80
        if (passwordBytes != null) connectFlags = connectFlags or 0x40
        val keepAlive = byteArrayOf((keepAliveSeconds shr 8).toByte(), (keepAliveSeconds and 0xFF).toByte())
        val clientIdBytes = encodeString(clientId)
        val payload = clientIdBytes + (usernameBytes ?: ByteArray(0)) + (passwordBytes ?: ByteArray(0))
        val variableHeader = protocolName + protocolLevel + byteArrayOf(connectFlags.toByte()) + keepAlive
        sendPacket(0x10, variableHeader + payload)
    }

    private fun sendSubscribe(topic: String) {
        val id = nextPacketId()
        val header = ByteArray(2)
        header[0] = (id shr 8).toByte()
        header[1] = (id and 0xFF).toByte()
        val topicBytes = encodeString(topic)
        val payload = ByteArray(topicBytes.size + 1)
        System.arraycopy(topicBytes, 0, payload, 0, topicBytes.size)
        payload[payload.size - 1] = 0 // QoS 0
        sendPacket(0x82, header + payload)
    }

    private fun sendUnsubscribe(topic: String) {
        val id = nextPacketId()
        val header = ByteArray(2)
        header[0] = (id shr 8).toByte()
        header[1] = (id and 0xFF).toByte()
        val topicBytes = encodeString(topic)
        sendPacket(0xA2, header + topicBytes)
    }

    private fun sendPacket(type: Int, data: ByteArray) {
        val stream = output ?: return
        val remaining = encodeLength(data.size)
        val packet = ByteArray(1 + remaining.size + data.size)
        packet[0] = type.toByte()
        System.arraycopy(remaining, 0, packet, 1, remaining.size)
        System.arraycopy(data, 0, packet, 1 + remaining.size, data.size)
        synchronized(writeLock) {
            stream.write(packet)
            stream.flush()
        }
    }

    private fun encodeString(value: String): ByteArray {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val result = ByteArray(bytes.size + 2)
        val len = max(0, bytes.size)
        result[0] = (len shr 8).toByte()
        result[1] = (len and 0xFF).toByte()
        System.arraycopy(bytes, 0, result, 2, bytes.size)
        return result
    }

    private fun encodeLength(length: Int): ByteArray {
        var x = length
        val buffer = ArrayList<Byte>(4)
        do {
            var encodedByte = (x % 128).toByte()
            x /= 128
            if (x > 0) {
                encodedByte = encodedByte or 0x80.toByte()
            }
            buffer.add(encodedByte)
        } while (x > 0 && buffer.size < 4)
        return buffer.toByteArray()
    }

    @Throws(IOException::class)
    private fun readRemainingLength(input: DataInputStream): Int {
        var multiplier = 1
        var value = 0
        var encodedByte: Int
        do {
            encodedByte = input.readUnsignedByte()
            value += (encodedByte and 0x7F) * multiplier
            multiplier *= 128
        } while ((encodedByte and 0x80) != 0 && multiplier <= 128 * 128 * 128)
        return value
    }

    private fun nextPacketId(): Int {
        var id = packetId.getAndIncrement()
        if (id > 0xFFFF) {
            packetId.set(1)
            id = 1
        }
        return id
    }

    private fun buildClientId(): String {
        val sanitized = config.name.lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "")
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val raw = "eln${sanitized.take(8)}$suffix"
        return raw.take(23)
    }

    private fun closeSocket() {
        try {
            input?.close()
        } catch (_: Exception) {
        }
        try {
            output?.close()
        } catch (_: Exception) {
        }
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        input = null
        output = null
        socket = null
    }
}

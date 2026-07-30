package mods.eln.packets

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import io.netty.buffer.ByteBuf
import mods.eln.integration.waila.TransparentNodeWailaEntry
import mods.eln.misc.Coordinate

/**
 * Created by Gregory Maddra on 2016-06-27.
 */
open class TransparentNodeResponsePacket : IMessage {

    lateinit var entries: List<TransparentNodeWailaEntry>
    val map: Map<String, String>
        get() = entries.associate { it.label to it.values.firstOrNull().orEmpty() }
    lateinit var coord: Coordinate

    constructor() {

    }

    constructor(m: Map<String, String>, c: Coordinate) {
        entries = m.map { (label, value) -> TransparentNodeWailaEntry(label, listOf(value)) }
        coord = c
    }

    constructor(entries: List<TransparentNodeWailaEntry>, c: Coordinate) {
        this.entries = entries
        coord = c
    }

    override fun fromBytes(buf: ByteBuf?) {
        val length = ByteBufUtils.readVarInt(buf, 5)
        entries = (1..length).map {
            val label = ByteBufUtils.readUTF8String(buf)
            val valueCount = ByteBufUtils.readVarInt(buf, 5)
            TransparentNodeWailaEntry(label, (1..valueCount).map { ByteBufUtils.readUTF8String(buf) })
        }
        val x = ByteBufUtils.readVarInt(buf, 5)
        val y = ByteBufUtils.readVarInt(buf, 5)
        val z = ByteBufUtils.readVarInt(buf, 5)
        val w = ByteBufUtils.readVarInt(buf, 5)
        coord = Coordinate(x, y, z, w)
    }

    override fun toBytes(buf: ByteBuf?) {
        ByteBufUtils.writeVarInt(buf, entries.size, 5)
        for (entry in entries) {
            ByteBufUtils.writeUTF8String(buf, entry.label)
            ByteBufUtils.writeVarInt(buf, entry.values.size, 5)
            entry.values.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        }
        ByteBufUtils.writeVarInt(buf, coord.x, 5)
        ByteBufUtils.writeVarInt(buf, coord.y, 5)
        ByteBufUtils.writeVarInt(buf, coord.z, 5)
        ByteBufUtils.writeVarInt(buf, coord.dimension, 5)
    }
}

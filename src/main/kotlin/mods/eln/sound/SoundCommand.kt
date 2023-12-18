package mods.eln.sound

import mods.eln.misc.Coordinate
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class SoundCommand {
    @JvmField
    var world: World? = null
    @JvmField
    var x: Double = 0.0
    @JvmField
    var y: Double = 0.0
    @JvmField
    var z: Double = 0.0
    @JvmField
    var track: String? = null
    var trackLength: Double = 0.0
    @JvmField
    var volume: Float = 1f
    @JvmField
    var pitch: Float = 1f
    @JvmField
    var rangeNominal: Float = 0f
    @JvmField
    var rangeMax: Float = 0f
    @JvmField
    var blockFactor: Float = 0f
    @JvmField
    var uuid: ArrayList<Int> = ArrayList()

    enum class Range {
        Small, Mid, Far
    }

    constructor() {
        mediumRange()
    }

    constructor(track: String?) {
        this.track = track
        mediumRange()
    }

    constructor(track: String?, trackLength: Double) {
        this.track = track
        this.trackLength = trackLength
        mediumRange()
    }

    constructor(s: SoundTrack) {
        track = s.track
        volume = s.volume
        pitch = s.pitch
        rangeNominal = s.rangeNominal
        rangeMax = s.rangeMax
        blockFactor = s.blockFactor
        uuid = s.uuid.clone() as ArrayList<Int>
    }

    fun copy(): SoundCommand {
        val c = SoundCommand()
        c.world = world
        c.x = x
        c.y = y
        c.z = z
        c.track = track
        c.trackLength = trackLength
        c.volume = volume
        c.pitch = pitch
        c.rangeNominal = rangeNominal
        c.rangeMax = rangeMax
        c.blockFactor = blockFactor
        c.uuid = uuid.clone() as ArrayList<Int>
        return c
    }

    fun play() {
        if (world!!.isRemote) SoundClient.play(this)
        else SoundServer.play(this)
    }

    fun set(c: Coordinate): SoundCommand {
        world = c.world()
        x = c.x + 0.5
        y = c.y + 0.5
        z = c.z + 0.5
        return this
    }

    fun set(c: TileEntity): SoundCommand {
        world = c.worldObj
        x = c.xCoord + 0.5
        y = c.yCoord + 0.5
        z = c.zCoord + 0.5
        //mediumRange();
        return this
    }

    fun set(x: Double, y: Double, z: Double, w: World?): SoundCommand {
        world = w
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun applyRange(range: Range?) {
        when (range) {
            Range.Small -> smallRange()
            Range.Far -> longRange()
            Range.Mid -> mediumRange()
            else -> mediumRange()
        }
    }

    fun mediumRange(): SoundCommand {
        rangeNominal = 4f
        rangeMax = 16f
        blockFactor = 1f
        return this
    }

    fun smallRange(): SoundCommand {
        rangeNominal = 2f
        rangeMax = 8f
        blockFactor = 3f
        return this
    }

    fun verySmallRange(): SoundCommand {
        rangeNominal = 2f
        rangeMax = 4f
        blockFactor = 10f
        return this
    }

    fun longRange(): SoundCommand {
        rangeNominal = 8f
        rangeMax = 48f
        blockFactor = 0.3f
        return this
    }

    fun mulVolume(volume: Float, pitch: Float): SoundCommand {
        this.volume *= volume
        this.pitch *= pitch
        return this
    }

    fun addUuid(uuid: Int): SoundCommand {
        this.uuid.add(uuid)
        return this
    }

    @Throws(IOException::class)
    fun writeTo(stream: DataOutputStream) {
        stream.writeInt((x * 8).toInt())
        stream.writeInt((y * 8).toInt())
        stream.writeInt((z * 8).toInt())

        stream.writeUTF(track)
        stream.writeFloat(volume)
        stream.writeFloat(pitch)
        stream.writeFloat(rangeNominal)
        stream.writeFloat(rangeMax)
        stream.writeFloat(blockFactor)
        stream.writeByte(uuid.size)
        for (i in uuid) {
            stream.writeInt(i)
        }
    }

    fun mulVolume(volume: Double): SoundCommand {
        this.volume *= volume.toFloat()
        return this
    }

    fun applyNominalVolume(nominalVolume: Double): SoundCommand {
        mulVolume(nominalVolume)
        return this
    }

    fun mulBlockAttenuation(factor: Double): SoundCommand {
        this.blockFactor *= factor.toFloat()
        return this
    }

    companion object {
        @Throws(IOException::class)
        fun fromStream(stream: DataInputStream, w: World?): SoundCommand {
            val p = SoundCommand()
            p.world = w

            p.x = stream.readInt() / 8.0
            p.y = stream.readInt() / 8.0
            p.z = stream.readInt() / 8.0
            p.track = stream.readUTF()
            p.volume = stream.readFloat()
            p.pitch = stream.readFloat()
            p.rangeNominal = stream.readFloat()
            p.rangeMax = stream.readFloat()
            p.blockFactor = stream.readFloat()
            p.uuid = ArrayList()
            for (idx in stream.readByte() downTo 1) {
                p.addUuid(stream.readInt())
            }
            return p
        }
    }
}

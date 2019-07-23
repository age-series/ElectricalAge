package mods.eln.sound

import mods.eln.misc.Coordinate
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class SoundCommand {

    internal var world: World? = null
    internal var x: Double = 0.0
    internal var y: Double = 0.0
    internal var z: Double = 0.0
    internal var track: String = ""
    internal var volume = 1f
    internal var pitch = 1f
    internal var rangeNominal: Float = 0.toFloat()
    internal var rangeMax: Float = 0.toFloat()
    internal var blockFactor: Float = 0.toFloat()

    constructor() {
        mediumRange()
    }

    constructor(track: String) {
        this.track = track
        mediumRange()
    }

    fun play() {
        if (world!!.isRemote)
            SoundClient.play(this)
        else
            SoundServer.play(this)
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
        return this
    }

    operator fun set(x: Double, y: Double, z: Double, w: World): SoundCommand {
        world = w
        this.x = x
        this.y = y
        this.z = z
        return this
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

    @Throws(IOException::class)
    fun writeTo(stream: DataOutputStream) {

        stream.writeDouble(x)
        stream.writeDouble(y)
        stream.writeDouble(z)

        stream.writeUTF(track)
        stream.writeFloat(volume)
        stream.writeFloat(pitch)
        stream.writeFloat(rangeNominal)
        stream.writeFloat(rangeMax)
        stream.writeFloat(blockFactor)
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
        fun fromStream(stream: DataInputStream, w: World): SoundCommand {
            val p = SoundCommand()
            p.world = w
            p.x = stream.readDouble()
            p.y = stream.readDouble()
            p.z = stream.readDouble()
            p.track = stream.readUTF()
            p.volume = stream.readFloat()
            p.pitch = stream.readFloat()
            p.rangeNominal = stream.readFloat()
            p.rangeMax = stream.readFloat()
            p.blockFactor = stream.readFloat()
            return p
        }

        fun sqDistDelta(cx: Double, cy: Double, cz: Double, px: Double, py: Double, pz: Double) = (cx - px) * (cx - px) + (cy - py) * (cy - py) + (cz - pz) * (cz - pz)

        fun toSoundData(sc: SoundCommand, player: EntityPlayer): ClientSoundHandler.SoundData {
            val coord = Coordinate()
            coord.x = sc.x.toInt()
            coord.y = sc.y.toInt()
            coord.z = sc.z.toInt()
            val oneshotSound = object: OneshotSound(sc.track, coord, ISound.AttenuationType.LINEAR) {
                override fun getPitch(): Float {
                    return sc.pitch
                }

                override fun getVolume(): Float {
                    return sc.volume
                }
            }
            val distSquared = sqDistDelta(sc.x, sc.y, sc.z, player.posX, player.posY, player.posZ)
            val sd = ClientSoundHandler.SoundData(oneshotSound, distSquared)
            return sd
        }
    }
}

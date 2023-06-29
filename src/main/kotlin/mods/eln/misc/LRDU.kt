package mods.eln.misc

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Represents the 6 possible directions along the axis of a block.
 */
enum class LRDU(var dir: Int) {
    /**
     *
     */
    Left(0),

    /**
     *
     */
    Right(1),

    /**
     *
     */
    Down(2),

    /**
     *
     */
    Up(3);

    fun toInt(): Int {
        return dir
    }

    //Don't change !
    fun inverse(): LRDU {
        return when (this) {
            Down -> Up
            Left -> Right
            Right -> Left
            Up -> Down
        }
    }

    fun inverseIfLR(): LRDU {
        return when (this) {
            Down -> Down
            Left -> Right
            Right -> Left
            Up -> Up
        }
    }

    fun applyTo(vector: DoubleArray, value: Double) {
        when (this) {
            Down -> vector[1] -= value
            Left -> vector[0] -= value
            Right -> vector[0] += value
            Up -> vector[1] += value
        }
    }

    fun rotate4PinDistances(distances: FloatArray): FloatArray {
        return if (distances.size != 4) distances else when (this) {
            Left -> floatArrayOf(distances[3], distances[2], distances[0], distances[1])
            Down -> floatArrayOf(distances[1], distances[0], distances[3], distances[2])
            Right -> floatArrayOf(distances[2], distances[3], distances[1], distances[0])
            Up -> distances
        }
    }

    val nextClockwise: LRDU
        get() {
            return when (this) {
                Down -> Left
                Left -> Up
                Right -> Down
                Up -> Right
            }
        }

    fun glRotateOnX() {
        when (this) {
            Left -> {
            }
            Up -> GL11.glRotatef(90f, 1f, 0f, 0f)
            Right -> GL11.glRotatef(180f, 1f, 0f, 0f)
            Down -> GL11.glRotatef(270f, 1f, 0f, 0f)
        }
    }

    fun rotateOnXnLeft(v: DoubleArray) {
        val y = v[1]
        val z = v[2]
        when (this) {
            Left -> {
            }
            Up -> {
                v[1] = -z
                v[2] = y
            }
            Right -> {
                v[1] = -y
                v[2] = -z
            }
            Down -> {
                v[1] = z
                v[2] = -y
            }
        }
    }

    fun rotateOnXnLeft(v: Vec3) {
        val y = v.yCoord
        val z = v.zCoord
        when (this) {
            Left -> {
            }
            Up -> {
                v.yCoord = -z
                v.zCoord = y
            }
            Right -> {
                v.yCoord = -y
                v.zCoord = -z
            }
            Down -> {
                v.yCoord = z
                v.zCoord = -y
            }
        }
    }

    fun left(): LRDU {
        return when (this) {
            Down -> Right
            Left -> Down
            Right -> Up
            Up -> Left
        }
    }

    fun right(): LRDU {
        return when (this) {
            Down -> Left
            Left -> Up
            Right -> Down
            Up -> Right
        }
    }

    fun writeToNBT(nbt: NBTTagCompound, name: String?) {
        nbt.setByte(name, toInt().toByte())
    }

    fun serialize(stream: DataOutputStream) {
        try {
            stream.writeByte(this.toInt())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun fromInt(value: Int): LRDU {
            when (value) {
                0 -> return Left
                1 -> return Right
                2 -> return Down
                3 -> return Up
            }
            return Left
        }

        @JvmStatic
        fun readFromNBT(nbt: NBTTagCompound, name: String?): LRDU {
            return fromInt(nbt.getByte(name).toInt())
        }

        @JvmStatic
        fun deserialize(stream: DataInputStream): LRDU {
            return try {
                fromInt(stream.readByte().toInt())
            } catch (e: IOException) {
                e.printStackTrace()
                Up
            }
        }
    }
}

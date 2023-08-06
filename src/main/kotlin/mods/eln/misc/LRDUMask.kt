package mods.eln.misc

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class LRDUMask {
    var mask: Int

    constructor() {
        mask = 0
    }

    constructor(mask: Int) {
        this.mask = mask
    }

    fun left(): Boolean {
        return mask and 1 != 0
    }

    fun right(): Boolean {
        return mask and 2 != 0
    }

    fun down(): Boolean {
        return mask and 4 != 0
    }

    fun up(): Boolean {
        return mask and 8 != 0
    }

    fun set(mask: Int) {
        this.mask = mask
    }

    operator fun set(lrdu: LRDU, value: Boolean) {
        mask = if (value) {
            mask or (1 shl lrdu.dir)
        } else {
            mask and (1 shl lrdu.dir).inv()
        }
    }

    operator fun get(lrdu: LRDU): Boolean {
        return mask and (1 shl lrdu.dir) != 0
    }

    fun serialize(stream: DataOutputStream) {
        try {
            stream.writeByte(mask)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun deserialize(stream: DataInputStream) {
        try {
            set(stream.readByte().toInt())
        } catch (e: IOException) {
            e.printStackTrace()
            set(0)
        }
    }
}

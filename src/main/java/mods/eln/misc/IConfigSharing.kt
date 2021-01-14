package mods.eln.misc

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

interface IConfigSharing {
    @Throws(IOException::class)
    fun serializeConfig(stream: DataOutputStream?)

    @Throws(IOException::class)
    fun deserialize(stream: DataInputStream?)
}

package mods.eln.sound

import mods.eln.client.ClientProxy
import mods.eln.sound.ClientSoundHandler.*
import net.minecraft.client.Minecraft

class LoopedSoundManager(val updateInterval: Float = 0.5f) {
    private var remaining = 0f
    private val loops = mutableSetOf<SoundData>()

    fun add(loop: LoopedSound) {
        if (loop.active) {
            loops.add(SoundData(loop, 100.0 * 100.0))
        }
    }

    fun dispose() = loops.forEach {
        if (it.sound is LoopedSound)
            it.sound.active = false
            ClientProxy.clientSoundHandler.stop(it)
    }

    // takes in two points and gets the squared distance delta between them
    fun sqDistDelta(cx: Double, cy: Double, cz: Double, px: Double, py: Double, pz: Double) = (cx - px) * (cx - px) + (cy - py) * (cy - py) + (cz - pz) * (cz - pz)

    fun process(deltaT: Float) {
        remaining -= deltaT
        if (remaining <= 0) {
            loops.forEach {
                if (it.sound !is LoopedSound) return
                // add 0.5 to put the point in the center of the block making sounds
                val cx = it.sound.coord.x + 0.5
                val cy = it.sound.coord.y + 0.5
                val cz = it.sound.coord.z + 0.5
                // get the player, and get the squared distance between the player and the block
                val player = Minecraft.getMinecraft().thePlayer
                val distDeltaSquared = sqDistDelta(cx, cy, cz, player.posX, player.posY, player.posZ)
                // when comparing, compare distDeltaSquared to the square of the distance delta that you are trying to compare against.
                it.distSquared = distDeltaSquared
                ClientProxy.clientSoundHandler.start(it)
            }
            remaining = updateInterval
        }
    }
}

package mods.eln.sound

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import mods.eln.debug.DP
import mods.eln.debug.DPType
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundHandler

class ClientSoundHandler {

    private val mcSoundHandler: SoundHandler = Minecraft.getMinecraft().soundHandler
    private val soundQueue: MutableList<SoundData> = mutableListOf()
    private val soundsPlaying: MutableList<SoundData> = mutableListOf()

    fun isPlaying(sound: SoundData): Boolean {
        return sound in soundsPlaying
    }

    fun isReallyPlaying(sound: SoundData): Boolean {
        return mcSoundHandler.isSoundPlaying(sound.sound)
    }

    fun start(sound: SoundData) {
        soundQueue.add(sound)
    }

    fun stop(sound: SoundData) {
        soundsPlaying.remove(sound)
        soundQueue.remove(sound)
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) return
        DP.println(DPType.SOUND, "Starting sound tick!")
        val startSounds = mutableListOf<SoundData>()
        val stopSounds = mutableListOf<SoundData>()

        soundQueue.sort()

        val numOfSounds = 16
        var i = 0
        while (i < numOfSounds && (i < soundQueue.size)) {
            if (soundQueue[i].sound.volume > 0) {
                startSounds.add(soundQueue[i])
                i++
            }
        }

        for (sound in soundsPlaying) {
            if (sound !in startSounds) {
                stopSounds.add(sound)
            }
        }

        for (sound in startSounds) {
            if (sound !in soundsPlaying)
                DP.println(DPType.SOUND, "Playing ${sound.sound}")
                mcSoundHandler.playSound(sound.sound)
                soundsPlaying.add(sound)
        }

        for (sound in stopSounds) {
            mcSoundHandler.stopSound(sound.sound)
            soundsPlaying.remove(sound)
        }
    }

    data class SoundData(val sound: ISound, var distSquared: Double): Comparable<SoundData> {
        override fun compareTo(other: SoundData): Int {
            return distSquared.toInt() - other.distSquared.toInt()
        }
    }
}

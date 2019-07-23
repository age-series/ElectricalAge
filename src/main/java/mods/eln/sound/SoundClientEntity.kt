package mods.eln.sound

import mods.eln.client.IUuidEntity
import mods.eln.debug.DP
import mods.eln.debug.DPType
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundManager

class SoundClientEntity(var sm: SoundManager, var sound: ISound) : IUuidEntity {

    internal var borneTimer = 5

    override fun isAlive(): Boolean {
        if (borneTimer != 0) {
            borneTimer--
            return true
        }
        return sm.isSoundPlaying(sound)
    }

    override fun kill() {
        DP.println(DPType.SOUND, "Sound deleted")
        sm.stopSound(sound)
    }
}

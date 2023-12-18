package mods.eln.sound

import mods.eln.client.IUuidEntity
import mods.eln.misc.Utils.println
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundManager

class SoundClientEntity(var sm: SoundManager, var sound: ISound) : IUuidEntity {
    var borneTimer: Int = 5

    override fun isAlive(): Boolean {
        if (borneTimer != 0) {
            borneTimer--
            return true
        }
        return sm.isSoundPlaying(sound)
    }

    override fun kill() {
        println("Sound deleted")
        sm.stopSound(sound)
    }
}

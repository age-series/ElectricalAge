package mods.eln.sound

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import mods.eln.client.UuidManager
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundManager
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent
import net.minecraftforge.common.MinecraftForge

class SoundClientEventListener(var uuidManager: UuidManager) {
    @JvmField
    var currentUuid: ArrayList<Int>? = null

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun event(e: PlaySoundSourceEvent) {
        if (currentUuid == null) return
        uuidManager.add(currentUuid!!, SoundClientEntity(e.manager, e.sound))
    }

    internal class KillSound {
        var sound: ISound? = null
        var sm: SoundManager? = null

        fun kill() {
            sm!!.stopSound(sound)
        }
    }
}

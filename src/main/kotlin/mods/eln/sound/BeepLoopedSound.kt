package mods.eln.sound

import mods.eln.misc.Coordinate

class BeepLoopedSound(
    coord: Coordinate,
    private var volume: Float = 1f,
    private var pitch: Float = 1f
) : LoopedSound("eln:beep_loop", coord) {
    override fun getVolume() = volume
    override fun getPitch() = pitch

    fun setVolume(value: Float) {
        volume = value
    }

    fun setPitch(value: Float) {
        pitch = value
    }

    fun setActiveState(value: Boolean) {
        active = value
    }

    fun isActive(): Boolean {
        return active
    }
}

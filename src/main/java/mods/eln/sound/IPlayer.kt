package mods.eln.sound

interface IPlayer {
    fun play(cmd: SoundCommand)
    fun stop(uuid: Int)
}

package mods.eln

import net.minecraft.entity.player.EntityPlayer

data class KeyState(
    val name: String,
    var state: Boolean = false
)

data class PlayerKeyState(
    var player: EntityPlayer,
    val keyStateList: List<KeyState> = listOf(
        KeyState(ServerKeyHandler.WRENCH),
        KeyState(ServerKeyHandler.WIKI),
        KeyState(ServerKeyHandler.LSHIFT),
        KeyState(ServerKeyHandler.RSHIFT)
    )
)

object ServerKeyHandler {
    const val WRENCH = "Wrench"
    const val WIKI = "Wiki"
    const val LSHIFT = "LeftShift"
    const val RSHIFT = "RightShift"

    private val playerKeyStateList = mutableListOf<PlayerKeyState>()

    fun get(name: String, player: EntityPlayer): Boolean {
        return playerKeyStateList.firstOrNull { it.player == player }?.keyStateList?.firstOrNull { it.name == name }?.state?: false
    }

    fun set(name: String, state: Boolean, player: EntityPlayer) {
        addPlayerToList(player)
        playerKeyStateList.firstOrNull { it.player == player }?.keyStateList?.firstOrNull { it.name == name }?.state = state
    }

    private fun addPlayerToList(player: EntityPlayer) {
        playerKeyStateList.forEach { if (it.player == player) return }
        playerKeyStateList.add(PlayerKeyState(player))
    }
}
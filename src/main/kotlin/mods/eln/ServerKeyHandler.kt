package mods.eln

data class KeyState(val name: String, var state: Boolean = false)

object ServerKeyHandler {
    val WRENCH = "Wrench"
    val WIKI = "Wiki"
    private val keyState = listOf(KeyState(WRENCH))

    fun get(name: String): Boolean {
        return keyState.firstOrNull {it.name == name}?.state?: false
    }

    fun set(name: String, state: Boolean) {
        keyState.firstOrNull { it.name == name }?.state = state
    }
}
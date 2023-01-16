package mods.eln.node

import mods.eln.Eln

abstract class GhostNode : NodeBase() {
    override fun mustBeSaved(): Boolean {
        return false
    }

    override val nodeUuid = Eln.ghostBlock.nodeUuid
}

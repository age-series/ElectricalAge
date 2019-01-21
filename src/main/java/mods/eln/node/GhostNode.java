package mods.eln.node;

import mods.eln.Eln;
import mods.eln.Vars;

public abstract class GhostNode extends NodeBase {
    @Override
    public boolean mustBeSaved() {
        return false;
    }

    @Override
    public String getNodeUuid() {
        return Vars.ghostBlock.getNodeUuid();
    }
}

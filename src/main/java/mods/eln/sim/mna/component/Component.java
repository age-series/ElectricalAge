package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.state.State;

public abstract class Component {

    private SubSystem subSystem;

    public IAbstractor abstractedBy;

    public Component() {}
    private String owner;

    public void addToSubsystem(SubSystem s) {
        this.subSystem = s;
    }

    public SubSystem getSubSystem() {
        if (isAbstracted()) return abstractedBy.getAbstractorSubSystem();
        return subSystem;
    }

    protected SubSystem getLocalSubSystem() {
        return subSystem;
    }

    public abstract void applyToSubsystem(SubSystem s);

    public abstract State[] getConnectedStates();

    public boolean canBeReplacedByInterSystem() {
        return false;
    }

    public void breakConnection() {
    }

    public void returnToRootSystem(RootSystem root) {
        root.addComponents.add(this);
    }

    public void dirty() {
        if (abstractedBy != null) {
            abstractedBy.dirty(this);
        } else if (getSubSystem() != null) {
            getSubSystem().invalidate();
        }
    }

    public void quitSubSystem() {
        subSystem = null;
    }

    public boolean isAbstracted() {
        return abstractedBy != null;
    }

    public void onAddToRootSystem() {}

    public void onRemoveFromRootSystem() {}

    public String toString() {
        return "(" + this.getClass().getSimpleName() + ")";
    }

    public Component setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getOwner() {
        return owner;
    }
}

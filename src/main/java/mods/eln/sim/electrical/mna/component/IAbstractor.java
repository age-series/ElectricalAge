package mods.eln.sim.electrical.mna.component;

import mods.eln.sim.electrical.mna.SubSystem;

public interface IAbstractor {

    void dirty(Component component);

    SubSystem getAbstractorSubSystem();
}

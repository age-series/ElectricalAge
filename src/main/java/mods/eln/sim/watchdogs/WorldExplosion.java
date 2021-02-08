package mods.eln.sim.watchdogs;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class WorldExplosion implements IDestructible {

    Object origin;

    Coordinate c;
    float strength;
    String type;

    public WorldExplosion(Coordinate c) {
        this.c = c;
    }

    public WorldExplosion(SixNodeElement e) {
        this.c = e.getCoordinate();
        this.type = e.toString();
        origin = e;
    }

    public WorldExplosion(TransparentNodeElement e) {
        this.c = e.coordinate();
        this.type = e.toString();
        origin = e;
    }

    public WorldExplosion(EnergyConverterElnToOtherNode e) {
        this.c = e.coordinate;
        this.type = e.toString();
        origin = e;
    }

    public WorldExplosion cableExplosion() {
        strength = 1.5f;
        return this;
    }

    public WorldExplosion machineExplosion() {
        strength = 3;
        return this;
    }

    @Override
    public void destructImpl() {
        if (Eln.explosionEnable)
            c.world().createExplosion(null, c.x, c.y, c.z, strength, true);
        else
            c.world().setBlock(c.x, c.y, c.z, Blocks.air);
    }

    @Override
    public String describe() {
        return String.format("%s (%s)", this.type, this.c.toString());
    }
}

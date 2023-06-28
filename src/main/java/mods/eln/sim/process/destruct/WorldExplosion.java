package mods.eln.sim.process.destruct;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class WorldExplosion implements IDestructable {

    Object origin;

    Coordinate coordinate;
    float strength;
    String type;

    public WorldExplosion(Coordinate c) {
        this.coordinate = c;
    }

    public WorldExplosion(SixNodeElement e) {
        this.coordinate = e.getCoordinate();
        this.type = e.toString();
        origin = e;
    }

    public WorldExplosion(TransparentNodeElement e) {
        this.coordinate = e.coordinate();
        this.type = e.toString();
        origin = e;
    }

    public WorldExplosion(EnergyConverterElnToOtherNode e) {
        this.coordinate = e.coordinate;
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
            coordinate.world().createExplosion((Entity) null, coordinate.x, coordinate.y, coordinate.z, strength, true);
        else
            coordinate.world().setBlock(coordinate.x, coordinate.y, coordinate.z, Blocks.air);
    }

    @Override
    public String describe() {
        return String.format("%s (%s)", this.type, this.coordinate.toString());
    }
}

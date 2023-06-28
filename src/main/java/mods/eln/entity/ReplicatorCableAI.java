package mods.eln.entity;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.*;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableElement;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;

import java.util.List;
import java.util.Random;

public class ReplicatorCableAI extends EntityAIBase implements ITimeRemoverObserver {

    ReplicatorEntity entity;

    public Coordinate cableCoordinate = null;
    Random rand = new Random();
    int lookingPerUpdate = 20;

    ElectricalLoad load = new ElectricalLoad(), cableLoad;
    Resistor resistorLoad = new Resistor(load, null);
    ElectricalConnection connection;
    TimeRemover timeRemover = new TimeRemover(this);

    double moveTimeOut;
    double moveTimeOutReset = 20;
    double resetTimeout;
    double resetTimeoutReset = 120;

    PreSimCheck preSimCheck;

    public ReplicatorCableAI(ReplicatorEntity entity) {
        load.setAsPrivate();
        this.entity = entity;
        Eln.instance.highVoltageCableDescriptor.applyTo(load);
        load.setSerialResistance(load.getSerialResistance() * 10);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        assert NodeManager.instance != null;
        List<NodeBase> nodes = NodeManager.instance.getNodes();
        if (nodes.isEmpty()) return false;
        for (int idx = 0; idx < lookingPerUpdate; idx++) {
            NodeBase node = nodes.get(rand.nextInt(nodes.size()));
            double distance = node.coordinate.distanceTo(entity);

            if (distance > 15) continue;

            if (!(node instanceof SixNode)) continue;

            SixNode sixNode = (SixNode) node;

            for (SixNodeElement e : sixNode.sideElementList) {
                if (e == null) continue;

                if (!(e instanceof ElectricalCableElement)) continue;

                ElectricalCableElement cable = (ElectricalCableElement) e;

                if (!isElectricalCableInteresting(cable)) continue;

                PathEntity path = entity.getNavigator().getPathToXYZ(node.coordinate.x, node.coordinate.y, node.coordinate.z);

                if (path == null) continue;

                entity.getNavigator().setPath(path, 1);
                cableCoordinate = node.coordinate;
                moveTimeOut = moveTimeOutReset;
                resistorLoad.highImpedance();
                resetTimeout = resetTimeoutReset * (0.8 + Math.random() * 0.4);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean continueExecuting() {
        return cableCoordinate != null;
    }

    @Override
    public void updateTask() {
        moveTimeOut -= 0.05;
        resetTimeout -= 0.05;
        ElectricalCableElement cable;

        if ((cable = getCable()) == null) {
            cableCoordinate = null;
            return;
        }

        cableLoad = cable.electricalLoad;
        double distance = cableCoordinate.distanceTo(entity);

        if (distance > 2 && (entity.getNavigator().getPath() == null || entity.getNavigator().getPath().isFinished())) {
            this.entity.getNavigator().tryMoveToXYZ(cableCoordinate.x, cableCoordinate.y, cableCoordinate.z, 1);
        }
        if (distance < 2) {
            double u = cable.electricalLoad.getVoltage();
            double nextRp = Math.pow(u / Eln.LVU, -0.3) * u * u / (50);
            if (resistorLoad.getResistance() < 0.8 * nextRp) {
                entity.attackEntityFrom(DamageSource.magic, 5);
            } else {
                entity.eatElectricity(resistorLoad.getPower() * 0.05);
            }

            resistorLoad.setResistance(nextRp);

            timeRemover.setTimeout(0.16);
            moveTimeOut = moveTimeOutReset;
        } else {
            resistorLoad.highImpedance();
        }

        if (moveTimeOut < 0 || resetTimeout < 0) {
            cableCoordinate = null;
        }
    }

    boolean isElectricalCableInteresting(ElectricalCableElement c) {
        return !c.descriptor.signalWire && !(c.electricalLoad.getVoltage() < 30);
    }

    ElectricalCableElement getCable() {
        if (cableCoordinate == null) return null;

        assert NodeManager.instance != null;
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(cableCoordinate);

        if (node == null) return null;

        if (node instanceof SixNode) {
            SixNode sixNode = (SixNode) node;
            for (SixNodeElement e : sixNode.sideElementList) {
                if (e == null) continue;

                if (e instanceof ElectricalCableElement) {
                    ElectricalCableElement cable = (ElectricalCableElement) e;
                    if (isElectricalCableInteresting(cable)) return cable;
                }
            }
        }
        return null;
    }

    @Override
    public void timeRemoverRemove() {
        Eln.simulator.removeElectricalLoad(load);
        Eln.simulator.removeElectricalComponent(connection);
        Eln.simulator.removeElectricalComponent(resistorLoad);
        Eln.simulator.removeSlowPreProcess(preSimCheck);
        connection = null;
    }

    @Override
    public void timeRemoverAdd() {
        Eln.simulator.addElectricalLoad(load);
        Eln.simulator.addElectricalComponent(connection = new ElectricalConnection(load, cableLoad));
        Eln.simulator.addElectricalComponent(resistorLoad);
        Eln.simulator.addSlowPreProcess(preSimCheck = new PreSimCheck());
    }

    class PreSimCheck implements IProcess {
        @Override
        public void process(double time) {
            if (!timeRemover.isArmed()) return;
            if (!Eln.simulator.isRegistred(cableLoad)) {
                timeRemover.shot();
            }
        }
    }
}

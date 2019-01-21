package mods.eln.sixnode.electricalwindsensor;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalWindSensorElement extends SixNodeElement {

    ElectricalWindSensorDescriptor descriptor;

    public NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
    public ElectricalWindSensorSlowProcess slowProcess = new ElectricalWindSensorSlowProcess(this);
    public NodePeriodicPublishProcess publishProcess = new NodePeriodicPublishProcess(sixNode, 5, 5);

    public ElectricalWindSensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        slowProcessList.add(slowProcess);
        slowProcessList.add(publishProcess);
        this.descriptor = (ElectricalWindSensorDescriptor) descriptor;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front == lrdu.left()) return outputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu.left()) return NodeBase.maskElectricalOutputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void initialize() {
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) Utils.getWind(sixNode.coordonate.dimention, sixNode.coordonate.y));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

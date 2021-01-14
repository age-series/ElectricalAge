package mods.eln.node.simple;

public class DescriptorBase {

    public String descriptorKey;

    public DescriptorBase(String key) {
        this.descriptorKey = key;
        DescriptorManager.put(key, this);
    }
}

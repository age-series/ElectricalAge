package mods.eln.simplenode.test;

import mods.eln.node.simple.SimpleNodeEntity;
import org.jetbrains.annotations.NotNull;

public class TestEntity extends SimpleNodeEntity {

    public TestEntity() {
        super("");
    }

    @NotNull
    @Override
    public String getNodeUuid() {
        return TestNode.getNodeUuidStatic();
    }
}

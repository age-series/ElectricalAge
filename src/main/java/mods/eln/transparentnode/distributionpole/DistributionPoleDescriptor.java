package mods.eln.transparentnode.distributionpole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import org.lwjgl.opengl.GL11;

public class DistributionPoleDescriptor extends TransparentNodeDescriptor {

    private Obj3D.Obj3DPart pole, cross, fuse, fuseHolder, insulator, transformer;

    public DistributionPoleDescriptor(String name, Obj3D obj, GhostGroup ghostGroup) {
        super(name, DistributionPoleElement.class, DistributionPoleRender.class);
        this.ghostGroup = ghostGroup;

        pole = obj.getPart("WoodPole");
        cross = obj.getPart("Cross");
        fuse = obj.getPart("Fuse");
        fuseHolder = obj.getPart("FuseHolder");
        insulator = obj.getPart("Insulator");
        transformer = obj.getPart("Transformer");

    }

    void draw(Direction front, boolean hasCrossbar, boolean hasTransformer, boolean hasFuseHolder, boolean hasFuse, float fuseEngaged) {
        front.glRotateZnRef();
        if (pole != null) pole.draw();

        if (hasCrossbar) {
            if(cross != null) cross.draw();
            if(insulator != null) insulator.draw();

            if (hasTransformer) {
                if(transformer != null) transformer.draw();
            }

            if(hasFuseHolder) {
                GL11.glTranslatef(0f, -0.5f, 0f);
                if(fuseHolder != null) fuseHolder.draw();
                if (hasFuse) {
                    GL11.glTranslatef(0f, 0.5f, 0f);
                    GL11.glRotatef(fuseEngaged * 90, -1, 0,0);
                    if(fuse != null) fuse.draw();
                }
            }
        }
    }
}

package mods.eln.transparentnode.distributionpole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.BoundingBox;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import org.lwjgl.opengl.GL11;

public class DistributionPoleDescriptor extends TransparentNodeDescriptor {

    private Obj3D.Obj3DPart pole, cross, fuse, fuseHolder, insulator, transformer;
    private float fusePivotX, fusePivotY, fusePivotZ;

    public DistributionPoleDescriptor(String name, Obj3D obj, GhostGroup ghostGroup) {
        super(name, DistributionPoleElement.class, DistributionPoleRender.class);
        this.ghostGroup = ghostGroup;

        pole = obj.getPart("WoodPoleD");
        if (pole == null) pole = obj.getPart("WoodPole");
        cross = obj.getPart("Cross");
        fuse = obj.getPart("Fuse");
        fuseHolder = obj.getPart("FuseHolder");
        insulator = obj.getPart("Insulator");
        transformer = obj.getPart("Transformer");
        if (transformer == null) transformer = obj.getPart("Trans2");

        if (fuseHolder != null) {
            BoundingBox holderBox = fuseHolder.boundingBox();
            fusePivotX = (float) ((holderBox.getMin().xCoord + holderBox.getMax().xCoord) * 0.5);
            fusePivotZ = (float) ((holderBox.getMin().zCoord + holderBox.getMax().zCoord) * 0.5);

            // Rotate around the lower hinge of the holder, centered on a cube
            // matching the fuse rod diameter placed at the fuse bottom.
            if (fuse != null) {
                BoundingBox fuseBox = fuse.boundingBox();
                float rodDiameter = (float) Math.min(
                    fuseBox.getMax().xCoord - fuseBox.getMin().xCoord,
                    fuseBox.getMax().zCoord - fuseBox.getMin().zCoord
                );
                float fuseBottomPivotY = (float) fuseBox.getMin().yCoord + rodDiameter * 0.5f;
                float holderLowerHingeY = (float) holderBox.getMin().yCoord + rodDiameter * 0.5f;
                fusePivotY = Math.max(fuseBottomPivotY, holderLowerHingeY);

                // Axis through the fuse and the inside bottom holder clip faces:
                // center on fuse in X, and shift toward holder-side clip in Z.
                fusePivotX = (float) ((fuseBox.getMin().xCoord + fuseBox.getMax().xCoord) * 0.5);
                fusePivotZ = (float) (fuseBox.getMax().zCoord - rodDiameter * 0.25f);
            } else {
                fusePivotY = (float) holderBox.getMin().yCoord;
            }
        }

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
                if(fuseHolder != null) fuseHolder.draw();
                if (hasFuse) {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(fusePivotX, fusePivotY, fusePivotZ);
                    GL11.glRotatef(fuseEngaged * 90f, -1f, 0f, 0f);
                    GL11.glTranslatef(-fusePivotX, -fusePivotY, -fusePivotZ);
                    if(fuse != null) fuse.draw();
                    GL11.glPopMatrix();
                }
            }
        }
    }
}

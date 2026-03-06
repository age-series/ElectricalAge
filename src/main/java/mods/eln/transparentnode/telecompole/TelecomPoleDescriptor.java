package mods.eln.transparentnode.telecompole;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import org.lwjgl.opengl.GL11;

public class TelecomPoleDescriptor extends TransparentNodeDescriptor {

    private Obj3D.Obj3DPart pole;

    public TelecomPoleDescriptor(String name, Obj3D obj, GhostGroup ghostGroup) {
        super(name, TelecomPoleElement.class, TelecomPoleRender.class);
        this.ghostGroup = ghostGroup;

        pole = obj.getPart("TelComPole");
    }

    public void draw() {
        if (pole != null) pole.draw();
    }
}

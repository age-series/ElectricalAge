package org.lwjgl.opengl;

public final class GL11 {
    public static final int GL_TEXTURE_2D = 0;
    public static final int GL_TRIANGLES = 0;
    public static final int GL_QUADS = 0;
    public static final int GL_COMPILE = 0;

    private GL11() {
    }

    public static void glDisable(int cap) {}
    public static void glEnable(int cap) {}
    public static void glBegin(int mode) {}
    public static void glEnd() {}
    public static void glNormal3f(float x, float y, float z) {}
    public static void glTexCoord2f(float s, float t) {}
    public static void glVertex3f(float x, float y, float z) {}
    public static int glGenLists(int range) { return 1; }
    public static void glNewList(int list, int mode) {}
    public static void glEndList() {}
    public static void glCallList(int list) {}
    public static void glPushMatrix() {}
    public static void glPopMatrix() {}
    public static void glTranslatef(float x, float y, float z) {}
    public static void glRotatef(float angle, float x, float y, float z) {}
}

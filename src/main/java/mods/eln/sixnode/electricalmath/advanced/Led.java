package mods.eln.sixnode.electricalmath.advanced;

import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

public class Led {
    public static final float[] color_red = new float[]{0.8f, 0f, 0f};
    public static final float[] color_green = new float[]{0f, 0.8f, 0f};
    public static final float[] color_yellow = new float[]{0.8f, 0.8f, 0f};
    public static final float[] color_off = new float[]{0.3f, 0.3f, 0.3f};

    public final Obj3D.Obj3DPart obj;

    boolean isOn = true;
    float[] led_color = color_off;
    boolean isFixed = true;
    boolean led_strobe_isOn = false;
    float led_strobe_chance = 0.4f;

    public Led(Obj3D.Obj3DPart obj) {
        this.obj = obj;
    }

    public void draw() {
        if (isOn) {
            if (isFixed) {
                GL11.glColor3f(led_color[0], led_color[1], led_color[2]);
                UtilsClient.drawLight(obj);
                return;
            } else {
                if (led_strobe_isOn) {
                    GL11.glColor3f(led_color[0], led_color[1], led_color[2]);
                    UtilsClient.drawLight(obj);
                    return;
                }
            }
        }

        GL11.glColor3f(color_off[0], color_off[1], color_off[2]);
        UtilsClient.drawLight(obj);
    }

    public void applyStrobeRandom(double rand){
        led_strobe_isOn = rand < led_strobe_chance;
    }

    public static void applyConfigTo(Led[] leds, int index, int length, int jumper, Consumer<Led> consumer){
        for (int idx = index; idx < index + length; idx += jumper) {
            consumer.accept(leds[idx]);
        }
    }

    public static void applyConfigTo(Led[] leds, int index, int length, Consumer<Led> consumer){
        for (int idx = index; idx < index + length; idx++) {
            consumer.accept(leds[idx]);
        }
    }

    public static void applyConfigTo(Led[] leds, Consumer<Led> consumer){
        applyConfigTo(leds, 0, leds.length, consumer);
    }
}

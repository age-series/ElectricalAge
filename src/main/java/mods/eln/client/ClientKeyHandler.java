package mods.eln.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import mods.eln.Eln;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientKeyHandler {

    public static final int wrenchId = 1;
    private static final String wrench = "Wrench";
    private static int[] keyValues = {Keyboard.KEY_C};
    private static String[] desc = {wrench};
    private static KeyBinding[] keys = new KeyBinding[desc.length];

    boolean[] states = new boolean[desc.length];

    public ClientKeyHandler() {

        for (int i = 0; i < desc.length; ++i) {
            if (i != 3)
                states[i] = false;
            keys[i] = new KeyBinding(desc[i], keyValues[i], StatCollector.translateToLocal("ElectricalAge"));
            ClientRegistry.registerKeyBinding(keys[i]);
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        for (int i = 0; i < desc.length; ++i) {
            boolean s = keys[i].getIsKeyPressed();
            if (s) return;
            if (states[i])
                setState(i, false);
            setState(i, true);
        }
    }

    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        for (int i = 0; i < desc.length; ++i) {
            boolean s = keys[i].getIsKeyPressed();
            if (!s && states[i]) {
                setState(i, false);
            }
        }
    }

    void setState(int id, boolean state) {
        states[id] = state;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.PACKET_PLAYER_KEY);
            stream.writeByte(id);
            stream.writeBoolean(state);
        } catch (IOException e) {
            e.printStackTrace();
        }

        UtilsClient.sendPacketToServer(bos);
    }
}

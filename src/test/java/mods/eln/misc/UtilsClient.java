package mods.eln.misc;

import net.minecraft.util.ResourceLocation;

public final class UtilsClient {
    public static ResourceLocation lastResource;
    public static int bindCount;

    private UtilsClient() {
    }

    public static void bindTexture(ResourceLocation resource) {
        lastResource = resource;
        bindCount++;
    }

    public static void reset() {
        lastResource = null;
        bindCount = 0;
    }
}

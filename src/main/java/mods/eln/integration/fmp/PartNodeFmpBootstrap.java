package mods.eln.integration.fmp;

import cpw.mods.fml.common.Loader;
import mods.eln.Eln;

public final class PartNodeFmpBootstrap {

    private PartNodeFmpBootstrap() {
    }

    public static void registerIfPresent() {
        if (!Loader.isModLoaded("ForgeMultipart")) {
            return;
        }

        try {
            Class<?> integration = Class.forName("mods.eln.integration.fmp.PartNodeFmpIntegration");
            integration.getMethod("register").invoke(null);
            Eln.LOGGER.info("Registered PartNode ForgeMultipart integration.");
        } catch (Throwable t) {
            Eln.LOGGER.error("Failed to register PartNode ForgeMultipart integration.", t);
        }
    }
}

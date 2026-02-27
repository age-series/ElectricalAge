package mods.eln.integration.fmp;

import codechicken.multipart.MultiPartRegistry;

public final class PartNodeFmpIntegration {

    private static boolean registered = false;

    private PartNodeFmpIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        final PartNodeFmpFactory factory = new PartNodeFmpFactory();
        MultiPartRegistry.registerParts(factory, new String[]{PartNodeFmpPart.TYPE});
        MultiPartRegistry.registerConverter(factory);
        registered = true;
    }
}

package mods.eln;

import cpw.mods.fml.common.Loader;

public class Other {

    public static boolean ic2Loaded = false;
    public static boolean ocLoaded = false;
    public static boolean ccLoaded = false;
    public static boolean teLoaded = false;

    public static double wattsToEu;
    public static double wattsToOC;
    public static double wattsToRf;

    public static final String modIdIc2 = "IC2";
    public static final String modIdOc = "OpenComputers";
    public static final String modIdTe = "Eln";
    public static final String modIdCc = "ComputerCraft";

    public static void check() {
        ic2Loaded = Loader.isModLoaded(modIdIc2);
        ocLoaded = Loader.isModLoaded(modIdOc);
        ccLoaded = Loader.isModLoaded(modIdCc);
        teLoaded = Loader.isModLoaded(modIdTe);
    }

    public static double getWattsToEu() {
        return wattsToEu;
    }

    public static double getWattsToOC() {
        return wattsToOC;
    }

    public static double getWattsToRf() {
        return wattsToRf;
    }
}

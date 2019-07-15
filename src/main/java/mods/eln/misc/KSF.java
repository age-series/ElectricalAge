package mods.eln.misc;

/**
 * KSF - Kotlin Shortfalls for the silly version we're using that literally has NO BITSHIFTING,
 * because WHY WOULD YOU WANT THAT
 */
public class KSF {
    /**
     * literally a << b
     *
     * @param a byte
     * @param b int
     * @return int
     */
    public static int shiftLeft(byte a, int b) {
        return a << b;
    }

    /**
     * literally a >> b
     *
     * @param a byte
     * @param b int
     * @return int
     */
    public static int shiftRight(byte a, int b) {
        return a >> b;
    }
}

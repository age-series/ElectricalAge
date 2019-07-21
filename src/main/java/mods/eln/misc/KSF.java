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

    /**
     *  returns -1 if less than 0, 0 if 0, and 1 if greater than 1
     *
     * @param a A number you want the sign of
     * @return The sign represented by -1, 0, or 1
     */
    public static double getSign(Double a) {
        return Math.signum(a);
    }
}

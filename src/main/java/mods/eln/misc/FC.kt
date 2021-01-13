package mods.eln.misc

/**
 * Formatting Codes (FC for short usage)
 *
 * Utility class to print message with colors. 16 colors are available.
 */
@Suppress("unused")
class FC(n: Int) {
    private val color: String
    override fun toString(): String {
        return "\u00a7" + color
    }

    companion object {
        // Only some short color aliases
        @JvmField
        val ORANGE = FC(6).toString()
        @JvmField
        val GREEN = FC(10).toString()
        @JvmField
        val RED = FC(12).toString()

        // All color indexes
        const val IDX_COLOR_BLACK = 0
        const val IDX_COLOR_DARK_BLUE = 1
        const val IDX_COLOR_DARK_GREEN = 2
        const val IDX_COLOR_DARK_CYAN = 3
        const val IDX_COLOR_DARK_RED = 4
        const val IDX_COLOR_DARK_MAGENTA = 5
        const val IDX_COLOR_DARK_YELLOW = 6
        const val IDX_COLOR_BRIGHT_GREY = 7
        const val IDX_COLOR_DARK_GREY = 8
        const val IDX_COLOR_BRIGHT_BLUE = 9
        const val IDX_COLOR_BRIGHT_GREEN = 10
        const val IDX_COLOR_BRIGHT_CYAN = 11
        const val IDX_COLOR_BRIGHT_RED = 12
        const val IDX_COLOR_BRIGHT_MAGENTA = 13
        const val IDX_COLOR_BRIGHT_YELLOW = 14
        const val IDX_COLOR_WHITE = 15

        // All color aliases
        @JvmField
        val BLACK = FC(IDX_COLOR_BLACK).toString()
        @JvmField
        val DARK_BLUE = FC(IDX_COLOR_DARK_BLUE).toString()
        @JvmField
        val DARK_GREEN = FC(IDX_COLOR_DARK_GREEN).toString()
        @JvmField
        val DARK_CYAN = FC(IDX_COLOR_DARK_CYAN).toString()
        @JvmField
        val DARK_RED = FC(IDX_COLOR_DARK_RED).toString()
        @JvmField
        val DARK_MAGENTA = FC(IDX_COLOR_DARK_MAGENTA).toString()
        @JvmField
        val DARK_YELLOW = FC(IDX_COLOR_DARK_YELLOW).toString()
        @JvmField
        val BRIGHT_GREY = FC(IDX_COLOR_BRIGHT_GREY).toString()
        @JvmField
        val DARK_GREY = FC(IDX_COLOR_DARK_GREY).toString()
        @JvmField
        val BRIGHT_BLUE = FC(IDX_COLOR_BRIGHT_BLUE).toString()
        @JvmField
        val BRIGHT_GREEN = FC(IDX_COLOR_BRIGHT_GREEN).toString()
        @JvmField
        val BRIGHT_CYAN = FC(IDX_COLOR_BRIGHT_CYAN).toString()
        @JvmField
        val BRIGHT_RED = FC(IDX_COLOR_BRIGHT_RED).toString()
        @JvmField
        val BRIGHT_MAGENTA = FC(IDX_COLOR_BRIGHT_MAGENTA).toString()
        @JvmField
        val BRIGHT_YELLOW = FC(IDX_COLOR_BRIGHT_YELLOW).toString()
        @JvmField
        val WHITE = FC(IDX_COLOR_WHITE).toString()

        // Other Minecraft formatting codes
        @JvmField
        val OBFUSCATED = "\u00a7k"
        @JvmField
        val BLOD = "\u00a7l"
        @JvmField
        val STRIKETHROUGH = "\u00a7m"
        @JvmField
        val UNDERLINE = "\u00a7n"
        @JvmField
        val ITALICS = "\u00a7o"
        @JvmField
        val RESET = "\u00a7r"
    }

    /**
     * Create a custom color.
     *
     * @param n 0 to 15 for all available colors
     */
    init {
        var n = n
        if (n < 0) n = 0 else if (n > 15) n = 15
        color = Integer.toHexString(n)
    }
}

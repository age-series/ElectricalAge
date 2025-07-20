package mods.eln.transparentnode.floodlight

enum class FloodlightConeWidth(val int: Int) {

    NARROW(60), MEDIUM(90), WIDE(120);

    companion object {
        fun fromInt(idx: Int): FloodlightConeWidth? {
            for (coneWidth in FloodlightConeWidth.values()) {
                if (coneWidth.int == idx) return coneWidth
            }
            return null
        }
    }

    fun cycleConeWidth(): FloodlightConeWidth {
        return when (this) {
            NARROW -> MEDIUM
            MEDIUM -> WIDE
            WIDE -> NARROW
        }
    }

}

enum class FloodlightConeRange(val int: Int) {

    NEAR(8), MIDDLE(16), FAR(24), EXTRA(32);

    companion object {
        fun fromInt(idx: Int): FloodlightConeRange? {
            for (coneRange in FloodlightConeRange.values()) {
                if (coneRange.int == idx) return coneRange
            }
            return null
        }
    }

    fun cycleConeRange(): FloodlightConeRange {
        return when (this) {
            NEAR -> MIDDLE
            MIDDLE -> FAR
            FAR -> EXTRA
            EXTRA -> NEAR
        }
    }

}
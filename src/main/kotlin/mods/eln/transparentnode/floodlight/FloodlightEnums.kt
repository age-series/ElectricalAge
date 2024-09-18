package mods.eln.transparentnode.floodlight

enum class FloodlightConeWidth(val int: Int) {

    NARROW(1), MEDIUM(5), WIDE(9);

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

    NEAR(10), MIDDLE(15), FAR(20);

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
            FAR -> NEAR
        }
    }

}
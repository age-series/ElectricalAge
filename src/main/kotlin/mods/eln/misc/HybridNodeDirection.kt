package mods.eln.misc

enum class HybridNodeDirection(val int: Int) {

    XN(0), XP(1), YN(2), YP(3), ZN(4), ZP(5);

    companion object {
        fun fromInt(idx: Int): HybridNodeDirection? {
            for (direction in HybridNodeDirection.values()) {
                if (direction.int == idx) return direction
            }
            return null
        }
    }

    fun toStandardDirection(): Direction {
        return when (this) {
            XN -> Direction.XN
            XP -> Direction.XP
            YN -> Direction.YN
            YP -> Direction.YP
            ZN -> Direction.ZN
            ZP -> Direction.ZP
        }
    }

    val inverse: HybridNodeDirection get() {
        return when (this) {
            XN -> XP
            XP -> XN
            YN -> YP
            YP -> YN
            ZN -> ZP
            ZP -> ZN
        }
    }

    fun right(axis: HybridNodeDirection): HybridNodeDirection {
        return when (axis) {
            XN -> {
                when (this) {
                    XN -> TODO("unused - impossible facing direction")
                    XP -> TODO("unused - impossible facing direction")
                    YN -> ZP
                    YP -> ZN
                    ZN -> YN
                    ZP -> YP
                }
            }
            XP -> {
                when (this) {
                    XN -> TODO("unused - impossible facing direction")
                    XP -> TODO("unused - impossible facing direction")
                    YN -> ZN
                    YP -> ZP
                    ZN -> YP
                    ZP -> YN
                }
            }
            YN -> {
                when (this) {
                    XN -> ZN
                    XP -> ZP
                    YN -> TODO("unused - impossible facing direction")
                    YP -> TODO("unused - impossible facing direction")
                    ZN -> XP
                    ZP -> XN
                }
            }
            YP -> {
                when (this) {
                    XN -> ZP
                    XP -> ZN
                    YN -> TODO("unused - impossible facing direction")
                    YP -> TODO("unused - impossible facing direction")
                    ZN -> XN
                    ZP -> XP
                }
            }
            ZN -> {
                when (this) {
                    XN -> YP
                    XP -> YN
                    YN -> XN
                    YP -> XP
                    ZN -> TODO("unused - impossible facing direction")
                    ZP -> TODO("unused - impossible facing direction")
                }
            }
            ZP -> {
                when (this) {
                    XN -> YN
                    XP -> YP
                    YN -> XP
                    YP -> XN
                    ZN -> TODO("unused - impossible facing direction")
                    ZP -> TODO("unused - impossible facing direction")
                }
            }
        }
    }

    fun left(axis: HybridNodeDirection): HybridNodeDirection {
        return right(axis).inverse
    }

    fun up(axis: HybridNodeDirection): HybridNodeDirection {
        return axis
    }

    fun down(axis: HybridNodeDirection): HybridNodeDirection {
        return up(axis).inverse
    }

    fun back(): HybridNodeDirection {
        return inverse
    }

}
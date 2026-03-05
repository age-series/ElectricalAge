package mods.eln.misc

enum class HybridNodeLRDU {
    Left, Right, Down, Up, None;

    private val clockwise: HybridNodeLRDU get() {
        return when (this) {
            Left -> Up
            Right -> Down
            Down -> Left
            Up -> Right
            None -> None
        }
    }

    private val counterClockwise: HybridNodeLRDU get() {
        return when (this) {
            Left -> Down
            Right -> Up
            Down -> Right
            Up -> Left
            None -> None
        }
    }

    private val inverse: HybridNodeLRDU get() {
        return when (this) {
            Left -> Right
            Right -> Left
            Down -> Up
            Up -> Down
            None -> None
        }
    }

    fun normalizeLRDU(rotationAxis: HybridNodeDirection, connectionSide: Direction): HybridNodeLRDU {
        return when (rotationAxis) {
            HybridNodeDirection.XN -> {
                when (connectionSide) {
                    Direction.XN -> None
                    Direction.XP -> None
                    Direction.YN -> this.inverse
                    Direction.YP -> this.inverse
                    Direction.ZN -> this.counterClockwise
                    Direction.ZP -> this.clockwise
                }
            }
            HybridNodeDirection.XP -> {
                when (connectionSide) {
                    Direction.XN -> None
                    Direction.XP -> None
                    Direction.YN -> this
                    Direction.YP -> this
                    Direction.ZN -> this.clockwise
                    Direction.ZP -> this.counterClockwise
                }
            }
            HybridNodeDirection.YN -> {
                when (connectionSide) {
                    Direction.XN -> this.inverse
                    Direction.XP -> this.inverse
                    Direction.YN -> None
                    Direction.YP -> None
                    Direction.ZN -> this.inverse
                    Direction.ZP -> this.inverse
                }
            }
            HybridNodeDirection.YP -> {
                when (connectionSide) {
                    Direction.XN -> this
                    Direction.XP -> this
                    Direction.YN -> None
                    Direction.YP -> None
                    Direction.ZN -> this
                    Direction.ZP -> this
                }
            }
            HybridNodeDirection.ZN -> {
                when (connectionSide) {
                    Direction.XN -> this.clockwise
                    Direction.XP -> this.counterClockwise
                    Direction.YN -> this.counterClockwise
                    Direction.YP -> this.clockwise
                    Direction.ZN -> None
                    Direction.ZP -> None
                }
            }
            HybridNodeDirection.ZP -> {
                when (connectionSide) {
                    Direction.XN -> this.counterClockwise
                    Direction.XP -> this.clockwise
                    Direction.YN -> this.clockwise
                    Direction.YP -> this.counterClockwise
                    Direction.ZN -> None
                    Direction.ZP -> None
                }
            }
        }
    }

}
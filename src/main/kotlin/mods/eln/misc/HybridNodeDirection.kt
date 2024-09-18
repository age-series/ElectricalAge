package mods.eln.misc

import org.lwjgl.opengl.GL11

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

    val inverse: HybridNodeDirection
        get() {
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

    fun glNormalizePlacement(axis: HybridNodeDirection) {
        when (axis) {
            XN -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(180f, -1f, 0f, 0f)
                when (this) {
                    XN -> TODO("unused - impossible rotation direction")
                    XP -> TODO("unused - impossible rotation direction")
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                    ZP -> GL11.glRotatef(90f, 0f, 1f, 0f)
                }
            }
            XP -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(0f, -1f, 0f, 0f)
                when (this) {
                    XN -> TODO("unused - impossible rotation direction")
                    XP -> TODO("unused - impossible rotation direction")
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> GL11.glRotatef(90f, 0f, 1f, 0f)
                    ZP -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                }
            }
            YN -> {
                when (this) {
                    XN -> {
                        GL11.glRotatef(180f, -1f, 0f, 0f)
                        GL11.glRotatef(180f, 0f, 1f, 0f)
                    }
                    XP -> {
                        GL11.glRotatef(180f, 1f, 0f, 0f)
                        GL11.glRotatef(0f, 0f, 1f, 0f)
                    }
                    YN -> TODO("unused - impossible rotation direction")
                    YP -> TODO("unused - impossible rotation direction")
                    ZN -> {
                        GL11.glRotatef(180f, 0f, 0f, -1f)
                        GL11.glRotatef(90f, 0f, 1f, 0f)
                    }
                    ZP -> {
                        GL11.glRotatef(180f, 0f, 0f, 1f)
                        GL11.glRotatef(-90f, 0f, 1f, 0f)
                    }
                }
            }
            YP -> {
                when (this) {
                    XN -> {
                        GL11.glRotatef(0f, -1f, 0f, 0f)
                        GL11.glRotatef(180f, 0f, 1f, 0f)
                    }
                    XP -> {
                        GL11.glRotatef(0f, 1f, 0f, 0f)
                        GL11.glRotatef(0f, 0f, 1f, 0f)
                    }
                    YN -> TODO("unused - impossible rotation direction")
                    YP -> TODO("unused - impossible rotation direction")
                    ZN -> {
                        GL11.glRotatef(0f, 0f, 0f, -1f)
                        GL11.glRotatef(90f, 0f, 1f, 0f)
                    }
                    ZP -> {
                        GL11.glRotatef(0f, 0f, 0f, 1f)
                        GL11.glRotatef(-90f, 0f, 1f, 0f)
                    }
                }
            }
            ZN -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(90f, -1f, 0f, 0f)
                when (this) {
                    XN -> GL11.glRotatef(90f, 0f, 1f, 0f)
                    XP -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> TODO("unused - impossible rotation direction")
                    ZP -> TODO("unused - impossible rotation direction")
                }
            }
            ZP -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(-90f, -1f, 0f, 0f)
                when (this) {
                    XN -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                    XP -> GL11.glRotatef(90f, 0f, 1f, 0f)
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> TODO("unused - impossible rotation direction")
                    ZP -> TODO("unused - impossible rotation direction")
                }
            }
        }
    }

}
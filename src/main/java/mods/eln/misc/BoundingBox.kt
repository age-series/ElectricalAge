package mods.eln.misc

import com.google.common.base.Objects
import net.minecraft.util.Vec3
import java.lang.Float.NEGATIVE_INFINITY
import java.lang.Float.POSITIVE_INFINITY

class BoundingBox(xMin: Float, xMax: Float, yMin: Float, yMax: Float, zMin: Float, zMax: Float) {
    val min: Vec3 = Vec3.createVectorHelper(xMin.toDouble(), yMin.toDouble(), zMin.toDouble())
    val max: Vec3 = Vec3.createVectorHelper(xMax.toDouble(), yMax.toDouble(), zMax.toDouble())

    fun merge(other: BoundingBox): BoundingBox {
        return BoundingBox(
            min.xCoord.coerceAtMost(other.min.xCoord).toFloat(),
            max.xCoord.coerceAtLeast(other.max.xCoord).toFloat(),
            min.yCoord.coerceAtMost(other.min.yCoord).toFloat(),
            max.yCoord.coerceAtLeast(other.max.yCoord).toFloat(),
            min.zCoord.coerceAtMost(other.min.zCoord).toFloat(),
            max.zCoord.coerceAtLeast(other.max.zCoord).toFloat()
        )
    }

    fun centre(): Vec3 {
        return Vec3.createVectorHelper(
            min.xCoord + (max.xCoord - min.xCoord) / 2,
            min.yCoord + (max.yCoord - min.yCoord) / 2,
            min.zCoord + (max.zCoord - min.zCoord) / 2
        )
    }

    override fun toString(): String {
        return Objects.toStringHelper(this)
            .add("min", min)
            .add("max", max)
            .toString()
    }

    companion object {
        @JvmStatic
        fun mergeIdentity(): BoundingBox {
            return BoundingBox(POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY)
        }
    }

}

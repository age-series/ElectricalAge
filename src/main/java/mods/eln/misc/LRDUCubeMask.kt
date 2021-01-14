package mods.eln.misc

class LRDUCubeMask {
    var lrduMaskArray = arrayOfNulls<LRDUMask>(6)
    fun getSide(direction: Direction): LRDUMask? {
        return lrduMaskArray[direction.int]
    }

    fun clear() {
        for (lrduMask in lrduMaskArray) {
            lrduMask!!.set(0)
        }
    }

    operator fun set(direction: Direction, lrdu: LRDU?, value: Boolean) {
        get(direction)!![lrdu] = value
    }

    operator fun get(direction: Direction, lrdu: LRDU?): Boolean {
        return get(direction)!![lrdu]
    }

    operator fun get(direction: Direction): LRDUMask? {
        return lrduMaskArray[direction.int]
    }

    fun getTranslate(side: Direction): LRDUMask {
        val mask = LRDUMask()
        for (lrdu in LRDU.values()) {
            val otherSide = side.applyLRDU(lrdu)
            val otherLrdu = otherSide.getLRDUGoingTo(side)
            mask[lrdu] = this[otherSide, otherLrdu]
        }
        return mask
    }

    init {
        for (idx in 0..5) {
            lrduMaskArray[idx] = LRDUMask()
        }
    }
}

package mods.eln.misc

class SlewLimiter {
    var positiveSlewRate = 0f
        private set
    var negativeSlewRate = 0f
        private set
    var target = 0f
    var position = 0f

    constructor(slewRate: Float) {
        setSlewRate(slewRate)
    }

    constructor(positive: Float, negative: Float) {
        setSlewRate(positive, negative)
    }

    fun targetReached(): Boolean {
        return position == target
    }

    fun targetReached(tolerance: Float): Boolean {
        return Math.abs(position - target) <= tolerance
    }

    fun setSlewRate(slewRate: Float) {
        positiveSlewRate = Math.abs(slewRate)
        negativeSlewRate = Math.abs(slewRate)
    }

    fun setSlewRate(positive: Float, negative: Float) {
        positiveSlewRate = Math.abs(positive)
        negativeSlewRate = Math.abs(negative)
    }

    fun step(deltaTime: Float) {
        var delta = target - position
        if (delta > 0f) delta = Math.min(delta, positiveSlewRate * deltaTime) else if (delta < 0f) delta = Math.max(delta, -negativeSlewRate * deltaTime)
        position += delta
    }
}

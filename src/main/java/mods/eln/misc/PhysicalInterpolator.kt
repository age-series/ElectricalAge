package mods.eln.misc

class PhysicalInterpolator(preTao: Float, var accPerSPerError: Float, var slowPerS: Float, var rebond: Float) {
    var target = 0f
    var factorSpeed = 0f
    var factorPos = 0f
    var factorFiltered = 0f
    var ff: Float = 1 / preTao
    var maxSpeed = 1000f
    fun step(deltaT: Float) {
        factorFiltered += (target - factorFiltered) * ff * deltaT
        val error = factorFiltered - factorPos
        factorSpeed *= 1 - slowPerS * deltaT
        factorSpeed += error * accPerSPerError * deltaT
        if (factorSpeed > maxSpeed) factorSpeed = maxSpeed
        if (factorSpeed < -maxSpeed) factorSpeed = -maxSpeed
        factorPos += factorSpeed * deltaT
        if (factorPos > 1.0) {
            factorFiltered = target
            factorPos = 1.0f
            factorSpeed = -factorSpeed * rebond
        }
        if (factorPos < 0.0) {
            factorFiltered = target
            factorPos = 0.0f
            factorSpeed = -factorSpeed * rebond
        }
    }

    fun get(): Float {
        return factorPos
    }

    fun setPos(value: Float) {
        factorPos = value
        factorFiltered = value
        target = value
    }
}

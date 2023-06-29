package mods.eln.misc

class PhysicalInterpolatorNoRebound(preTao: Float, var accPerSPerError: Float, var slowPerS: Float) {
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

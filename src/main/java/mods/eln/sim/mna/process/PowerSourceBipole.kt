package mods.eln.sim.mna.process

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class PowerSourceBipole(private val aPin: State, private val bPin: State, private val aSrc: VoltageSource, private val bSrc: VoltageSource) : IRootSystemPreStepProcess, INBTTReady {

    var p: Double = 0.toDouble()
    internal var Umax: Double = 0.toDouble()
    internal var Imax: Double = 0.toDouble()

    internal fun setMax(Umax: Double, Imax: Double) {
        this.Umax = Umax
        this.Imax = Imax
    }

    fun setImax(imax: Double) {
        Imax = imax
    }

    fun setUmax(umax: Double) {
        Umax = umax
    }

    override fun rootSystemPreStepProcess() {
        val a = aPin.subSystem!!.getTh(aPin, aSrc)
        val b = bPin.subSystem!!.getTh(bPin, bSrc)

        if (a.U.isNaN()) {
            a.U = 0.0
            a.R = MnaConst.highImpedance
        }
        if (b.U.isNaN()) {
            b.U = 0.0
            b.R = MnaConst.highImpedance
        }

        val Uth = a.U - b.U
        val Rth = a.R + b.R
        if (Uth >= Umax) {
            aSrc.u = a.U
            bSrc.u = b.U
        } else {
            var U = (Math.sqrt(Uth * Uth + 4.0 * p * Rth) + Uth) / 2
            U = Math.min(Math.min(U, Umax), Uth + Rth * Imax)
            if (java.lang.Double.isNaN(U)) U = 0.0

            val I = (Uth - U) / Rth
            aSrc.u = a.U - I * a.R
            bSrc.u = b.U + I * b.R
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        p = nbt.getDouble(str + "P")
        setUmax(nbt.getDouble(str + "Umax"))
        setImax(nbt.getDouble(str + "Imax"))
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "P", p)
        nbt.setDouble(str + "Umax", Umax)
        nbt.setDouble(str + "Imax", Imax)
    }
}

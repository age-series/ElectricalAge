package mods.eln.sim.mna.component

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class PowerSource(name: String, aPin: State) : VoltageSource(name, aPin, null), IRootSystemPreStepProcess, INBTTReady {

    override var p: Double = 0.0
    var Umax: Double = 0.0
    var Imax: Double = 0.0

    fun getEffectiveP() = getBipoleU() * getCurrent()

    override fun quitSubSystem() {
        getSubSystem()!!.root?.removeProcess(this)
        super.quitSubSystem()
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        getSubSystem()!!.root?.addProcess(this)
        s.addProcess(this)
    }

    override fun rootSystemPreStepProcess() {
        val t = aPin!!.subSystem!!.getTh(aPin!!, this)

        var U = (Math.sqrt(t.U * t.U + 4.0 * p * t.R) + t.U) / 2
        U = Math.min(Math.min(U, Umax), t.U + t.R * Imax)
        if (java.lang.Double.isNaN(U)) U = 0.0
        if (U < t.U) U = t.U

        u = U
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        super.readFromNBT(nbt, strl)

        strl += name

        p = nbt.getDouble(strl + "P")
        Umax = nbt.getDouble(strl + "Umax")
        Imax = nbt.getDouble(strl + "Imax")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        super.writeToNBT(nbt, strl)

        strl += name

        nbt.setDouble(strl + "P", p)
        nbt.setDouble(strl + "Umax", Umax)
        nbt.setDouble(strl + "Imax", Imax)
    }
}

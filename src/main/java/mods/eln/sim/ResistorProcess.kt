package mods.eln.sim

import mods.eln.sim.core.IProcess
import mods.eln.sim.mna.passive.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.thermal.ThermalLoad
import mods.eln.sixnode.resistor.ResistorDescriptor
import mods.eln.sixnode.resistor.ResistorElement

/**
 * Created by svein on 07/08/15.
 */
class ResistorProcess(internal var element: ResistorElement, internal var r: Resistor, internal var thermal: ThermalLoad, internal var descriptor: ResistorDescriptor) : IProcess {

    private var lastR = -1.0

    override fun process(time: Double) {
        var newR = Math.max(
                MnaConst.noImpedance,
                element.nominalRs * (1 + descriptor.tempCoef * thermal.Tc))
        if (element.control != null) {
            newR *= (element.control.normalized + 0.01) / 1.01
        }
        if (newR > lastR * 1.01 || newR < lastR * 0.99) {
            r.r = newR
            lastR = newR
            element.needPublish()
        }

        // NOTE: While a thermistor seems like a neat idea, it may be prohibitively computationally expensive,
        //       since it can be using a lot of CPU time (every time it updates the resistance, the A matrix needs to
        //       be recalculated
        //
        //        /*
        //        * https://en.wikipedia.org/wiki/Thermistor
        //        *
        //        * R = exp[(x - y/2)^(1/3) - (x + y/2)^(1/3)]
        //        * y = 1/c*(a - 1/T)
        //        * x = sqrt((b/3c)^3 + (y/2)^2)
        //        */
        //
        //        double T = thermal.Tc;
        //        double y = 1.0 / descriptor.shC * (descriptor.shA - 1.0/T);
        //        double x = Math.sqrt(Math.pow(descriptor.shB / 3.0 / descriptor.shC, 3) + Math.pow(y / 2.0, 2));
        //        double R = Math.exp(Math.pow(x - y/2, 1.0/3.0) - Math.pow(x + y/2, 1.0/3.0));
        //
        //        r.setR(R);
    }
}

package mods.eln.gridnode.electricalpole

import mods.eln.gridnode.GridDescriptor
import mods.eln.misc.Obj3D
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor

enum class Kind {
    OVERHEAD,
    TRANSFORMER_TO_GROUND,
    SHUNT_TO_GROUND,
}

/**
 * Created by svein on 07/08/15.
 */
class ElectricalPoleDescriptor(name: String,
                               obj: Obj3D, cableTexture: String,
                               cableDescriptor: ElectricalCableDescriptor,
                               val kind: Kind,
                               connectRange: Int, val voltageLimit: Double)
    : GridDescriptor(name, obj, ElectricalPoleElement::class.java, ElectricalPoleRender::class.java, cableTexture, cableDescriptor, connectRange) {
    val minimalLoadToHum = 0.2f

    val includeTransformer get() = kind == Kind.TRANSFORMER_TO_GROUND
    val isShunt get() = kind == Kind.SHUNT_TO_GROUND

    init {
        obj.getPart("foot")?.let {
            // Don't draw the foot on the T1 utility pole.
            // XXX is there a better way to check for the model?
            if (kind != Kind.OVERHEAD || name == "Transmission Tower")
                static_parts.add(it)
        }
        if (includeTransformer) {
            arrayOf("transformer", "cables").forEach {
                obj.getPart(it)?.let { rotating_parts.add(it) }
            }
        }
    }

    override fun hasCustomIcon() = this.name == "Transmission Tower"
}

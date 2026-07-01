package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaRegistrar
import mods.eln.simplenode.computerprobe.ComputerProbeBlock

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaRegistrar", modid = "Waila")
object WailaIntegration {

    @JvmStatic
    fun callbackRegister(registrar: IWailaRegistrar) {
        val transparentNodeProvider = TransparentNodeWailaProvider()
        val sixNodeProvider = SixNodeWailaProvider()
        val ghostNodeProvider = GhostNodeWailaProvider(transparentNodeProvider, sixNodeProvider)
        val computerProbeProvider = ComputerProbeWailaProvider()

        registrar.registerBodyProvider(transparentNodeProvider, mods.eln.node.transparent.TransparentNodeBlock::class.java)
        registrar.registerBodyProvider(computerProbeProvider, ComputerProbeBlock::class.java)
        registrar.registerNBTProvider(computerProbeProvider, ComputerProbeBlock::class.java)

        registrar.registerHeadProvider(ghostNodeProvider, mods.eln.ghost.GhostBlock::class.java)
        registrar.registerBodyProvider(ghostNodeProvider, mods.eln.ghost.GhostBlock::class.java)
        registrar.registerStackProvider(ghostNodeProvider, mods.eln.ghost.GhostBlock::class.java)

        registrar.registerHeadProvider(sixNodeProvider, mods.eln.node.six.SixNodeBlock::class.java)
        registrar.registerBodyProvider(sixNodeProvider, mods.eln.node.six.SixNodeBlock::class.java)
        registrar.registerStackProvider(sixNodeProvider, mods.eln.node.six.SixNodeBlock::class.java)
    }
}

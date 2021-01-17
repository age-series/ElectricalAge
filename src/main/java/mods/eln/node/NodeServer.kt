package mods.eln.node

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraft.entity.player.EntityPlayerMP

class NodeServer {
    fun init() {
        //	NodeBlockEntity.nodeAddedList.clear();
    }

    fun stop() {
        //	NodeBlockEntity.nodeAddedList.clear();
    }

    var counter = 0
    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val server = FMLCommonHandler.instance().minecraftServerInstance
        if (server != null) {
            for (node in NodeManager.instance!!.nodeList) {
                if (node.needPublish) {
                    node.publishToAllPlayer()
                }
            }
            for (obj in server.configurationManager.playerEntityList) {
                val player = obj as EntityPlayerMP?
                var openContainerNode: NodeBase? = null
                var container: INodeContainer? = null
                if (player!!.openContainer != null && player.openContainer is INodeContainer) {
                    container = player.openContainer as INodeContainer
                    openContainerNode = container.node
                }
                for (node in NodeManager.instance!!.nodeList) {
                    if (node === openContainerNode) {
                        if (counter % (1 + container!!.refreshRateDivider) == 0) node.publishToPlayer(player)
                    }
                }
            }
            counter++
        }
    }

    init {
        FMLCommonHandler.instance().bus().register(this)
    }
}

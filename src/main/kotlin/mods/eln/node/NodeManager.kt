package mods.eln.node

import mods.eln.misc.Coordinate
import mods.eln.misc.Utils.getTags
import mods.eln.misc.Utils.println
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeElement
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.WorldSavedData
import java.util.*

class NodeManager(par1Str: String?) : WorldSavedData(par1Str) {
    val nodeArray: HashMap<Coordinate, NodeBase>
    val nodes: ArrayList<NodeBase>
    val nodeList: Collection<NodeBase>
        get() = nodeArray.values

    fun addNode(node: NodeBase) {
        // nodeArray.add(node);
        val old = nodeArray.put(node.coordinate, node)
        if (old != null) {
            nodes.remove(old)
        }
        nodes.add(node)
        println("NodeManager has " + nodeArray.size + "node")
        // nodeArray.put(new NodeIdentifier(node), node);
    }

    fun removeNode(node: NodeBase?) {
        if (node == null) return
        nodeArray.remove(node.coordinate)
        nodes.remove(node)
        println("NodeManager has " + nodeArray.size + "node")
    }

    fun removeCoordonate(c: Coordinate?) {
        // nodeArray.remove(node);
        val n = nodeArray.remove(c)
        if (n != null) nodes.remove(n)
        println("NodeManager has " + nodeArray.size + "node")
    }

    override fun isDirty(): Boolean {
        return true
    }

    override fun readFromNBT(nbt: NBTTagCompound) {}

    override fun writeToNBT(nbt: NBTTagCompound) {}

    fun getNodeFromCoordonate(nodeCoordinate: Coordinate?): NodeBase? {
        return nodeArray[nodeCoordinate]
    }

    fun getTransparentNodeFromCoordinate(coord: Coordinate?): TransparentNodeElement? {
        val base = getNodeFromCoordonate(coord)
        if (base is TransparentNode) {
            return base.element
        }
        return null
    }

    var rand = Random()
    val randomNode: NodeBase?
        get() = if (nodes.isEmpty()) null else nodes[rand.nextInt(nodes.size)]

    fun loadFromNbt(nbt: NBTTagCompound?) {
        val addedNode: MutableList<NodeBase> = ArrayList()
        for (o in getTags(nbt!!)) {
            val tag = o
            val nodeClass = UUIDToClass[tag.getString("tag")]
            try {
                val node = nodeClass!!.getConstructor().newInstance() as NodeBase
                node.readFromNBT(tag)
                addNode(node)
                addedNode.add(node)
                node.initializeFromNBT()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        for (n in addedNode) {
            n.globalBoot()
        }
    }

    fun saveToNbt(nbt: NBTTagCompound, dim: Int) {
        var nodeCounter = 0
        val nodesCopy: MutableList<NodeBase> = ArrayList()
        nodesCopy.addAll(nodes)
        for (node in nodesCopy) {
            try {
                if (node.mustBeSaved() == false) continue
                if (dim != Int.MIN_VALUE && node.coordinate.dimension != dim) continue
                val nbtNode = NBTTagCompound()
                nbtNode.setString("tag", node.nodeUuid)
                node.writeToNBT(nbtNode)
                nbt.setTag("n" + nodeCounter++, nbtNode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clear() {
        nodes.clear()
        nodeArray.clear()
    }

    fun unload(dimensionId: Int) {
        val i = nodes.iterator()
        while (i.hasNext()) {
            val n = i.next()
            if (n.coordinate.dimension == dimensionId) {
                n.unload()
                i.remove()
                nodeArray.remove(n.coordinate)
            }
        }
    }

    companion object {
        @JvmField
        var instance: NodeManager? = null
        val UUIDToClass = HashMap<String, Class<*>>()
        @JvmStatic
        fun registerUuid(uuid: String, classType: Class<*>) {
            UUIDToClass[uuid] = classType
        }
    }

    // private ArrayList<Node> nodeArray = new ArrayList<Node>();
    init {
        nodeArray = HashMap()
        nodes = ArrayList()
        instance = this
    }
}

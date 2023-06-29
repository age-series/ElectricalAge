package mods.eln.node

import mods.eln.sim.IProcess

class NodePeriodicPublishProcess(var node: NodeBase, var base: Double, var random: Double) : IProcess {
    var counter = 0.0

    override fun process(time: Double) {
        counter -= time
        if (counter <= 0.0) {
            counter += base + Math.random() * random
            node.needPublish = true
        }
    }

    fun reconfigure(base: Double, random: Double) {
        this.base = base
        this.random = random
        counter = 0.0
    }
}

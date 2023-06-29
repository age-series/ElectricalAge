package mods.eln.node

interface IThermalDestructorDescriptor {
    val thermalDestructionMax: Double
    val thermalDestructionStart: Double
    val thermalDestructionPerOverflow: Double
    val thermalDestructionProbabilityPerOverflow: Double
}

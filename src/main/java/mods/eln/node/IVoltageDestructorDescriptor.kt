package mods.eln.node

interface IVoltageDestructorDescriptor {
    val voltageDestructionMax: Double
    val voltageDestructionStart: Double
    val voltageDestructionPerOverflow: Double
    val voltageDestructionProbabilityPerOverflow: Double
}

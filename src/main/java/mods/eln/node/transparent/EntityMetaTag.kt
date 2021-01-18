package mods.eln.node.transparent

/**
 * Used to differentiate between subclasses of TransparentNodeEntity, so that
 * our TEs can implement different interfaces depending on what functionality
 * they have.
 */
enum class EntityMetaTag(val meta: Int, val cls: Class<*>) {
    Fluid(1, TransparentNodeEntityWithFluid::class.java),
    Basic(3, TransparentNodeEntity::class.java); // 3, because this is the default value used in pre-metatag worlds
}

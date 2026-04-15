package mods.eln.config

import mods.eln.Eln
import mods.eln.item.electricalitem.OreScannerConfigElement
import mods.eln.misc.Utils
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraftforge.oredict.OreDictionary

/**
 * Loads ore scanner configuration from JsonConfig and resolves ore entries
 * to block keys for use by the ore scanner and auto miner.
 */
object OreScannerConfigLoader {

    private const val ORE_FACTORS_PATH = "tools.xrayScanner.oreFactors"
    private const val AUTO_DISCOVERY_KEY = "tools.xrayScanner.autoDiscoverOreDictionaryOres"
    private const val AUTO_DISCOVERY_FACTOR_KEY = "tools.xrayScanner.autoDiscoveryOreFactor"
    private const val DEFAULT_OTHER_MOD_FACTOR = 0.15

    /**
     * Loads ore scanner entries from config. Each config key is either:
     * - A block reference (contains ':'): "modid:name" or "modid:name:meta"
     * - An OreDictionary name (no ':'): "oreCopper"
     *
     * Returns deduplicated list where later entries override earlier by blockKey.
     */
    fun loadOreScannerConfig(): List<OreScannerConfigElement> {
        val config = Eln.config
        val oreFactors = config.getStringDoubleMap(ORE_FACTORS_PATH)
        val blockKeyMap = linkedMapOf<Int, Float>()

        for ((key, factor) in oreFactors) {
            val f = factor.toFloat()
            if (key.contains(':')) {
                resolveBlockReference(key, f, blockKeyMap)
            } else {
                resolveOreDictionaryName(key, f, blockKeyMap)
            }
        }

        return blockKeyMap.map { (blockKey, factor) ->
            OreScannerConfigElement(blockKey, factor)
        }
    }

    /**
     * Auto-discovers ores from OreDictionary that are not already in configEntries.
     * Only runs if autoDiscoverOreDictionaryOres is enabled.
     * Uses autoDiscoveryOreFactor for all auto-discovered ores.
     */
    fun loadOreDictionaryAutoDiscovery(existingBlockKeys: Map<Int, Float>): List<OreScannerConfigElement> {
        val config = Eln.config
        if (!config.getBooleanOrElse(AUTO_DISCOVERY_KEY, true)) {
            return emptyList()
        }

        val otherModFactor = config.getDoubleOrElse(AUTO_DISCOVERY_FACTOR_KEY, DEFAULT_OTHER_MOD_FACTOR).toFloat()
        val results = mutableListOf<OreScannerConfigElement>()

        for (name in OreDictionary.getOreNames()) {
            if (name == null || !name.startsWith("ore")) continue
            for (stack in OreDictionary.getOres(name)) {
                val damage = stack.itemDamage
                val meta = if (damage == OreDictionary.WILDCARD_VALUE) 0 else stack.item.getMetadata(damage)
                val blockKey = Utils.getItemId(stack) + (meta shl 12)
                if (blockKey !in existingBlockKeys) {
                    results.add(OreScannerConfigElement(blockKey, otherModFactor))
                }
            }
        }

        return results
    }

    private fun resolveBlockReference(key: String, factor: Float, blockKeyMap: MutableMap<Int, Float>) {
        val parts = key.split(':')
        if (parts.size < 2) {
            return
        }
        val modid = parts[0]
        val name = parts[1]
        val meta = if (parts.size >= 3) parts[2].toIntOrNull() ?: 0 else 0

        val block = GameRegistry.findBlock(modid, name)
        if (block == null) {
            return
        }

        val blockKey = Block.getIdFromBlock(block) + (meta shl 12)
        blockKeyMap[blockKey] = factor
    }

    private fun resolveOreDictionaryName(key: String, factor: Float, blockKeyMap: MutableMap<Int, Float>) {
        val ores = OreDictionary.getOres(key)
        if (ores.isEmpty()) {
            return
        }

        for (stack in ores) {
            val damage = stack.itemDamage
            val meta = if (damage == OreDictionary.WILDCARD_VALUE) 0 else stack.item.getMetadata(damage)
            val blockKey = Utils.getItemId(stack) + (meta shl 12)
            blockKeyMap[blockKey] = factor
        }
    }
}

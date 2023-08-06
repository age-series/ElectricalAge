package mods.eln.cable

import mods.eln.misc.UtilsClient.bindTexture
import net.minecraft.util.ResourceLocation

class CableRenderDescriptor(modName: String?, cableTexture: String?, var widthPixel: Float, var heightPixel: Float) {
    var width: Float = widthPixel / 16
    var height: Float = heightPixel / 16
    var widthDiv2: Float = width / 2
    @JvmField
    var cableTexture = ResourceLocation(modName, cableTexture)

    fun bindCableTexture() {
        bindTexture(cableTexture)
    }
}

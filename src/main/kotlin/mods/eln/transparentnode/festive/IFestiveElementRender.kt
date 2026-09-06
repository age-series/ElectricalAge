package mods.eln.transparentnode.festive

interface IFestiveElementRender {
    var lampSupplyChannel: String
    var activeLampSupplyConnection: Boolean

    fun clientSetString(id: Byte, text: String)
}
package mods.eln.item

import mods.eln.Eln
import mods.eln.falstad.FalstadImporter
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.UtilsClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

private const val MAX_FALSTAD_IMPORT_BYTES = 32000

class FalstadImportToolDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "configcopytool") {
    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer): ItemStack {
        if (w.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(FalstadImportGui())
        }
        return s
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Right click to import a Falstad netlist from the clipboard."))
        list.add(tr("Places a simplified ELN build on flat ground near the player."))
    }
}

class FalstadImportGui : GuiScreen() {
    override fun initGui() {
        super.initGui()
        buttonList.clear()
        buttonList.add(GuiButton(0, width / 2 - 70, height / 2 - 10, 140, 20, tr("Paste Clipboard")))
        buttonList.add(GuiButton(1, width / 2 - 70, height / 2 + 16, 140, 20, tr("Cancel")))
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> {
                val clipboard = getClipboardString().orEmpty().trim()
                val player = Minecraft.getMinecraft().thePlayer
                if (clipboard.isEmpty()) {
                    if (player != null) addChatMessage(player, tr("Falstad import: clipboard is empty."))
                    return
                }

                if (!looksLikeFalstadData(clipboard)) {
                    if (player != null) addChatMessage(player, tr("Falstad import: clipboard is not valid Falstad data."))
                    return
                }

                val bytes = clipboard.toByteArray(StandardCharsets.UTF_8)
                if (bytes.size > MAX_FALSTAD_IMPORT_BYTES) {
                    if (player != null) {
                        addChatMessage(
                            player,
                            tr(
                                "Falstad import: netlist is too large to send (%1$ bytes, limit %2$).",
                                bytes.size,
                                MAX_FALSTAD_IMPORT_BYTES
                            )
                        )
                    }
                    return
                }
                val bos = ByteArrayOutputStream(bytes.size + 8)
                val stream = DataOutputStream(bos)
                stream.writeByte(Eln.packetFalstadImport.toInt())
                stream.writeInt(bytes.size)
                stream.write(bytes)
                UtilsClient.sendPacketToServer(bos)
                mc.displayGuiScreen(null)
            }
            else -> mc.displayGuiScreen(null)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        drawCenteredString(fontRendererObj, tr("Falstad Import Tool"), width / 2, height / 2 - 42, 0xFFFFFF)
        drawCenteredString(fontRendererObj, tr("Reads Falstad text from the system clipboard."), width / 2, height / 2 - 28, 0xA0A0A0)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame(): Boolean = false
}

object FalstadImportPacketHandler {
    fun handle(player: EntityPlayerMP, bytes: ByteArray) {
        FalstadImporter.importFromClipboardAsync(player, String(bytes, StandardCharsets.UTF_8))
    }
}

private fun looksLikeFalstadData(text: String): Boolean {
    val trimmed = text.trimStart()
    if (trimmed.startsWith("<cir")) return true
    val firstLine = trimmed.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("$") }
        ?: return false
    val token = firstLine.substringBefore(' ')
    return token.matches(Regex("[A-Za-z]+|\\d+"))
}

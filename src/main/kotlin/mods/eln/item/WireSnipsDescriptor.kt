package mods.eln.item

import mods.eln.Eln
import mods.eln.GuiHandler
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.IGuiObject
import mods.eln.gui.SlotWithSkin
import mods.eln.i18n.I18N.tr
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.misc.Utils
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.InventoryBasic
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import kotlin.math.min

class WireSnipsDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "wiresnips") {
    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Right click to cut utility wire in your inventory"))
    }

    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer): ItemStack {
        if (!w.isRemote) {
            p.openGui(Eln.instance, GuiHandler.wireSnipsOpen, w, 0, 0, 0)
        }
        return s
    }
}

class WireSnipsContainer(private val player: EntityPlayer) : Container() {
    companion object {
        private const val cutActionBase = 1_000_000_000
        private const val cutActionSlotScale = 1_000_000
        private const val inputSlotIndex = 0
        private const val playerInventoryStart = 1
        private const val playerInventoryEnd = playerInventoryStart + 36
    }

    private val inputInventory = InventoryBasic("wireSnips", false, 1)

    init {
        addSlotToContainer(object : SlotWithSkin(inputInventory, inputSlotIndex, 8, 32, SlotSkin.medium) {
            override fun getSlotStackLimit(): Int = 1
        })
        bindPlayerInventory()
    }

    private fun bindPlayerInventory() {
        for (row in 0..2) {
            for (column in 0..8) {
                addSlotToContainer(SlotWithSkin(player.inventory, column + row * 9 + 9, 8 + column * 18, 118 + row * 18, SlotSkin.medium))
            }
        }
        for (column in 0..8) {
            addSlotToContainer(SlotWithSkin(player.inventory, column, 8 + column * 18, 176, SlotSkin.medium))
        }
    }

    fun selectedWireStack(): ItemStack? = inputInventory.getStackInSlot(inputSlotIndex)

    fun selectedWireDescriptor(): UtilityCableDescriptor? {
        val stack = selectedWireStack() ?: return null
        return stack.utilityCableDescriptor()
    }

    fun encodeCutAction(lengthMeters: Double): Int {
        val clampedMeters = lengthMeters.toInt().coerceAtLeast(1)
        return cutActionBase + inputSlotIndex * cutActionSlotScale + clampedMeters
    }

    override fun enchantItem(player: EntityPlayer, action: Int): Boolean {
        if (action < cutActionBase) return false
        val encoded = action - cutActionBase
        val slot = encoded / cutActionSlotScale
        val targetLength = (encoded % cutActionSlotScale).toDouble()
        if (slot != inputSlotIndex || targetLength <= 0.0) return false

        val stack = inputInventory.getStackInSlot(inputSlotIndex) ?: return false
        val descriptor = UtilityCableDescriptor.allDescriptors().firstOrNull { it.checkSameItemStack(stack) } ?: return false
        val available = descriptor.getRemainingLengthMeters(stack)
        if (targetLength >= available) return false

        val cutStack = descriptor.newItemStack(1)
        descriptor.setRemainingLengthMeters(cutStack, targetLength)
        descriptor.setRemainingLengthMeters(stack, available - targetLength)
        inputInventory.markDirty()
        player.inventory.markDirty()

        if (!player.inventory.addItemStackToInventory(cutStack)) {
            player.dropPlayerItemWithRandomChoice(cutStack, false)
        }
        if (descriptor.getRemainingLengthMeters(stack) <= 0.0) {
            inputInventory.setInventorySlotContents(inputSlotIndex, null)
        }
        detectAndSendChanges()
        return true
    }

    override fun canInteractWith(player: EntityPlayer): Boolean = true

    override fun transferStackInSlot(player: EntityPlayer, slotId: Int): ItemStack? {
        val slot = inventorySlots[slotId] as? Slot ?: return null
        if (!slot.hasStack) return null

        val stack = slot.stack
        val original = stack.copy()
        if (slotId == inputSlotIndex) {
            if (!mergeItemStack(stack, playerInventoryStart, playerInventoryEnd, true)) return null
        } else {
            val descriptor = stack.utilityCableDescriptor()
            if (descriptor == null || descriptor.getRemainingLengthMeters(stack) <= 0.0) {
                return null
            }
            val inputSlot = inventorySlots[inputSlotIndex] as Slot
            if (inputSlot.hasStack) {
                return null
            }
            val moved = min(stack.stackSize, inputSlot.slotStackLimit)
            val movedStack = stack.copy()
            movedStack.stackSize = moved
            inputSlot.putStack(movedStack)
            stack.stackSize -= moved
        }

        if (stack.stackSize <= 0) {
            slot.putStack(null)
        } else {
            slot.onSlotChanged()
        }
        return original
    }

    override fun onContainerClosed(player: EntityPlayer) {
        super.onContainerClosed(player)
        val stack = inputInventory.getStackInSlot(inputSlotIndex) ?: return
        inputInventory.setInventorySlotContents(inputSlotIndex, null)
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropPlayerItemWithRandomChoice(stack, false)
        }
    }

    private fun ItemStack.utilityCableDescriptor(): UtilityCableDescriptor? {
        return if (item === Eln.sixNodeItem) {
            Eln.sixNodeItem.getDescriptor(this) as? UtilityCableDescriptor
        } else {
            GenericItemBlockUsingDamageDescriptor.getDescriptor(this, UtilityCableDescriptor::class.java) as? UtilityCableDescriptor
        }
    }
}

class WireSnipsGui(player: EntityPlayer) : GuiContainerEln(WireSnipsContainer(player)) {
    private val snipsContainer: WireSnipsContainer
        get() = inventorySlots as WireSnipsContainer

    private lateinit var cutButton: GuiButtonEln
    private lateinit var lengthField: GuiTextFieldEln

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 216, 8, 134)
    }

    override fun initGui() {
        super.initGui()
        lengthField = newGuiTextField(8, 72, 58).apply {
            text = "32"
            setComment(0, tr("Cut length in whole meters"))
        }
        cutButton = newGuiButton(72, 70, 96, tr("Cut Wire"))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        when (obj) {
            cutButton -> {
                val length = lengthField.text.toDoubleOrNull() ?: return
                Minecraft.getMinecraft().playerController.sendEnchantPacket(snipsContainer.windowId, snipsContainer.encodeCutAction(length))
            }
        }
    }

    override fun preDraw(f: Float, mouseX: Int, mouseY: Int) {
        super.preDraw(f, mouseX, mouseY)
        val descriptor = snipsContainer.selectedWireDescriptor()
        cutButton.enabled = descriptor != null && (lengthField.text.toDoubleOrNull() ?: 0.0) > 0.0
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        val stack = snipsContainer.selectedWireStack()
        val descriptor = snipsContainer.selectedWireDescriptor()
        drawString(8, 6, tr("Wire Snips"))
        drawString(8, 20, tr("Input Wire"))
        drawString(8, 62, tr("Length (m)"))
        if (stack == null) {
            drawString(8, 104, tr("Insert a wire coil to cut"))
            return
        }
        if (descriptor == null) {
            drawString(8, 104, tr("Input must be a utility wire"))
            return
        }
        drawString(8, 104, stack.displayName)
        drawString(8, 116, tr("Remaining: %1$ m", Utils.plotValue(descriptor.getRemainingLengthMeters(stack))))
    }
}

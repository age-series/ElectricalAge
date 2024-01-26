@file:Suppress("NAME_SHADOWING")
package mods.eln.misc

import cpw.mods.fml.common.network.internal.FMLProxyPacket
import mods.eln.Eln
import mods.eln.GuiHandler
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.node.six.SixNodeEntity
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C17PacketCustomPayload
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import net.minecraft.world.EnumSkyBlock
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.sqrt

object UtilsClient {
    @JvmField
    var guiLastOpen: GuiScreen? = null
    var lightmapTexUnitTextureEnable = false
    internal var itemRenderer: RenderItem? = null
    @JvmStatic
    var uuid = Int.MIN_VALUE
        get() {
            if (field > -1) field = Int.MIN_VALUE
            return field++
        }
        private set
    val whiteTexture = ResourceLocation("eln", "sprites/cable.png")
    val portableBatteryOverlayResource = ResourceLocation("eln", "sprites/portablebatteryoverlay.png")
    fun distanceFromClientPlayer(@Suppress("UNUSED_PARAMETER") world: World?, xCoord: Int, yCoord: Int, zCoord: Int): Float {
        val player = Minecraft.getMinecraft().thePlayer
        return Math.sqrt((xCoord - player.posX) * (xCoord - player.posX) + (yCoord - player.posY) * (yCoord - player.posY) + (zCoord - player.posZ) * (zCoord - player.posZ)).toFloat()
    }

    @JvmStatic
    fun distanceFromClientPlayer(tileEntity: SixNodeEntity): Float {
        return distanceFromClientPlayer(tileEntity.worldObj, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
    }

    val clientPlayer: EntityClientPlayerMP
        get() = Minecraft.getMinecraft().thePlayer

    fun drawHaloNoLightSetup(halo: Obj3DPart?, r: Float, g: Float, b: Float, w: World, x: Int, y: Int, z: Int, bilinear: Boolean) {
        if (halo == null) return
        if (bilinear) enableBilinear()
        val light = getLight(w, x, y, z) * 19 / 15 - 4
        val e: Entity = clientPlayer
        val d = (Math.abs(x - e.posX) + Math.abs(y - e.posY) + Math.abs(z - e.posZ)).toFloat()
        GL11.glColor4f(r, g, b, 1f - light / 15f)
        halo.draw(d * 20, 1f, 0f, 0f)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        if (bilinear) disableBilinear()
    }

    @JvmStatic
    fun clientOpenGui(gui: GuiScreen?) {
        guiLastOpen = gui
        val clientPlayer = clientPlayer
        clientPlayer.openGui(Eln.instance, GuiHandler.genericOpen, clientPlayer.worldObj, 0, 0, 0)
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, r: Float, g: Float, b: Float, w: World, x: Int, y: Int, z: Int, bilinear: Boolean) {
        disableLight()
        enableBlend()
        drawHaloNoLightSetup(halo, r, g, b, w, x, y, z, bilinear)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawHaloNoLightSetup(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: TileEntity, bilinear: Boolean) {
        drawHaloNoLightSetup(halo, r, g, b, e.worldObj, e.xCoord, e.yCoord, e.zCoord, bilinear)
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: TileEntity, bilinear: Boolean) {
        drawHalo(halo, r, g, b, e.worldObj, e.xCoord, e.yCoord, e.zCoord, bilinear)
    }

    @JvmStatic
    fun drawHaloNoLightSetup(halo: Obj3DPart?, @Suppress("UNUSED_PARAMETER") distance: Float) {
        if (halo == null) return
        halo.faceGroup[0].bindTexture()
        enableBilinear()
        halo.drawNoBind()
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, distance: Float) {
        disableLight()
        enableBlend()
        drawHaloNoLightSetup(halo, distance)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawHaloNoLightSetup(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: Entity, bilinear: Boolean) {
        if (halo == null) return
        if (bilinear) enableBilinear()
        val light = getLight(e.worldObj, MathHelper.floor_double(e.posX), MathHelper.floor_double(e.posY), MathHelper.floor_double(e.posZ))
        // light =
        // e.worldObj.getLightBrightnessForSkyBlocks(MathHelper.floor_double(e.posX),
        // MathHelper.floor_double(e.posY), MathHelper.floor_double(e.posZ),0);
        // Utils.println(light);
        GL11.glColor4f(r, g, b, 1f - light / 15f)
        halo.draw()
        GL11.glColor4f(1f, 1f, 1f, 1f)
        if (bilinear) disableBilinear()
    }

    @JvmStatic
    fun drawHalo(halo: Obj3DPart?, r: Float, g: Float, b: Float, e: Entity, bilinear: Boolean) {
        disableLight()
        enableBlend()
        drawHaloNoLightSetup(halo, r, g, b, e, bilinear)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun enableBilinear() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
    }

    @JvmStatic
    fun disableBilinear() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    }

    @JvmStatic
    fun disableCulling() {
        GL11.glDisable(GL11.GL_CULL_FACE)
    }

    @JvmStatic
    fun enableCulling() {
        GL11.glEnable(GL11.GL_CULL_FACE)
    }

    @JvmStatic
    fun disableTexture() {
        bindTexture(whiteTexture)
        //GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    @JvmStatic
    fun enableTexture() {
        //GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @JvmStatic
    fun disableLight() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        lightmapTexUnitTextureEnable = GL11.glGetBoolean(GL11.GL_TEXTURE_2D)
        if (lightmapTexUnitTextureEnable) GL11.glDisable(GL11.GL_TEXTURE_2D)
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit)
        GL11.glDisable(GL11.GL_LIGHTING)
    }

    @JvmStatic
    fun enableLight() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        if (lightmapTexUnitTextureEnable) GL11.glEnable(GL11.GL_TEXTURE_2D)
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit)
        GL11.glEnable(GL11.GL_LIGHTING)
    }

    @JvmStatic
    fun enableBlend() {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        // GL11.glDepthMask(true);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.02f)
        // GL11.glDisable(GL11.GL_ALPHA_TEST);
        /*
         * Utils.println(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) + " " + GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA) + " " + GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) + " " + GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
		 */

        // Utils.println(GL11.glGetInteger(GL11.GL_BLEND_SRC) + " " + GL11.glGetInteger(GL11.GL_BLEND_DST) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
        /*
		 * GL11.glEnable(2977); GL11.glEnable(3042);
		 */
        // OpenGlHelper.glBlendFunc(770, 770, 771, 771);
    }

    @JvmStatic
    fun disableBlend() {
        GL11.glDisable(GL11.GL_BLEND)

        // GL11.glDepthMask(true);
        // GL11.glEnable(GL11.GL_ALPHA_TEST);
        // GL11.glDisable(GL11.GL_BLEND);
        // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Utils.println(GL11.glGetInteger(GL11.GL_BLEND_SRC) + " " + GL11.glGetInteger(GL11.GL_BLEND_DST) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
        // GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
        // GL11.glBlendFunc(1, 1);
        // GL11.glDisable(3042);

        // OpenGlHelper.glBlendFunc(1, 1, 1, 1);
    }

    fun drawIcon(type: ItemRenderType) {
        enableBlend()
        when (type) {
            ItemRenderType.INVENTORY -> {
                disableCulling()
                GL11.glBegin(GL11.GL_QUADS)
                GL11.glTexCoord2f(1f, 0f)
                GL11.glVertex3f(16f, 0f, 0f)
                GL11.glTexCoord2f(0f, 0f)
                GL11.glVertex3f(0f, 0f, 0f)
                GL11.glTexCoord2f(0f, 1f)
                GL11.glVertex3f(0f, 16f, 0f)
                GL11.glTexCoord2f(1f, 1f)
                GL11.glVertex3f(16f, 16f, 0f)
                GL11.glEnd()
                enableCulling()
            }
            ItemRenderType.ENTITY -> {
                disableCulling()
                GL11.glBegin(GL11.GL_QUADS)
                GL11.glTexCoord2f(1f, 1f)
                GL11.glVertex3f(0f, 0f, 0.5f)
                GL11.glTexCoord2f(0f, 1f)
                GL11.glVertex3f(0.0f, 0f, -0.5f)
                GL11.glTexCoord2f(0f, 0f)
                GL11.glVertex3f(0.0f, 1f, -0.5f)
                GL11.glTexCoord2f(1f, 0f)
                GL11.glVertex3f(0.0f, 1f, 0.5f)
                GL11.glEnd()
                enableCulling()
            }
            else -> {
                GL11.glTranslatef(0.5f, -0.3f, 0.5f)
                disableCulling()
                GL11.glBegin(GL11.GL_QUADS)
                GL11.glTexCoord2f(1f, 1f)
                GL11.glVertex3f(0.0f, 0.5f, 0.5f)
                GL11.glTexCoord2f(0f, 1f)
                GL11.glVertex3f(0.0f, 0.5f, -0.5f)
                GL11.glTexCoord2f(0f, 0f)
                GL11.glVertex3f(0.0f, 1.5f, -0.5f)
                GL11.glTexCoord2f(1f, 0f)
                GL11.glVertex3f(0.0f, 1.5f, 0.5f)
                GL11.glEnd()
                enableCulling()
            }
        }
        disableBlend()
    }

    @JvmStatic
    fun drawIcon(type: ItemRenderType, icon: ResourceLocation?) {
        bindTexture(icon)
        drawIcon(type)
    }

    fun drawEnergyBare(type: ItemRenderType, e: Float) {
        drawIcon(type, portableBatteryOverlayResource)
        val x = 13f
        val y = 14f - e * 12f
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glColor3f(0f, 0f, 0f)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex3f(x + 1f, 2f, 0.01f)
        GL11.glVertex3f(x, 2f, 0.01f)
        GL11.glVertex3f(x, 14f, 0.01f)
        GL11.glVertex3f(x + 1f, 14f, 0.01f)
        GL11.glEnd()
        GL11.glColor3f(1f, e, 0f)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex3f(x + 1f, y, 0.02f)
        GL11.glVertex3f(x, y, 0.02f)
        GL11.glVertex3f(x, 14f, 0.02f)
        GL11.glVertex3f(x + 1f, 14f, 0.02f)
        GL11.glEnd()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glColor3f(1f, 1f, 1f)
    }

    @JvmStatic
    fun bindTexture(resource: ResourceLocation?) {
        Minecraft.getMinecraft().renderEngine.bindTexture(resource)
    }

    @JvmStatic
    fun ledOnOffColor(on: Boolean) {
        if (!on) GL11.glColor3f(0.7f, 0f, 0f) // Red
        else GL11.glColor3f(0f, 0.7f, 0f) // Green
    }

    @JvmStatic
    fun ledOnOffColorC(on: Boolean): Color {
        return if (!on) Color(0.7f, 0f, 0f) // Red
        else Color(0f, 0.7f, 0f) // Green
    }

    @JvmStatic
    fun drawLight(part: Obj3DPart?) {
        if (part == null) return
        disableLight()
        enableBlend()
        part.draw()
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawLightNoBind(part: Obj3DPart?) {
        if (part == null) return
        disableLight()
        enableBlend()
        part.drawNoBind()
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun drawGuiBackground(ressource: ResourceLocation?, guiScreen: GuiScreen, xSize: Int, ySize: Int) {
        bindTexture(ressource)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        val x = (guiScreen.width - xSize) / 2
        val y = (guiScreen.height - ySize) / 2
        guiScreen.drawTexturedModalRect(x, y, 0, 0, xSize, ySize)
    }

    fun drawLight(part: Obj3DPart?, angle: Float, x: Float, y: Float, z: Float) {
        if (part == null) return
        disableLight()
        enableBlend()
        part.draw(angle, x, y, z)
        enableLight()
        disableBlend()
    }

    @JvmStatic
    fun glDefaultColor() {
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }

    @JvmStatic
    fun drawEntityItem(entityItem: EntityItem?, x: Double, y: Double, z: Double, roty: Float, scale: Float) {
        if (entityItem == null) return
        entityItem.hoverStart = 0.0f
        entityItem.rotationYaw = 0.0f
        entityItem.motionX = 0.0
        entityItem.motionY = 0.0
        entityItem.motionZ = 0.0
        val var10: Render = RenderManager.instance.getEntityRenderObject(entityItem)
        GL11.glPushMatrix()
        GL11.glTranslatef(x.toFloat(), y.toFloat(), z.toFloat())
        GL11.glRotatef(roty, 0f, 1f, 0f)
        GL11.glScalef(scale, scale, scale)
        var10.doRender(entityItem, 0.0, 0.0, 0.0, 0f, 0f)
        GL11.glPopMatrix()
    }

    fun drawConnectionPinSixNode(d: Float, w: Float, h: Float) {
        var d = d
        var w = w
        var h = h
        d += 0.1f
        d *= 0.0625f
        w *= 0.0625f
        h *= 0.0625f
        val w2 = w * 0.5f
        disableTexture()
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex3f(-w2, d, 0f)
        GL11.glVertex3f(w2, d, 0f)
        GL11.glVertex3f(w2, d, h)
        GL11.glVertex3f(-w2, d, h)
        GL11.glEnd()
        enableTexture()
    }

    @JvmStatic
    fun drawConnectionPinSixNode(front: LRDU, dList: FloatArray, w: Float, h: Float) {
        // front.glRotateOnX();
        // drawConnectionPinSixNode(d[front.toInt()], w, h);
        var w = w
        var h = h
        var d = dList[front.toInt()]
        d += 0.04f
        d *= 0.0625f
        w *= 0.0625f
        h *= 0.0625f
        val w2 = w * 0.5f
        disableTexture()
        GL11.glBegin(GL11.GL_QUADS)
        when (front) {
            LRDU.Left -> {
                GL11.glVertex3f(0f, -w2, -d)
                GL11.glVertex3f(0f, w2, -d)
                GL11.glVertex3f(h, w2, -d)
                GL11.glVertex3f(h, -w2, -d)
            }
            LRDU.Right -> {
                GL11.glVertex3f(h, -w2, d)
                GL11.glVertex3f(h, w2, d)
                GL11.glVertex3f(0f, w2, d)
                GL11.glVertex3f(0f, -w2, d)
            }
            LRDU.Down -> {
                GL11.glVertex3f(h, -d, -w2)
                GL11.glVertex3f(h, -d, w2)
                GL11.glVertex3f(0f, -d, w2)
                GL11.glVertex3f(0f, -d, -w2)
            }
            LRDU.Up -> {
                GL11.glVertex3f(0f, d, -w2)
                GL11.glVertex3f(0f, d, w2)
                GL11.glVertex3f(h, d, w2)
                GL11.glVertex3f(h, d, -w2)
            }
        }
        GL11.glEnd()
        enableTexture()
    }

    val itemRender: RenderItem?
        get() {
            if (itemRenderer == null) itemRenderer = RenderItem()
            return itemRenderer
        }

    fun mc(): Minecraft {
        return Minecraft.getMinecraft()
    }

    fun guiScale() {
        GL11.glScalef(16f, 16f, 1f)
    }

    @JvmStatic
    fun drawItemStack(par1ItemStack: ItemStack?, x: Int, y: Int, @Suppress("UNUSED_PARAMETER") par4Str: String?, gui: Boolean) {
        // Block b = Block.getBlockFromItem(par1ItemStack.getItem());
        // b.rend
        // ForgeHooksClient.renderInventoryItem(new RenderBlocks(),Minecraft.getMinecraft().getTextureManager(),par1ItemStack,false,0,x,y);
        // ForgeHooksClient.renderInventoryItem(Minecraft.getMinecraft().bl, engine, item, inColor, zLevel, x, y)
        val itemRenderer = itemRender
        // GL11.glDisable(3042);
        if (gui) {
            GL11.glEnable(32826)
            RenderHelper.enableGUIStandardItemLighting()
        }
        // GL11.glTranslatef(0.0F, 0.0F, 32.0F);
        // ForgeHooksClient.renderInventoryItem(new RenderBlocks(),Minecraft.getMinecraft().getTextureManager(),par1ItemStack,false,0,x,y);
        itemRenderer!!.zLevel = 400.0f
        // ForgeHooksClient.renderInventoryItem(renderBlocks, engine, item, inColor, zLevel, x, y)
        var font: FontRenderer? = null
        if (par1ItemStack != null) {
            val i = par1ItemStack.item ?: return
            font = i.getFontRenderer(par1ItemStack)
        }
        if (font == null) font = mc().fontRenderer
        itemRenderer.renderItemAndEffectIntoGUI(font, mc().textureManager, par1ItemStack, x, y)
        // itemRenderer.renderItemOverlayIntoGUI(font, mc().getTextureManager(), par1ItemStack, x, y, par4Str);
        itemRenderer.zLevel = 0.0f
        if (gui) {
            RenderHelper.disableStandardItemLighting()
            GL11.glDisable(32826)
        }
        if (par1ItemStack!!.stackSize > 1) {
            disableDepthTest()
            // GL11.glPushMatrix();
            // GL
            // GL11.glScalef(0.5f, 0.5f, 0.5f);
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("" + par1ItemStack.stackSize, x + 10, y + 9, -0x1)
            // GL11.glPopMatrix();
            enableDepthTest()
        }
    }

    fun clientDistanceTo(e: Entity?): Double {
        if (e == null) return 100000000.0
        val c: Entity = Minecraft.getMinecraft().thePlayer
        val x = c.posX - e.posX
        val y = c.posY - e.posY
        val z = c.posZ - e.posZ
        return sqrt(x * x + y * y + z * z)
    }

    @JvmStatic
    fun clientDistanceTo(t: TransparentNodeEntity?): Double {
        if (t == null) return 100000000.0
        val c: Entity = Minecraft.getMinecraft().thePlayer
        val x = c.posX - t.xCoord
        val y = c.posY - t.yCoord
        val z = c.posZ - t.zCoord
        return sqrt(x * x + y * y + z * z)
    }

    fun getLight(w: World, x: Int, y: Int, z: Int): Int {
        val b = w.getSkyBlockTypeBrightness(EnumSkyBlock.Block, x, y, z)
        val s = w.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, x, y, z) - w.calculateSkylightSubtracted(0f)
        return b.coerceAtLeast(s)
    }

    @JvmStatic
    fun disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST)
    }

    @JvmStatic
    fun enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    @JvmStatic
    fun sendPacketToServer(bos: ByteArrayOutputStream) {
        val packet = C17PacketCustomPayload(Eln.channelName, bos.toByteArray())
        Eln.eventChannel.sendToServer(FMLProxyPacket(packet))
        // Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new FMLProxyPacket(packet));
    }

    val glListsAllocated = HashSet<Int>()
    @JvmStatic
    fun glGenListsSafe(): Int {
        val id = GL11.glGenLists(1)
        glListsAllocated.add(id)
        return id
    }

    @JvmStatic
    fun glDeleteListsSafe(id: Int) {
        glListsAllocated.remove(id)
        GL11.glDeleteLists(id, 1)
    }

    @JvmStatic
    fun glDeleteListsAllSafe() {
        try {
            for (id in glListsAllocated) {
                GL11.glDeleteLists(id, 1)
            }
            glListsAllocated.clear()
        } catch (e: Exception) {
            //nic
        }
    }

    @JvmStatic
    fun showItemTooltip(details: List<String>, realismDetails: List<String>, realisticEnum: RealisticEnum?, dst: MutableList<String>) {
        if (realisticEnum != null)
            dst.add("§r${realisticEnum.color}${realisticEnum.name}§r")
        if (details.isNotEmpty()) {
            if (isShiftHeld()) {
                dst.addAll(details)
            } else {
                dst.add("§F§o${tr("Hold [shift] for details")}")
            }
        }
        if (realismDetails.isNotEmpty()) {
            if (isControlHeld()) {
                dst.addAll(realismDetails)
            } else {
                if (realisticEnum != null) {
                    if (realismDetails.isNotEmpty()) {
                        dst.add("§F§o${tr("Hold [ctrl] for realism details")}")
                    }
                }
            }
        }
        dst.listLengthFormatter(24)
    }

    private fun List<String>.listLengthFormatter(length: Int) {
        val chars: Sequence<Char> = "abcd".asSequence()
        val str: String = chars.joinToString()
    }

    private fun isShiftHeld(): Boolean {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
    }

    private fun isControlHeld(): Boolean {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
    }

    @JvmStatic
    fun getWeather(world: World): Double {
        if (world.isThundering) return 1.0
        return if (world.isRaining) 0.5 else 0.0
    }
}

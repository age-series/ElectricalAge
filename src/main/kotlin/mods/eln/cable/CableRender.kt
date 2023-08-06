package mods.eln.cable

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUMask
import mods.eln.node.NodeBase.Companion.isBlockWrappable
import mods.eln.node.NodeBlockEntity
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object CableRender {
    @JvmStatic
    fun connectionType(entity: NodeBlockEntity, connectedSide: LRDUMask, side: Direction): CableRenderType {
        var x2: Int
        var y2: Int
        var z2: Int
        val connectionTypeBuild = CableRenderType()
        var otherTileEntity: TileEntity?
        for (lrdu in LRDU.values()) {
            //noConnection
            if (!connectedSide[lrdu]) continue
            val sideLrdu = side.applyLRDU(lrdu)
            x2 = entity.xCoord
            y2 = entity.yCoord
            z2 = entity.zCoord
            when (sideLrdu) {
                Direction.XN -> x2--
                Direction.XP -> x2++
                Direction.YN -> y2--
                Direction.YP -> y2++
                Direction.ZN -> z2--
                Direction.ZP -> z2++
            }

            //standardConnection
            otherTileEntity = entity.worldObj.getTileEntity(x2, y2, z2)
            if (otherTileEntity is SixNodeEntity) {
                val sixNodeEntity = otherTileEntity
                if (sixNodeEntity.elementRenderList[side.int] != null) {
                    val otherSide = side.applyLRDU(lrdu)
                    connectionTypeBuild.otherdry[lrdu.dir] =
                        sixNodeEntity.getCableDry(otherSide, otherSide.getLRDUGoingTo(side))
                    connectionTypeBuild.otherRender[lrdu.dir] =
                        sixNodeEntity.getCableRender(otherSide, otherSide.getLRDUGoingTo(side)!!)
                    continue
                }
            }

            //no wrappeConection ?
            if (!isBlockWrappable(entity.worldObj.getBlock(x2, y2, z2), entity.worldObj, x2, y2, z2)) {
                continue
            } else {
                when (side) {
                    Direction.XN -> x2--
                    Direction.XP -> x2++
                    Direction.YN -> y2--
                    Direction.YP -> y2++
                    Direction.ZN -> z2--
                    Direction.ZP -> z2++
                }
                otherTileEntity = entity.worldObj.getTileEntity(x2, y2, z2)
                if (otherTileEntity is NodeBlockEntity) {
                    val otherDirection = side.inverse
                    val otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu)!!.inverse()
                    val render = entity.getCableRender(sideLrdu, sideLrdu.getLRDUGoingTo(side)!!)
                    val otherNode = otherTileEntity
                    val otherRender = otherNode.getCableRender(otherDirection, otherLRDU)
                    if (render == null) {
                        //Utils.println("ASSERT cableRender missing");
                        continue
                    }
                    if (otherRender == null) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend
                        connectionTypeBuild.endAt[lrdu.dir] = render.heightPixel
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                        continue
                    }
                    if (render.width == otherRender.width) {
                        if (sideLrdu.int > otherDirection.int) {
                            connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend
                            connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel
                        }
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                        continue
                    }
                    if (render.width < otherRender.width) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend
                        connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                        continue
                    }
                }
            }
        }
        return connectionTypeBuild
    }

    @JvmStatic
    fun connectionType(element: SixNodeElementRender, side: Direction): CableRenderType {
        var x2: Int
        var y2: Int
        var z2: Int
        val connectionTypeBuild = CableRenderType()
        var otherTileEntity: TileEntity?
        for (lrdu in LRDU.values()) {
            //noConnection
            if (!element.connectedSide[lrdu]) continue
            val sideLrdu = side.applyLRDU(lrdu)

            //InternalConnection
            if (element.tileEntity.elementRenderList[sideLrdu.int] != null) {
                val otherLRDU = sideLrdu.getLRDUGoingTo(side)
                val render = element.getCableRender(lrdu)
                val otherElement = element.tileEntity.elementRenderList[sideLrdu.int]!!
                val otherRender = otherElement.getCableRender(otherLRDU!!)
                if (otherRender == null || render == null) {
                    continue
                }
                if (render.width == otherRender.width) {
                    if (side.int > sideLrdu.int) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Internal
                        connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel
                    }
                    connectionTypeBuild.otherdry[lrdu.dir] = otherElement.getCableDry(otherLRDU)
                    connectionTypeBuild.otherRender[lrdu.dir] = otherElement.getCableRender(otherLRDU)
                    continue
                }
                if (render.width < otherRender.width) {
                    connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Internal
                    connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel
                    connectionTypeBuild.otherdry[lrdu.dir] = otherElement.getCableDry(otherLRDU)
                    connectionTypeBuild.otherRender[lrdu.dir] = otherElement.getCableRender(otherLRDU)
                    continue
                }
                connectionTypeBuild.otherdry[lrdu.dir] = otherElement.getCableDry(otherLRDU)
                connectionTypeBuild.otherRender[lrdu.dir] = otherElement.getCableRender(otherLRDU)
                continue
            }
            x2 = element.tileEntity.xCoord
            y2 = element.tileEntity.yCoord
            z2 = element.tileEntity.zCoord
            when (sideLrdu) {
                Direction.XN -> x2--
                Direction.XP -> x2++
                Direction.YN -> y2--
                Direction.YP -> y2++
                Direction.ZN -> z2--
                Direction.ZP -> z2++
            }

            //standardConnection
            otherTileEntity = element.tileEntity.worldObj.getTileEntity(x2, y2, z2)
            if (otherTileEntity is SixNodeEntity) {
                val sixNodeEntity = otherTileEntity
                if (sixNodeEntity.elementRenderList[side.int] != null) {
                    connectionTypeBuild.otherdry[lrdu.dir] =
                        sixNodeEntity.elementRenderList[side.int]!!.getCableDry(lrdu.inverse())
                    connectionTypeBuild.otherRender[lrdu.dir] =
                        sixNodeEntity.elementRenderList[side.int]!!.getCableRender(lrdu.inverse())
                    continue
                }
            }

            //no wrappeConection ?
            if (!isBlockWrappable(
                    element.tileEntity.worldObj.getBlock(x2, y2, z2),
                    element.tileEntity.worldObj,
                    x2,
                    y2,
                    z2
                )
            ) {
                continue
            } else {
                when (side) {
                    Direction.XN -> x2--
                    Direction.XP -> x2++
                    Direction.YN -> y2--
                    Direction.YP -> y2++
                    Direction.ZN -> z2--
                    Direction.ZP -> z2++
                }
                otherTileEntity = element.tileEntity.worldObj.getTileEntity(x2, y2, z2)
                if (otherTileEntity is NodeBlockEntity) {
                    val otherDirection = side.inverse
                    val otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu)!!.inverse()
                    val render = element.getCableRender(lrdu) ?: continue
                    val otherNode = otherTileEntity
                    val otherRender = otherNode.getCableRender(otherDirection, otherLRDU)
                    if (otherRender == null) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend
                        connectionTypeBuild.endAt[lrdu.dir] = render.heightPixel
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                        continue
                    }
                    if (render.width == otherRender.width) {
                        if (sideLrdu.int > otherDirection.int) {
                            connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend
                            connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel
                        }
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                        continue
                    }
                    if (render.width < otherRender.width) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend
                        connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                        continue
                    }
                    connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU)
                    connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU)
                    continue
                }
            }
        }
        return connectionTypeBuild
    }

    @JvmStatic
    fun drawCable(
        cable: CableRenderDescriptor?,
        connection: LRDUMask,
        connectionType: CableRenderType,
        deltaStart: Float = cable!!.widthDiv2 / 2f,
        drawBottom: Boolean = false
    ) {
        if (cable == null) return
        var endLeft = -deltaStart
        var endRight = deltaStart
        var endUp = deltaStart
        var endDown = -deltaStart
        val startLeft = -connectionType.startAt[0]
        val startRight = connectionType.startAt[1]
        val startUp = connectionType.startAt[2]
        val startDown = -connectionType.startAt[3]
        if ((connection.mask == 0) and (deltaStart >= 0f)) {
            endLeft = -cable.widthDiv2 - 3.0f / 16.0f
            endRight = cable.widthDiv2 + 3.0f / 16.0f
            endDown = -cable.widthDiv2 - 3.0f / 16.0f
            endUp = cable.widthDiv2 + 3.0f / 16.0f
        } else {
            if (connection[LRDU.Left]) {
                endLeft = -0.5f
            }
            if (connection[LRDU.Right]) {
                endRight = 0.5f
            }
            if (connection[LRDU.Down]) {
                endDown = -0.5f
            }
            if (connection[LRDU.Up]) {
                endUp = 0.5f
            }
        }
        when (connectionType.method[0]) {
            CableRenderTypeMethodType.Internal -> endLeft += (connectionType.endAt[0] / 16.0).toFloat()
            CableRenderTypeMethodType.Etend -> endLeft -= (connectionType.endAt[0] / 16.0).toFloat()
            else -> {}
        }
        when (connectionType.method[1]) {
            CableRenderTypeMethodType.Internal -> endRight -= (connectionType.endAt[1] / 16.0).toFloat()
            CableRenderTypeMethodType.Etend -> endRight += (connectionType.endAt[1] / 16.0).toFloat()
            else -> {}
        }
        when (connectionType.method[2]) {
            CableRenderTypeMethodType.Internal -> endDown += (connectionType.endAt[2] / 16.0).toFloat()
            CableRenderTypeMethodType.Etend -> endDown -= (connectionType.endAt[2] / 16.0).toFloat()
            else -> {}
        }
        when (connectionType.method[3]) {
            CableRenderTypeMethodType.Internal -> endUp -= (connectionType.endAt[3] / 16.0).toFloat()
            CableRenderTypeMethodType.Etend -> endUp += (connectionType.endAt[3] / 16.0).toFloat()
            else -> {}
        }
        val height = cable.height
        val tx = 0.25f
        val ty = 0.5f
        if (endLeft < startLeft) {
            // Draws top, bottom, and two sides of the cable
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            GL11.glNormal3f(0f, 1f, 0f)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endLeft)
            GL11.glVertex3f(0f, cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startLeft)
            GL11.glVertex3f(0f, cable.widthDiv2, startLeft)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endLeft)
            GL11.glVertex3f(height, cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + startLeft)
            GL11.glVertex3f(height, cable.widthDiv2, startLeft)
            GL11.glNormal3f(1f, 0f, 0f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endLeft)
            GL11.glVertex3f(height, -cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + startLeft)
            GL11.glVertex3f(height, -cable.widthDiv2, startLeft)
            GL11.glNormal3f(0f, -1f, 0f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f - height, ty + endLeft)
            GL11.glVertex3f(0f, -cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f - height, ty + startLeft)
            GL11.glVertex3f(0f, -cable.widthDiv2, startLeft)
            if (drawBottom) {
                GL11.glNormal3f(0f, 1f, 0f)
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endLeft)
                GL11.glVertex3f(0f, cable.widthDiv2, endLeft)
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startLeft)
                GL11.glVertex3f(0f, cable.widthDiv2, startLeft)
            }
            GL11.glEnd()

            // Draws end cap
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glNormal3f(0f, 0f, -1f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endLeft - height)
            GL11.glVertex3f(0f, -cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endLeft - height)
            GL11.glVertex3f(0f, cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endLeft)
            GL11.glVertex3f(height, cable.widthDiv2, endLeft)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endLeft)
            GL11.glVertex3f(height, -cable.widthDiv2, endLeft)
            GL11.glEnd()
        }
        if (endRight > startRight) {
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            GL11.glNormal3f(0f, 1f, 0f)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startRight)
            GL11.glVertex3f(0f, cable.widthDiv2, startRight)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endRight)
            GL11.glVertex3f(0f, cable.widthDiv2, endRight)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + startRight)
            GL11.glVertex3f(height, cable.widthDiv2, startRight)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endRight)
            GL11.glVertex3f(height, cable.widthDiv2, endRight)
            GL11.glNormal3f(1f, 0f, 0f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + startRight)
            GL11.glVertex3f(height, -cable.widthDiv2, startRight)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endRight)
            GL11.glVertex3f(height, -cable.widthDiv2, endRight)
            GL11.glNormal3f(0f, -1f, 0f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f - height, ty + startRight)
            GL11.glVertex3f(0f, -cable.widthDiv2, startRight)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f - height, ty + endRight)
            GL11.glVertex3f(0f, -cable.widthDiv2, endRight)
            if (drawBottom) {
                GL11.glNormal3f(0f, 1f, 0f)
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startRight)
                GL11.glVertex3f(0f, cable.widthDiv2, startRight)
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endRight)
                GL11.glVertex3f(0f, cable.widthDiv2, endRight)
            }
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glNormal3f(0f, 0f, 1f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endRight)
            GL11.glVertex3f(height, -cable.widthDiv2, endRight)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endRight)
            GL11.glVertex3f(height, cable.widthDiv2, endRight)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endRight + height)
            GL11.glVertex3f(0f, cable.widthDiv2, endRight)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endRight + height)
            GL11.glVertex3f(0f, -cable.widthDiv2, endRight)
            GL11.glEnd()
        }
        if (endDown < startDown) {
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            GL11.glNormal3f(0f, 0f, -1f)
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endDown)
            GL11.glVertex3f(0f, endDown, -cable.widthDiv2)
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startDown)
            GL11.glVertex3f(0f, startDown, -cable.widthDiv2)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endDown)
            GL11.glVertex3f(height, endDown, -cable.widthDiv2)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + startDown)
            GL11.glVertex3f(height, startDown, -cable.widthDiv2)
            GL11.glNormal3f(1f, 0f, 0f)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endDown)
            GL11.glVertex3f(height, endDown, cable.widthDiv2)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + startDown)
            GL11.glVertex3f(height, startDown, cable.widthDiv2)
            GL11.glNormal3f(0f, 0f, 1f)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endDown)
            GL11.glVertex3f(0f, endDown, cable.widthDiv2)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startDown)
            GL11.glVertex3f(0f, startDown, cable.widthDiv2)
            if (drawBottom) {
                GL11.glNormal3f(0f, 0f, -1f)
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endDown)
                GL11.glVertex3f(0f, endDown, -cable.widthDiv2)
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startDown)
                GL11.glVertex3f(0f, startDown, -cable.widthDiv2)
            }
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glNormal3f(0f, -1f, 0f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endDown)
            GL11.glVertex3f(height, endDown, -cable.widthDiv2)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endDown)
            GL11.glVertex3f(height, endDown, cable.widthDiv2)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endDown - height)
            GL11.glVertex3f(0f, endDown, cable.widthDiv2)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endDown - height)
            GL11.glVertex3f(0f, endDown, -cable.widthDiv2)
            GL11.glEnd()
        }
        if (endUp > startUp) {
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            GL11.glNormal3f(0f, 0f, -1f)
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startUp)
            GL11.glVertex3f(0f, startUp, -cable.widthDiv2)
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endUp)
            GL11.glVertex3f(0f, endUp, -cable.widthDiv2)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + startUp)
            GL11.glVertex3f(height, startUp, -cable.widthDiv2)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endUp)
            GL11.glVertex3f(height, endUp, -cable.widthDiv2)
            GL11.glNormal3f(1f, 0f, 0f)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + startUp)
            GL11.glVertex3f(height, startUp, cable.widthDiv2)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endUp)
            GL11.glVertex3f(height, endUp, cable.widthDiv2)
            GL11.glNormal3f(0f, 0f, 1f)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startUp)
            GL11.glVertex3f(0f, startUp, cable.widthDiv2)
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endUp)
            GL11.glVertex3f(0f, endUp, cable.widthDiv2)
            if (drawBottom) {
                GL11.glNormal3f(0f, 0f, -1f)
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startUp)
                GL11.glVertex3f(0f, startUp, -cable.widthDiv2)
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endUp)
                GL11.glVertex3f(0f, endUp, -cable.widthDiv2)
            }
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glNormal3f(0f, 1f, 0f)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endUp + height)
            GL11.glVertex3f(0f, endUp, -cable.widthDiv2)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endUp + height)
            GL11.glVertex3f(0f, endUp, cable.widthDiv2)
            GL11.glTexCoord2f(tx + cable.widthDiv2 * 0.5f, ty + endUp)
            GL11.glVertex3f(height, endUp, cable.widthDiv2)
            GL11.glTexCoord2f(tx - cable.widthDiv2 * 0.5f, ty + endUp)
            GL11.glVertex3f(height, endUp, -cable.widthDiv2)
            GL11.glEnd()
        }
    }

    @JvmStatic
    fun drawNode(cable: CableRenderDescriptor, connection: LRDUMask, connectionType: CableRenderType?) {
        if (connection.mask == 0 || (connection[LRDU.Left] || connection[LRDU.Right]) && (connection[LRDU.Down] || connection[LRDU.Up]) || connection.mask == 1 || connection.mask == 2 || connection.mask == 4 || connection.mask == 8) {
            val widthDiv2 = cable.widthDiv2 + 1.0f / 16.0f
            val height = cable.height + 1.0f / 16.0f
            val tx = 0.75f
            val ty = 0.5f
            GL11.glColor4d(1.0, 1.0, 1.0, 1.0)

            // Renders the top and two sides of the node cap
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            GL11.glNormal3f(0f, 1f, 0f)
            GL11.glTexCoord2f(tx + (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty - widthDiv2)
            GL11.glVertex3f(0f, widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx + (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty + widthDiv2)
            GL11.glVertex3f(0f, widthDiv2, widthDiv2)
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2)
            GL11.glVertex3f(height, widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2)
            GL11.glVertex3f(height, widthDiv2, widthDiv2)
            GL11.glNormal3f(1f, 0f, 0f)
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2)
            GL11.glVertex3f(height, -widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2)
            GL11.glVertex3f(height, -widthDiv2, widthDiv2)
            GL11.glNormal3f(0f, -1f, 0f)
            GL11.glTexCoord2f(tx - (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty - widthDiv2)
            GL11.glVertex3f(0f, -widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx - (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty + widthDiv2)
            GL11.glVertex3f(0f, -widthDiv2, widthDiv2)
            GL11.glEnd()

            // Renders remaining 2 sides of the node cap
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glNormal3f(0f, 0f, -1f)
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2 - cable.height - 1.0f / 16.0f)
            GL11.glVertex3f(0f, -widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2 - cable.height - 1.0f / 16.0f)
            GL11.glVertex3f(0f, widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2)
            GL11.glVertex3f(height, widthDiv2, -widthDiv2)
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2)
            GL11.glVertex3f(height, -widthDiv2, -widthDiv2)
            GL11.glNormal3f(0f, 0f, 1f)
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2)
            GL11.glVertex3f(height, -widthDiv2, widthDiv2)
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2)
            GL11.glVertex3f(height, widthDiv2, widthDiv2)
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2 + cable.height + 1.0f / 16.0f)
            GL11.glVertex3f(0f, widthDiv2, widthDiv2)
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2 + cable.height + 1.0f / 16.0f)
            GL11.glVertex3f(0f, -widthDiv2, widthDiv2)
            GL11.glEnd()
        }
    }
}

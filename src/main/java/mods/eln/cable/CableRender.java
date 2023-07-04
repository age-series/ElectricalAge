package mods.eln.cable;

import mods.eln.cable.CableRenderType.CableRenderTypeMethodType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class CableRender {
    private CableRender() {
    }
    /*
	static final int connectionStandard = 0;
	static final int connectionInternal = 1;
	static final int connectionWrappeHalf = 2;
	static final int connectionWrappeFull = 3;
	static final int connectionExtend = 5;
	*/

    public static CableRenderType connectionType(NodeBlockEntity entity, LRDUMask connectedSide, Direction side) {
        Block block;
        int x2, y2, z2;
        CableRenderType connectionTypeBuild = new CableRenderType();
        TileEntity otherTileEntity;

        for (LRDU lrdu : LRDU.values()) {
            //noConnection
            if (!connectedSide.get(lrdu)) continue;

            Direction sideLrdu = side.applyLRDU(lrdu);

            x2 = entity.xCoord;
            y2 = entity.yCoord;
            z2 = entity.zCoord;

            switch (sideLrdu) {
                case XN:
                    x2--;
                    break;
                case XP:
                    x2++;
                    break;
                case YN:
                    y2--;
                    break;
                case YP:
                    y2++;
                    break;
                case ZN:
                    z2--;
                    break;
                case ZP:
                    z2++;
                    break;
                default:
                    break;
            }

            //standardConnection
            otherTileEntity = entity.getWorldObj().getTileEntity(x2, y2, z2);
            if (otherTileEntity instanceof SixNodeEntity) {
                SixNodeEntity sixNodeEntity = (SixNodeEntity) otherTileEntity;
                if (sixNodeEntity.elementRenderList[side.getInt()] != null) {
                    Direction otherSide = side.applyLRDU(lrdu);
                    connectionTypeBuild.otherdry[lrdu.getDir()] = sixNodeEntity.getCableDry(otherSide, otherSide.getLRDUGoingTo(side));
                    connectionTypeBuild.otherRender[lrdu.getDir()] = sixNodeEntity.getCableRender(otherSide, otherSide.getLRDUGoingTo(side));
                    continue;
                }
            }

            //no wrappeConection ?
            if (!NodeBase.isBlockWrappable(entity.getWorldObj().getBlock(x2, y2, z2), entity.getWorldObj(), x2, y2, z2)) {
                continue;
            } else {
                switch (side) {
                    case XN:
                        x2--;
                        break;
                    case XP:
                        x2++;
                        break;
                    case YN:
                        y2--;
                        break;
                    case YP:
                        y2++;
                        break;
                    case ZN:
                        z2--;
                        break;
                    case ZP:
                        z2++;
                        break;
                    default:
                        break;
                }

                otherTileEntity = entity.getWorldObj().getTileEntity(x2, y2, z2);

                if (otherTileEntity instanceof NodeBlockEntity) {
				/*
					Direction otherDirection = side.getInverse();
					LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
					CableRenderDescriptor render = entity.getCableRender(sideLrdu,sideLrdu.getLRDUGoingTo(side));
					//CableRenderDescriptor render = entity.getCableRender(side,lrdu);
					NodeBlockEntity otherNode =  ((NodeBlockEntity)otherTileEntity);
					CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);*/
				/*	Direction otherDirection = side.getInverse();
					LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
					CableRenderDescriptor render = entity.getCableRender(sideLrdu,sideLrdu.getLRDUGoingTo(side));
					NodeBlockEntity otherNode =  ((NodeBlockEntity)otherTileEntity);
					CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);
					*/
                    Direction otherDirection = side.getInverse();
                    LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
                    CableRenderDescriptor render = entity.getCableRender(sideLrdu, sideLrdu.getLRDUGoingTo(side));
                    NodeBlockEntity otherNode = (NodeBlockEntity) otherTileEntity;
                    CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);

                    if (render == null) {
                        //Utils.println("ASSERT cableRender missing");
                        continue;
                    }

                    if (otherRender == null) {
                        connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.getDir()] = render.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //	=  += (connectionExtend + (render.heightPixel<<4))<<(lrdu.dir*8) ;
                        continue;
                    }
                    //	if(element.tileEntity.hashCode() > otherTileEntity.hashCode())
                    if (render.width == otherRender.width) {
                        if (sideLrdu.getInt() > otherDirection.getInt()) {
                            connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Etend;
                            connectionTypeBuild.endAt[lrdu.getDir()] = otherRender.heightPixel;
                            //connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        }
                        connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                        continue;
                    }
                    if (render.width < otherRender.width) {
                        connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.getDir()] = otherRender.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        continue;
                    }
                }
            }
        }
        return connectionTypeBuild;
    }

    public static CableRenderType connectionType(SixNodeElementRender element, Direction side) {
        Block block;
        int x2, y2, z2;
        CableRenderType connectionTypeBuild = new CableRenderType();
        TileEntity otherTileEntity;

        for (LRDU lrdu : LRDU.values()) {
            //noConnection
            if (!element.connectedSide.get(lrdu)) continue;

            Direction sideLrdu = side.applyLRDU(lrdu);

            //InternalConnection
            if (element.getTileEntity().elementRenderList[sideLrdu.getInt()] != null) {
                LRDU otherLRDU = sideLrdu.getLRDUGoingTo(side);
                CableRenderDescriptor render = element.getCableRender(lrdu);
                SixNodeElementRender otherElement = element.getTileEntity().elementRenderList[sideLrdu.getInt()];
                CableRenderDescriptor otherRender = otherElement.getCableRender(otherLRDU);

                if (otherRender == null || render == null) {
                    continue;
                }

                if (render.width == otherRender.width) {
                    if (side.getInt() > sideLrdu.getInt()) {
                        connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Internal;
                        connectionTypeBuild.endAt[lrdu.getDir()] = otherRender.heightPixel;
                        //connectionTypeBuild += (connectionInternal + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                    }
                    connectionTypeBuild.otherdry[lrdu.getDir()] = otherElement.getCableDry(otherLRDU);
                    connectionTypeBuild.otherRender[lrdu.getDir()] = otherElement.getCableRender(otherLRDU);
                    continue;
                }

                if (render.width < otherRender.width) {
                    connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Internal;
                    connectionTypeBuild.endAt[lrdu.getDir()] = otherRender.heightPixel;
                    connectionTypeBuild.otherdry[lrdu.getDir()] = otherElement.getCableDry(otherLRDU);
                    connectionTypeBuild.otherRender[lrdu.getDir()] = otherElement.getCableRender(otherLRDU);
                    //connectionTypeBuild += (connectionInternal + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                    continue;
                }
                connectionTypeBuild.otherdry[lrdu.getDir()] = otherElement.getCableDry(otherLRDU);
                connectionTypeBuild.otherRender[lrdu.getDir()] = otherElement.getCableRender(otherLRDU);
                continue;
            }

            x2 = element.getTileEntity().xCoord;
            y2 = element.getTileEntity().yCoord;
            z2 = element.getTileEntity().zCoord;

            switch (sideLrdu) {
                case XN:
                    x2--;
                    break;
                case XP:
                    x2++;
                    break;
                case YN:
                    y2--;
                    break;
                case YP:
                    y2++;
                    break;
                case ZN:
                    z2--;
                    break;
                case ZP:
                    z2++;
                    break;
                default:
                    break;
            }

            //standardConnection
            otherTileEntity = element.getTileEntity().getWorldObj().getTileEntity(x2, y2, z2);
            if (otherTileEntity instanceof SixNodeEntity) {
                SixNodeEntity sixNodeEntity = (SixNodeEntity) otherTileEntity;
                if (sixNodeEntity.elementRenderList[side.getInt()] != null) {
                    connectionTypeBuild.otherdry[lrdu.getDir()] = sixNodeEntity.elementRenderList[side.getInt()].getCableDry(lrdu.inverse());
                    connectionTypeBuild.otherRender[lrdu.getDir()] = sixNodeEntity.elementRenderList[side.getInt()].getCableRender(lrdu.inverse());
                    continue;
                }
            }

            //no wrappeConection ?
            if (!NodeBase.isBlockWrappable(element.getTileEntity().getWorldObj().getBlock(x2, y2, z2), element.getTileEntity().getWorldObj(), x2, y2, z2)) {
                continue;
            } else {
                switch (side) {
                    case XN:
                        x2--;
                        break;
                    case XP:
                        x2++;
                        break;
                    case YN:
                        y2--;
                        break;
                    case YP:
                        y2++;
                        break;
                    case ZN:
                        z2--;
                        break;
                    case ZP:
                        z2++;
                        break;
                    default:
                        break;
                }

                otherTileEntity = element.getTileEntity().getWorldObj().getTileEntity(x2, y2, z2);

                if (otherTileEntity instanceof NodeBlockEntity) {
                    //Direction otherDirection = side.getInverse();
					/*Direction otherDirection = side.applyLRDU(lrdu).getInverse();
					LRDU otherLRDU = otherDirection.getLRDUGoingTo(side.getInverse());
					CableRenderDescriptor render = element.getCableRender(lrdu);
					NodeBlockEntity otherNode = ((NodeBlockEntity)otherTileEntity);
					CableRenderDescriptor otherRender = otherNode.getCableRender(side.getInverse(), lrdu//.inverse());
				*/
                    Direction otherDirection = side.getInverse();
                    LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
                    CableRenderDescriptor render = element.getCableRender(lrdu);

                    if (render == null)
                        continue;
                    NodeBlockEntity otherNode = (NodeBlockEntity) otherTileEntity;
                    CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);

                    if (otherRender == null) {
                        connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.getDir()] = render.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //connectionTypeBuild += (connectionExtend + (render.heightPixel<<4))<<(lrdu.dir*8) ;
                        continue;
                    }
                    //	if(element.tileEntity.hashCode() > otherTileEntity.hashCode())
                    if (render.width == otherRender.width) {
                        if (sideLrdu.getInt() > otherDirection.getInt()) {
                            connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Etend;
                            connectionTypeBuild.endAt[lrdu.getDir()] = otherRender.heightPixel;
                            ///connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        }
                        connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                        continue;
                    }
                    if (render.width < otherRender.width) {
                        connectionTypeBuild.method[lrdu.getDir()] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.getDir()] = otherRender.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        continue;
                    }
                    connectionTypeBuild.otherdry[lrdu.getDir()] = otherNode.getCableDry(otherDirection, otherLRDU);
                    connectionTypeBuild.otherRender[lrdu.getDir()] = otherNode.getCableRender(otherDirection, otherLRDU);
                    continue;
                }

				/*
				if(otherTileEntity instanceof SixNodeEntity) {
				//	SixNodeEntity sixNodeEntity = (SixNodeEntity) otherTileEntity;
					//sixNodeEntity.elementRenderList[0].
					connectionTypeBuild += connectionWrappeHalf<<(lrdu.dir*8);
					continue;
				}
				else {
					connectionTypeBuild += connectionWrappeFull<<(lrdu.dir*8);
					continue;
				}*/
            }
        }
        return connectionTypeBuild;
    }

    public static void drawCable(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType) {
        drawCable(cable, connection, connectionType, cable.widthDiv2 / 2f);
    }

    public static void drawCable(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType, float deltaStart) {
        drawCable(cable, connection, connectionType, deltaStart, false);
    }

    public static void drawCable(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType, float deltaStart, boolean drawBottom) {
        if (cable == null) return;

        float tx, ty;
        float endLeft = -deltaStart, endRight = deltaStart, endUp = deltaStart, endDown = -deltaStart;
        float startLeft = -connectionType.startAt[0], startRight = connectionType.startAt[1], startUp = connectionType.startAt[2], startDown = -connectionType.startAt[3];

        if (connection.mask == 0 & deltaStart >= 0f) {
            endLeft = -cable.widthDiv2 - 3.0f / 16.0f;
            endRight = cable.widthDiv2 + 3.0f / 16.0f;
            endDown = -cable.widthDiv2 - 3.0f / 16.0f;
            endUp = cable.widthDiv2 + 3.0f / 16.0f;
        } else {
            if (connection.get(LRDU.Left)) {
                endLeft = -0.5f;
            }
            if (connection.get(LRDU.Right)) {
                endRight = 0.5f;
            }
            if (connection.get(LRDU.Down)) {
                endDown = -0.5f;
            }
            if (connection.get(LRDU.Up)) {
                endUp = 0.5f;
            }
        }

        switch (connectionType.method[0]) {
            case Internal:
                endLeft += (connectionType.endAt[0]) / 16.0;
                break;
            case Etend:
                endLeft -= (connectionType.endAt[0]) / 16.0;
                break;
            default:
                break;
        }

        switch (connectionType.method[1]) {
            case Internal:
                endRight -= (connectionType.endAt[1]) / 16.0;
                break;
            case Etend:
                endRight += (connectionType.endAt[1]) / 16.0;
                break;
            default:
                break;
        }

        switch (connectionType.method[2]) {
            case Internal:
                endDown += (connectionType.endAt[2]) / 16.0;
                break;
            case Etend:
                endDown -= (connectionType.endAt[2]) / 16.0;
                break;
            default:
                break;
        }

        switch (connectionType.method[3]) {
            case Internal:
                endUp -= (connectionType.endAt[3]) / 16.0;
                break;
            case Etend:
                endUp += (connectionType.endAt[3]) / 16.0;
                break;
            default:
                break;
        }

        float height = cable.height;
        tx = 0.25f;
        ty = 0.5f;

        if (endLeft < startLeft) {
            // Draws top, bottom, and two sides of the cable
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glNormal3f(0f, 1f, 0f);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endLeft);
            GL11.glVertex3f(0, cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startLeft);
            GL11.glVertex3f(0, cable.widthDiv2, startLeft);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endLeft);
            GL11.glVertex3f(height, cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startLeft);
            GL11.glVertex3f(height, cable.widthDiv2, startLeft);
            GL11.glNormal3f(1f, 0f, 0f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endLeft);
            GL11.glVertex3f(height, -cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startLeft);
            GL11.glVertex3f(height, -cable.widthDiv2, startLeft);
            GL11.glNormal3f(0f, -1f, 0f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + endLeft);
            GL11.glVertex3f(0, -cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + startLeft);
            GL11.glVertex3f(0, -cable.widthDiv2, startLeft);
            if (drawBottom) {
                GL11.glNormal3f(0f, 1f, 0f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endLeft);
                GL11.glVertex3f(0, cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startLeft);
                GL11.glVertex3f(0, cable.widthDiv2, startLeft);
            }
            GL11.glEnd();

            // Draws end cap
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0f, 0f, -1f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endLeft - height);
            GL11.glVertex3f(0, -cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endLeft - height);
            GL11.glVertex3f(0, cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endLeft);
            GL11.glVertex3f(height, cable.widthDiv2, endLeft);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endLeft);
            GL11.glVertex3f(height, -cable.widthDiv2, endLeft);
            GL11.glEnd();
        }

        if (endRight > startRight) {
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glNormal3f(0f, 1f, 0f);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startRight);
            GL11.glVertex3f(0, cable.widthDiv2, startRight);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endRight);
            GL11.glVertex3f(0, cable.widthDiv2, endRight);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startRight);
            GL11.glVertex3f(height, cable.widthDiv2, startRight);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endRight);
            GL11.glVertex3f(height, cable.widthDiv2, endRight);
            GL11.glNormal3f(1f, 0f, 0f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startRight);
            GL11.glVertex3f(height, -cable.widthDiv2, startRight);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endRight);
            GL11.glVertex3f(height, -cable.widthDiv2, endRight);
            GL11.glNormal3f(0f, -1f, 0f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + startRight);
            GL11.glVertex3f(0, -cable.widthDiv2, startRight);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + endRight);
            GL11.glVertex3f(0, -cable.widthDiv2, endRight);
            if (drawBottom) {
                GL11.glNormal3f(0f, 1f, 0f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startRight);
                GL11.glVertex3f(0, cable.widthDiv2, startRight);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endRight);
                GL11.glVertex3f(0, cable.widthDiv2, endRight);
            }
            GL11.glEnd();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0f, 0f, 1f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endRight);
            GL11.glVertex3f(height, -cable.widthDiv2, endRight);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endRight);
            GL11.glVertex3f(height, cable.widthDiv2, endRight);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endRight + height);
            GL11.glVertex3f(0, cable.widthDiv2, endRight);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endRight + height);
            GL11.glVertex3f(0, -cable.widthDiv2, endRight);
            GL11.glEnd();
        }
        if (endDown < startDown) {
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glNormal3f(0f, 0f, -1f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endDown);
            GL11.glVertex3f(0, endDown, -cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startDown);
            GL11.glVertex3f(0, startDown, -cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endDown);
            GL11.glVertex3f(height, endDown, -cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startDown);
            GL11.glVertex3f(height, startDown, -cable.widthDiv2);
            GL11.glNormal3f(1f, 0f, 0f);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endDown);
            GL11.glVertex3f(height, endDown, cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startDown);
            GL11.glVertex3f(height, startDown, cable.widthDiv2);
            GL11.glNormal3f(0f, 0f, 1f);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endDown);
            GL11.glVertex3f(0, endDown, cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startDown);
            GL11.glVertex3f(0, startDown, cable.widthDiv2);
            if (drawBottom) {
                GL11.glNormal3f(0f, 0f, -1f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endDown);
                GL11.glVertex3f(0, endDown, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startDown);
                GL11.glVertex3f(0, startDown, -cable.widthDiv2);
            }
            GL11.glEnd();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0f, -1f, 0f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endDown);
            GL11.glVertex3f(height, endDown, -cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endDown);
            GL11.glVertex3f(height, endDown, cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endDown - height);
            GL11.glVertex3f(0, endDown, cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endDown - height);
            GL11.glVertex3f(0, endDown, -cable.widthDiv2);
            GL11.glEnd();
        }
        if (endUp > startUp) {
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glNormal3f(0f, 0f, -1f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startUp);
            GL11.glVertex3f(0, startUp, -cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endUp);
            GL11.glVertex3f(0, endUp, -cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startUp);
            GL11.glVertex3f(height, startUp, -cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endUp);
            GL11.glVertex3f(height, endUp, -cable.widthDiv2);
            GL11.glNormal3f(1f, 0f, 0f);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startUp);
            GL11.glVertex3f(height, startUp, cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endUp);
            GL11.glVertex3f(height, endUp, cable.widthDiv2);
            GL11.glNormal3f(0f, 0f, 1f);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startUp);
            GL11.glVertex3f(0, startUp, cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endUp);
            GL11.glVertex3f(0, endUp, cable.widthDiv2);
            if (drawBottom) {
                GL11.glNormal3f(0f, 0f, -1f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startUp);
                GL11.glVertex3f(0, startUp, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endUp);
                GL11.glVertex3f(0, endUp, -cable.widthDiv2);
            }
            GL11.glEnd();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0f, 1f, 0f);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endUp + height);
            GL11.glVertex3f(0, endUp, -cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endUp + height);
            GL11.glVertex3f(0, endUp, cable.widthDiv2);
            GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endUp);
            GL11.glVertex3f(height, endUp, cable.widthDiv2);
            GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endUp);
            GL11.glVertex3f(height, endUp, -cable.widthDiv2);
            GL11.glEnd();
        }

    }

    public static void drawNode(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType) {
        if ((connection.mask == 0 || ((connection.get(LRDU.Left) || connection.get(LRDU.Right)) && (connection.get(LRDU.Down) || connection.get(LRDU.Up))) || connection.mask == 1 || connection.mask == 2 || connection.mask == 4 || connection.mask == 8)) {
            float widthDiv2 = cable.widthDiv2 + 1.0f / 16.0f;
            float height = cable.height + 1.0f / 16.0f;
            float tx = 0.75f;
            float ty = 0.5f;
            GL11.glColor4d(1.0, 1.0, 1.0, 1.0);

            // Renders the top and two sides of the node cap
            GL11.glBegin(GL11.GL_QUAD_STRIP);

            GL11.glNormal3f(0f, 1f, 0f);
            GL11.glTexCoord2f(tx + (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(0, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(0, widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx + (widthDiv2) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + (widthDiv2) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, widthDiv2, widthDiv2);

            GL11.glNormal3f(1f, 0f, 0f);
            GL11.glTexCoord2f(tx - (widthDiv2) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx - (widthDiv2) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, widthDiv2);

            GL11.glNormal3f(0f, -1f, 0f);
            GL11.glTexCoord2f(tx - (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(0, -widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx - (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(0, -widthDiv2, widthDiv2);

            GL11.glEnd();

            // Renders remaining 2 sides of the node cap
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0f, 0f, -1f);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2 - cable.height - 1.0f / 16.0f);
            GL11.glVertex3f(0, -widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2 - cable.height - 1.0f / 16.0f);
            GL11.glVertex3f(0, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, -widthDiv2);

            GL11.glNormal3f(0f, 0f, 1f);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2 + cable.height + 1.0f / 16.0f);
            GL11.glVertex3f(0, widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2 + cable.height + 1.0f / 16.0f);
            GL11.glVertex3f(0, -widthDiv2, widthDiv2);
            GL11.glEnd();

        }
    }
}

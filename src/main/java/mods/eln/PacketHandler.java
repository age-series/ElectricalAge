package mods.eln;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.channel.ChannelHandler.Sharable;
import mods.eln.client.ClientProxy;
import mods.eln.debug.DP;
import mods.eln.debug.DPType;
import mods.eln.misc.Coordinate;
import mods.eln.misc.IConfigSharing;
import mods.eln.misc.KeyRegistry;
import mods.eln.misc.Utils;
import mods.eln.node.INodeEntity;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.server.PlayerManager;
import mods.eln.sound.ClientSoundHandler;
import mods.eln.sound.SoundClient;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.tileentity.TileEntity;

import java.io.*;

@Sharable
public class PacketHandler {

    public PacketHandler() {
        Eln.eventChannel.register(this);
    }


    @SubscribeEvent
    public void onServerPacket(ServerCustomPacketEvent event) {
        FMLProxyPacket packet = event.packet;
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet.payload().array()));
        NetworkManager manager = event.manager;
        EntityPlayer player = ((NetHandlerPlayServer) event.handler).playerEntity; // EntityPlayerMP

        packetRx(stream, manager, player);
    }


    public void packetRx(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            switch (stream.readByte()) {
                case Eln.PACKET_PLAYER_KEY:
                    packetPlayerKey(stream, manager, player);
                    break;
                case Eln.PACKET_NODE_SINGLE_SERIALIZED:
                    packetNodeSingleSerialized(stream, manager, player);
                    break;
                case Eln.PACKET_PUBLISH_FOR_NODE:
                    packetForNode(stream, manager, player);
                    break;
                case Eln.PACKET_FOR_CLIENT_NODE:
                    packetForClientNode(stream, manager, player);
                    break;
                case Eln.PACKET_OPEN_LOCAL_GUI:
                    packetOpenLocalGui(stream, manager, player);
                    break;
                case Eln.PACKET_PLAY_SOUND:
                    packetPlaySound(stream, manager, player);
                    break;
                case Eln.PACKET_STOP_SOUND:

                    break;
                case Eln.PACKET_CLIENT_TO_SERVER_CONNECTION:
                    packetNewClient(manager, player);
                    break;
                case Eln.PACKET_SERVER_TO_CLIENT_INFO:
                    packetServerInfo(stream, manager, player);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void packetNewClient(NetworkManager manager, EntityPlayer player) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.PACKET_SERVER_TO_CLIENT_INFO);
            for (IConfigSharing c : Eln.configShared) {
                c.serializeConfig(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.sendPacketToClient(bos, (EntityPlayerMP) player);
    }

    private void packetServerInfo(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        for (IConfigSharing c : Eln.configShared) {
            try {
                c.deserialize(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void packetPlaySound(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            if (stream.readByte() != player.dimension)
                return;
            DP.println(DPType.SOUND, "Playing sound on client!");

            SoundCommand sc = SoundCommand.Companion.fromStream(stream, player.worldObj);
            ClientSoundHandler.SoundData sd = SoundCommand.Companion.toSoundData(sc, player);
            ClientProxy.clientSoundHandler.start(sd);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void packetStopSound(DataInputStream stream, NetworkManager manager, EntityPlayer player) {

    }

    void packetOpenLocalGui(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        EntityPlayer clientPlayer = (EntityPlayer) player;
        try {
            clientPlayer.openGui(Eln.instance, stream.readInt(),
                clientPlayer.worldObj, stream.readInt(), stream.readInt(),
                stream.readInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetForNode(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            Coordinate coordonate = new Coordinate(stream.readInt(),
                stream.readInt(), stream.readInt(), stream.readByte());

            NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordonate);
            if (node != null && node.getNodeUuid().equals(stream.readUTF())) {
                node.networkUnserialize(stream, (EntityPlayerMP) player);
            } else {
                DP.println(DPType.NETWORK, "packetForNode node found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetForClientNode(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        EntityPlayer clientPlayer = (EntityPlayer) player;
        int x, y, z, dimention;
        try {

            x = stream.readInt();
            y = stream.readInt();
            z = stream.readInt();
            dimention = stream.readByte();


            if (clientPlayer.dimension == dimention) {
                TileEntity entity = clientPlayer.worldObj.getTileEntity(x, y, z);
                if (entity instanceof INodeEntity) {
                    INodeEntity node = (INodeEntity) entity;
                    if (node.getNodeUuid().equals(stream.readUTF())) {
                        node.serverPacketUnserialize(stream);
                        if (0 != stream.available()) {
                            DP.println(DPType.NETWORK, "0 != stream.available()");
                        }
                    } else {
                        DP.println(DPType.NETWORK, "Wrong node UUID warning");
                        int dataSkipLength = stream.readByte();
                        for (int idx = 0; idx < dataSkipLength; idx++) {
                            stream.readByte();
                        }
                    }
                }
            } else
                DP.println(DPType.NETWORK, "No node found for " + x + " " + y + " " + z);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void packetNodeSingleSerialized(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        try {
            EntityPlayer clientPlayer = player;
            int x, y, z, dimention;
            x = stream.readInt();
            y = stream.readInt();
            z = stream.readInt();
            dimention = stream.readByte();

            if (clientPlayer.dimension == dimention) {
                TileEntity entity = clientPlayer.worldObj.getTileEntity(x, y, z);
                if (entity instanceof INodeEntity) {
                    INodeEntity node = (INodeEntity) entity;
                    if (node.getNodeUuid().equals(stream.readUTF())) {
                        node.serverPublishUnserialize(stream);
                        if (0 != stream.available()) {
                            DP.println(DPType.NETWORK, "0 != stream.available()");

                        }
                    } else {
                        DP.println(DPType.NETWORK, "Wrong node UUID warning");
                        int dataSkipLength = stream.readByte();
                        for (int idx = 0; idx < dataSkipLength; idx++) {
                            stream.readByte();
                        }
                    }
                } else
                    DP.println(DPType.NETWORK, "No node found for " + x + " " + y + " " + z);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * packetPlayerKey - handles key interactions
     * @param stream Network Stream
     * @param manager Network Manager
     * @param player Player reference (the one whose client sent the packet)
     */
    void packetPlayerKey(DataInputStream stream, NetworkManager manager, EntityPlayer player) {
        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        try {
            byte id = stream.readByte();
            boolean state = stream.readBoolean();

            if (id == KeyRegistry.getKeyID("Wrench")) {
                PlayerManager.PlayerMetadata metadata = Eln.playerManager.get(playerMP);
                metadata.setInteractEnable(state);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

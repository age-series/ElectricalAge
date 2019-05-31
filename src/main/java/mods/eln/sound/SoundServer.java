package mods.eln.sound;

import cpw.mods.fml.common.FMLCommonHandler;
import mods.eln.Eln;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SoundServer {
    /*public static void playFromBlock(World world,int x, int y, int z, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(world, x+0.5, y+0.5, z+0.5, track, volume, pitch, rangeNominal, rangeMax,1);
	}
	public static void playFromCoord(Coordonate c, String track, float volume, float pitch,float rangeNominal,float rangeMax){
		play(c.world(), c.x+0.5, c.y+0.5, c.z+0.5, track, volume, pitch, rangeNominal, rangeMax,1);
	}
	
	public static void playFromCoord(Coordonate c, String track, float volume, float pitch,float rangeNominal,float rangeMax,float blockFactor){
		play(c.world(), c.x+0.5, c.y+0.5, c.z+0.5, track, volume, pitch, rangeNominal, rangeMax,blockFactor);
	}*/


    public static void play(SoundCommand p) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);

        try {
            stream.writeByte(Eln.PACKET_PLAY_SOUND);

            stream.writeByte(p.world.provider.dimensionId);

            p.writeTo(stream);

            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

            for (Object obj : server.getConfigurationManager().playerEntityList) {
                EntityPlayerMP player = (EntityPlayerMP) obj;
                if (player.dimension == p.world.provider.dimensionId && player.getDistance(p.x, p.y, p.z) < p.rangeMax + 2)
                    ;
                Utils.sendPacketToClient(bos, player);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

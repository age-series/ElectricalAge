package mods.eln.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.six.SixNodeRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.node.transparent.TransparentNodeRender;
import mods.eln.sixnode.tutorialsign.TutorialSignOverlay;
import mods.eln.sound.SoundClientEventListener;
import net.minecraft.client.model.ModelSilverfish;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    public static UuidManager uuidManager;
    public static SoundClientEventListener soundClientEventListener;

    @Override
    public void registerRenderers() {
        new ClientPacketHandler();
        ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
        ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());

        MinecraftForgeClient.registerItemRenderer(Vars.transparentNodeItem, Vars.transparentNodeItem);
        MinecraftForgeClient.registerItemRenderer(Vars.sixNodeItem, Vars.sixNodeItem);
        MinecraftForgeClient.registerItemRenderer(Vars.sharedItem, Vars.sharedItem);
        MinecraftForgeClient.registerItemRenderer(Vars.sharedItemStackOne, Vars.sharedItemStackOne);

        RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(), (float) 0.3));

        Vars.clientKeyHandler = new ClientKeyHandler();
        FMLCommonHandler.instance().bus().register(Vars.clientKeyHandler);
        MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
        uuidManager = new UuidManager();
        soundClientEventListener = new SoundClientEventListener(uuidManager);

        if (Vars.versionCheckEnabled)
            FMLCommonHandler.instance().bus().register(VersionCheckerHandler.getInstance());

        if (Vars.analyticsEnabled)
            FMLCommonHandler.instance().bus().register(AnalyticsHandler.getInstance());

        new FrameTime();
        new ConnectionListener();
    }
}

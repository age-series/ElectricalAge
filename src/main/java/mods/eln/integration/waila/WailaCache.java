package mods.eln.integration.waila;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.misc.Coordonate;
import mods.eln.packets.GhostNodeWailaRequestPacket;
import mods.eln.packets.SixNodeWailaRequestPacket;
import mods.eln.packets.TransparentNodeRequestPacket;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gregory Maddra on 2016-06-29.
 */
public class WailaCache {

    public static LoadingCache<Coordonate, Map<String, String>> nodes = CacheBuilder.newBuilder()
        .maximumSize(20)
        .refreshAfterWrite(2, TimeUnit.SECONDS)
        .build(
            new CacheLoader<Coordonate, Map<String, String>>() {
                public Map<String, String> load(Coordonate key) throws Exception {
                    Vars.elnNetwork.sendToServer(new TransparentNodeRequestPacket(key));
                    return null;
                }

                @Override
                public ListenableFuture<Map<String, String>> reload(Coordonate key,
                                                                    Map<String, String> oldValue) throws Exception {
                    load(key);
                    return Futures.immediateFuture(oldValue);
                }
            }
        );

    public static LoadingCache<SixNodeCoordonate, SixNodeWailaData> sixNodes = CacheBuilder.newBuilder()
        .maximumSize(20)
        .refreshAfterWrite(2, TimeUnit.SECONDS)
        .build(
            new CacheLoader<SixNodeCoordonate, SixNodeWailaData>() {
                public SixNodeWailaData load(SixNodeCoordonate key) throws Exception {
                    Vars.elnNetwork.sendToServer(new SixNodeWailaRequestPacket(key.getCoord(), key.getSide()));
                    return null;
                }

                @Override
                public ListenableFuture<SixNodeWailaData> reload(SixNodeCoordonate key,
                                                                 SixNodeWailaData oldValue) throws Exception {
                    load(key);
                    return Futures.immediateFuture(oldValue);
                }
            }
        );

    public static LoadingCache<Coordonate, GhostNodeWailaData> ghostNodes = CacheBuilder.newBuilder()
        .maximumSize(20)
        .refreshAfterWrite(10, TimeUnit.SECONDS)
        .build(
            new CacheLoader<Coordonate, GhostNodeWailaData>() {
                public GhostNodeWailaData load(Coordonate key) throws Exception {
                    Vars.elnNetwork.sendToServer(new GhostNodeWailaRequestPacket(key));
                    return null;
                }

                @Override
                public ListenableFuture<GhostNodeWailaData> reload(Coordonate key,
                                                                   GhostNodeWailaData oldValue) throws Exception {
                    load(key);
                    return Futures.immediateFuture(oldValue);
                }
            }
        );

}

package mods.eln.integration.waila;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.packets.GhostNodeWailaRequestPacket;
import mods.eln.packets.SixNodeWailaRequestPacket;
import mods.eln.packets.TransparentNodeRequestPacket;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gregory Maddra on 2016-06-29.
 */
public class WailaCache {

    public static LoadingCache<Coordinate, Map<String, String>> nodes = CacheBuilder.newBuilder()
        .maximumSize(20)
        .refreshAfterWrite(2, TimeUnit.SECONDS)
        .build(
            cacheLoader(
                key -> {
                    Eln.elnNetwork.sendToServer(new TransparentNodeRequestPacket(key));
                    return null;
                }
            )
        );

    public static LoadingCache<SixNodeCoordonate, SixNodeWailaData> sixNodes = CacheBuilder.newBuilder()
        .maximumSize(20)
        .refreshAfterWrite(2, TimeUnit.SECONDS)
        .build(
            cacheLoader(
                key -> {
                    Eln.elnNetwork.sendToServer(new SixNodeWailaRequestPacket(key.getCoord(), key.getSide()));
                    return null;
                }
            )
        );

    public static LoadingCache<Coordinate, GhostNodeWailaData> ghostNodes = CacheBuilder.newBuilder()
        .maximumSize(20)
        .refreshAfterWrite(10, TimeUnit.SECONDS)
        .build(
            cacheLoader(
                key -> {
                    Eln.elnNetwork.sendToServer(new GhostNodeWailaRequestPacket(key));
                    return null;
                }
            )
        );

    private static <K, V> CacheLoader<K, V> cacheLoader(LoaderFunction<K, V> loader) {
        return new WailaDelegatingCacheLoader<>(loader);
    }
}

interface LoaderFunction<K, V> {
    V load(K key) throws Exception;
}

final class WailaDelegatingCacheLoader<K, V> extends CacheLoader<K, V> {
    private final LoaderFunction<K, V> loader;

    WailaDelegatingCacheLoader(LoaderFunction<K, V> loader) {
        this.loader = loader;
    }

    @Override
    public V load(K key) throws Exception {
        return loader.load(key);
    }

    @Override
    public ListenableFuture<V> reload(K key, V oldValue) throws Exception {
        loader.load(key);
        return Futures.immediateFuture(oldValue);
    }
}

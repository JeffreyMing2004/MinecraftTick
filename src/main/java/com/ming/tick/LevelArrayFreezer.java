package com.ming.tick;

import java.lang.reflect.Field;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Pauses world (game) ticking by emptying the cached world array that
 * {@code MinecraftServer#tickChildren} iterates every tick. With the array empty
 * no level is ticked at all (entities, block entities, random ticks, weather,
 * day time, mob spawning...), while the server loop, network, player logins and
 * console commands keep running normally.
 *
 * <p>{@code worldArray}, {@code worldArrayMarker} and {@code worldArrayLast} are
 * fields added by a Forge patch rather than vanilla, so their names are identical
 * in development and in production jars.</p>
 */
final class LevelArrayFreezer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ServerLevel[] EMPTY = new ServerLevel[0];

    private static boolean initAttempted;
    private static Field worldArray;
    private static Field worldArrayMarker;
    private static Field worldArrayLast;

    private LevelArrayFreezer() {
    }

    static boolean isAvailable() {
        if (!initAttempted) {
            initAttempted = true;
            try {
                Field array = MinecraftServer.class.getDeclaredField("worldArray");
                Field marker = MinecraftServer.class.getDeclaredField("worldArrayMarker");
                Field last = MinecraftServer.class.getDeclaredField("worldArrayLast");
                array.setAccessible(true);
                marker.setAccessible(true);
                last.setAccessible(true);
                worldArray = array;
                worldArrayMarker = marker;
                worldArrayLast = last;
            } catch (ReflectiveOperationException e) {
                LOGGER.error("tick mod: could not access the Forge world tick cache, empty-server pausing stays disabled", e);
            }
        }
        return worldArray != null;
    }

    /**
     * Skips all world ticks until {@link #resume()} is called. Must be re-asserted
     * every tick because a world being added or removed invalidates the cache.
     */
    static void pause(MinecraftServer server) {
        try {
            worldArray.set(server, EMPTY);
            worldArrayLast.setInt(server, worldArrayMarker.getInt(server));
        } catch (IllegalAccessException e) {
            LOGGER.error("tick mod: failed to pause game ticks", e);
        }
    }

    /** Restores normal ticking; the array rebuilds itself from the live level map on the next tick. */
    static void resume(MinecraftServer server) {
        try {
            worldArrayLast.setInt(server, -1);
        } catch (IllegalAccessException e) {
            LOGGER.error("tick mod: failed to resume game ticks", e);
        }
    }
}

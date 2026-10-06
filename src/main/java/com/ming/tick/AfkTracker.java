package com.ming.tick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Detects idling players by comparing head rotation and position every tick.
 * A player that has not moved for the configured timeout counts as AFK
 * ("fake offline") for the pause logic, so an AFK fisher no longer keeps the
 * world ticking. Chat, block/entity interaction and container opening also
 * count as activity, for players who interact without looking around.
 *
 * <p>Players can also be marked AFK manually with /afk; the mark applies
 * immediately and is cleared by any real activity or /afk off.</p>
 *
 * <p>The tracker runs on the server thread inside the tick loop and keeps
 * working while the world is frozen, so a frozen player who starts moving
 * resumes the world within the same tick.</p>
 */
final class AfkTracker {

    private static final class State {
        double x, y, z;
        float yRot, xRot;
        long lastActiveMillis;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Set<UUID> MANUAL_AFK = new HashSet<>();

    private AfkTracker() {
    }

    /** Updates movement baselines and returns the players that are not AFK. */
    static List<ServerPlayer> update(MinecraftServer server, long afkTimeoutMillis) {
        long now = Util.getMillis();
        List<ServerPlayer> active = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            State state = STATES.computeIfAbsent(player.getUUID(), id -> {
                State created = new State();
                snapshot(created, player);
                created.lastActiveMillis = now;
                return created;
            });
            if (state.x != player.getX() || state.y != player.getY() || state.z != player.getZ()
                    || state.yRot != player.getYRot() || state.xRot != player.getXRot()) {
                snapshot(state, player);
                state.lastActiveMillis = now;
                MANUAL_AFK.remove(player.getUUID());
            }
            boolean inactive = MANUAL_AFK.contains(player.getUUID())
                    || (afkTimeoutMillis > 0 && now - state.lastActiveMillis >= afkTimeoutMillis);
            if (!inactive) {
                active.add(player);
            }
        }
        return active;
    }

    static boolean isManualAfk(UUID playerId) {
        return MANUAL_AFK.contains(playerId);
    }

    static void setManualAfk(UUID playerId, boolean afk) {
        if (afk) {
            MANUAL_AFK.add(playerId);
        } else {
            MANUAL_AFK.remove(playerId);
            markActive(playerId);
        }
    }

    static void markActive(UUID playerId) {
        State state = STATES.get(playerId);
        if (state != null) {
            state.lastActiveMillis = Util.getMillis();
        }
        MANUAL_AFK.remove(playerId);
    }

    static void forget(UUID playerId) {
        STATES.remove(playerId);
        MANUAL_AFK.remove(playerId);
    }

    static void clear() {
        STATES.clear();
        MANUAL_AFK.clear();
    }

    private static void snapshot(State state, ServerPlayer player) {
        state.x = player.getX();
        state.y = player.getY();
        state.z = player.getZ();
        state.yRot = player.getYRot();
        state.xRot = player.getXRot();
    }
}

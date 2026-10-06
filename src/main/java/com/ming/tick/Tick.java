package com.ming.tick;

import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Server-side utility mod: while no players are online - or every player online
 * is AFK - all game (world) ticks are paused, and they resume at the normal rate
 * as soon as a player joins or starts moving again.
 */
@Mod(Tick.MODID)
public class Tick {

    public static final String MODID = "tick";
    static final Logger LOGGER = LogUtils.getLogger();

    public Tick() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @Mod.EventBusSubscriber(modid = Tick.MODID)
    public static class ServerEvents {

        private static boolean paused;
        /** True while paused because every online player was AFK (false = paused with nobody online). */
        private static boolean pausedByAfk;

        // Runs in the START phase, before MinecraftServer#tickChildren iterates
        // the world array, so a freeze/resume takes effect within the same tick.
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            MinecraftServer server = event.getServer();
            if (!LevelArrayFreezer.isAvailable() || !Config.pauseWhenEmpty) {
                return;
            }

            // Manual /afk marks count even when the automatic timeout is disabled (0).
            List<ServerPlayer> activePlayers = AfkTracker.update(server, Config.afkTimeoutSeconds * 1000L);
            boolean empty = activePlayers.isEmpty();

            if (empty) {
                pausedByAfk = !server.getPlayerList().getPlayers().isEmpty();
                if (!paused) {
                    paused = true;
                    if (pausedByAfk) {
                        boolean allManual = server.getPlayerList().getPlayers().stream()
                                .allMatch(p -> AfkTracker.isManualAfk(p.getUUID()));
                        if (allManual) {
                            LOGGER.info("检测到在线玩家均已手动挂机（视为假离线），已暂停游戏刻，世界天数停止推进");
                        } else {
                            LOGGER.info("检测到在线玩家均已挂机超过 {} 秒（视为假离线），已暂停游戏刻，世界天数停止推进", Config.afkTimeoutSeconds);
                        }
                    } else {
                        LOGGER.info("检测到服务器无人在线，已暂停游戏刻，世界天数停止推进");
                    }
                }
                LevelArrayFreezer.pause(server);
            } else if (paused) {
                paused = false;
                boolean byAfk = pausedByAfk;
                pausedByAfk = false;
                LevelArrayFreezer.resume(server);
                LOGGER.info("检测到玩家 {} {}（当前在线 {} 人），游戏天数继续运行，游戏刻已恢复",
                        activePlayers.get(0).getGameProfile().getName(),
                        byAfk ? "恢复活动" : "进入",
                        server.getPlayerList().getPlayerCount());
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            AfkCommand.register(event.getDispatcher());
        }

        // A player who interacts without moving the camera still counts as active.
        @SubscribeEvent
        public static void onChat(ServerChatEvent event) {
            AfkTracker.markActive(event.getPlayer().getUUID());
        }

        // A player who interacts without moving the camera still counts as active —
        // except fishing rod casts/retracts (including the held-right-click use spam of
        // AFK fishing farms), which must not keep the world running.
        @SubscribeEvent
        public static void onInteract(PlayerInteractEvent event) {
            if (event.getEntity().level().isClientSide || event.getItemStack().getItem() instanceof FishingRodItem) {
                return;
            }
            AfkTracker.markActive(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onContainerOpen(PlayerContainerEvent.Open event) {
            if (!event.getEntity().level().isClientSide) {
                AfkTracker.markActive(event.getEntity().getUUID());
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            AfkTracker.forget(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            AfkTracker.clear();
        }
    }
}

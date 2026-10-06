package com.ming.tick;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Server-side utility mod: while no players are online all game (world) ticks
 * are paused, and they resume at the normal rate as soon as a player joins.
 */
@Mod(Tick.MODID)
public class Tick {

    public static final String MODID = "tick";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Tick() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @Mod.EventBusSubscriber(modid = Tick.MODID)
    public static class ServerEvents {

        private static boolean paused;

        // Runs in the START phase, before MinecraftServer#tickChildren iterates
        // the world array, so a freeze/resume takes effect within the same tick.
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            MinecraftServer server = event.getServer();
            if (!LevelArrayFreezer.isAvailable()) {
                return;
            }

            boolean empty = server.getPlayerList().getPlayers().isEmpty();
            if (empty && Config.pauseWhenEmpty) {
                if (!paused) {
                    paused = true;
                    LOGGER.info("检测到服务器无人在线，已暂停游戏刻，世界天数停止推进");
                }
                LevelArrayFreezer.pause(server);
            } else if (paused) {
                paused = false;
                LevelArrayFreezer.resume(server);
                LOGGER.info("检测到玩家 {} 进入（当前在线 {} 人），游戏天数继续运行，游戏刻已恢复",
                        server.getPlayerList().getPlayers().get(0).getGameProfile().getName(),
                        server.getPlayerList().getPlayerCount());
            }
        }
    }
}

package com.ming.tick;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Tick.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue PAUSE_WHEN_EMPTY = BUILDER
            .comment("Pause all game (world) ticks while no players are online and resume them when a player joins")
            .define("pauseWhenEmpty", true);

    private static final ForgeConfigSpec.IntValue AFK_TIMEOUT_SECONDS = BUILDER
            .comment("Seconds without moving the camera or interacting before a player counts as AFK (fake offline).",
                    "While every player online is AFK, world ticks are paused exactly like on an empty server,",
                    "so AFK fishing no longer keeps the world running. 0 disables AFK detection.")
            .defineInRange("afkTimeoutSeconds", 300, 0, 86400);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean pauseWhenEmpty = true;
    public static int afkTimeoutSeconds = 300;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        pauseWhenEmpty = PAUSE_WHEN_EMPTY.get();
        afkTimeoutSeconds = AFK_TIMEOUT_SECONDS.get();
    }
}

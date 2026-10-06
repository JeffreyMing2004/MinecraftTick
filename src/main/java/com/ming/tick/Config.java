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

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean pauseWhenEmpty = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        pauseWhenEmpty = PAUSE_WHEN_EMPTY.get();
    }
}

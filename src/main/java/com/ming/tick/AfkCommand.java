package com.ming.tick;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /afk — manually toggles the "fake offline" state used by the pause logic.
 *
 * <ul>
 *   <li>/afk — toggle for yourself (any player)</li>
 *   <li>/afk on|off — set for yourself (any player)</li>
 *   <li>/afk &lt;player&gt; on|off — set for someone else (permission level 2)</li>
 * </ul>
 *
 * <p>A manual mark applies immediately without waiting for the configured
 * timeout and survives until the player uses /afk off or shows real activity
 * (movement, interaction, chat), which clears it automatically.</p>
 */
final class AfkCommand {

    private AfkCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
                .executes(ctx -> toggle(ctx.getSource()))
                .then(Commands.literal("on")
                        .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("off")
                        .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(), false)))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("on")
                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false)))));
    }

    private static int toggle(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return set(source, player, !AfkTracker.isManualAfk(player.getUUID()));
    }

    private static int set(CommandSourceStack source, ServerPlayer player, boolean afk) {
        AfkTracker.setManualAfk(player.getUUID(), afk);
        player.sendSystemMessage(Component.literal(afk
                ? "你已进入挂机状态，移动或输入 /afk off 取消"
                : "已取消挂机状态"));
        String target = player.getGameProfile().getName();
        source.sendSuccess(() -> Component.literal(target + (afk ? " 已进入挂机状态" : " 已取消挂机状态")), true);
        Tick.LOGGER.info("玩家 {} {}挂机状态（执行者：{}）",
                target,
                afk ? "进入手动" : "取消手动",
                source.getTextName());
        return 1;
    }
}

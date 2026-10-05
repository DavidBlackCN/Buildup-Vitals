package com.davidblackcn.buildupvitals.command;

import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RecoveryCommand {
    private RecoveryCommand() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
                Commands.literal("buildupvitals")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("recovery")
                                .executes(command -> show(command.getSource(), command.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(command -> show(command.getSource(), EntityArgument.getPlayer(command, "player")))))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        var state = PlayerRecovery.state(player);
        var benefit = PlayerMealBenefits.state(player);
        int interval = state.mode() == RecoveryState.Mode.FOOD ? PlayerMealBenefits.foodInterval(player) : state.mode().interval();
        source.sendSuccess(() -> Component.literal("Recovery: " + player.getScoreboardName()
                + ", reserve=" + state.reserve() + " HP, mode=" + state.mode()
                + ", progress=" + state.progress() + "/" + interval
                + ", meal_benefit=" + benefit.type().map(Object::toString).orElse("none")
                + ", remaining_ticks=" + benefit.remainingTicks()
                + ", health=" + player.getHealth() + "/" + player.getMaxHealth()
                + ", hunger=" + player.getFoodData().getFoodLevel()
                + ", saturation=" + player.getFoodData().getSaturationLevel()), false);
        return 1;
    }
}

package com.davidblackcn.buildupvitals.command;

import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
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
        var food = player.getFoodData();
        double natural = com.davidblackcn.buildupvitals.food.recovery.RecoveryController.naturalInterval(
                new com.davidblackcn.buildupvitals.food.recovery.RecoveryController.Conditions(player.isAlive(), !player.isCreative() && !player.isSpectator(),
                        player.getHealth(), player.getMaxHealth(), food.getFoodLevel(), food.getSaturationLevel(),
                        player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION)), PlayerRecovery.naturalSpeed(player));
        double interval = state.mode() == com.davidblackcn.buildupvitals.food.recovery.RecoveryState.Mode.FOOD
                ? Math.min(PlayerMealBenefits.foodInterval(player), natural) : natural;
        var overeating = com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.state(player);
        boolean thirst = com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled();
        String hydration = thirst ? com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge.thirst(player) + "/"
                + com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge.quenched(player) + ", quenched_bonus="
                + com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge.quenchedRecovery(player) : "disabled";
        source.sendSuccess(() -> Component.literal("Recovery: " + player.getScoreboardName()
                + ", reserve=" + state.reserve() + " HP, mode=" + state.mode()
                + ", progress=" + state.progress() + "/" + interval
                + ", meal_benefit=" + benefit.type().map(Object::toString).orElse("none")
                + ", remaining_ticks=" + benefit.remainingTicks()
                + ", health=" + player.getHealth() + "/" + player.getMaxHealth()
                + ", hunger=" + player.getFoodData().getFoodLevel()
                + ", saturation=" + player.getFoodData().getSaturationLevel()
                + ", overeat_load=" + overeating.load() + ", overfull=" + com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.overfull(player)
                + ", variety=" + PlayerDiet.state(player).variety().score() + ", natural_speed=" + PlayerRecovery.naturalSpeed(player)
                + ", TWT2=" + thirst + ", thirst/quenched=" + hydration), false);
        return 1;
    }
}

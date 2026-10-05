package com.davidblackcn.buildupvitals.command;

import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.diet.VarietyBalance;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DietCommand {
    private DietCommand() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
                Commands.literal("buildupvitals")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("diet")
                                .executes(command -> show(command.getSource(), command.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(command -> show(command.getSource(), EntityArgument.getPlayer(command, "player")))))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        var memory = PlayerDiet.state(player);
        var variety = memory.variety();
        var text = new StringBuilder("Diet: ").append(player.getScoreboardName())
                .append(", meals=").append(memory.entries().size()).append('/').append(VarietyBalance.WINDOW)
                .append(", score=").append(variety.score()).append(", repeat_factor=").append(variety.repeatFactor())
                .append("\ncategory_diversity=").append(variety.categoryDiversity())
                .append(", group_diversity=").append(variety.groupDiversity()).append(", quality_weight=").append(variety.qualityWeight())
                .append("\nfood_multiplier=").append(variety.foodMultiplier()).append(", benefit_multiplier=").append(variety.benefitMultiplier())
                .append(", well_fed_multiplier=").append(variety.wellFedMultiplier()).append(", well_fed_ticks=").append(variety.wellFedInterval())
                .append("\nHistory (oldest first; overworld game ticks):");
        for (var entry : memory.entries()) text.append("\n").append(entry.foodId()).append(" quality=").append(entry.quality().id())
                .append(" categories=").append(entry.categories()).append(" group=").append(entry.varietyGroup()).append(" time=").append(entry.timestamp());
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }
}

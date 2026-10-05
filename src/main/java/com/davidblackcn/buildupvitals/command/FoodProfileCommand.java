package com.davidblackcn.buildupvitals.command;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.ProfileSnapshot;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class FoodProfileCommand {
    private FoodProfileCommand() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
                Commands.literal("buildupvitals")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("food")
                                .then(Commands.literal("profile")
                                        .then(Commands.argument("item", ItemArgument.item(context))
                                                .executes(command -> show(command.getSource(), ItemArgument
                                                        .getItem(command, "item").item().unwrapKey().orElseThrow().identifier())))))));
    }

    private static int show(CommandSourceStack source, Identifier item) {
        ProfileSnapshot snapshot = FoodProfileLoader.snapshot(source.getServer());
        var match = snapshot.resolve(item);
        var profile = match.profile();
        String origin = match.definition().map(definition -> "profile=" + definition.source().id()
                + ", file=" + definition.source().file() + ", pack=" + definition.source().pack()
                + ", selector=" + definition.selector().type() + ":" + definition.selector().id()
                + ", pack_priority=" + definition.source().packPriority() + ", priority=" + definition.priority())
                .orElse("profile=<fallback>, source=code (no matching profile)");
        String text = "Buildup Vitals food profile: " + item + "\n" + origin
                + "\nfallback=" + match.fallback() + ", quality=" + profile.quality().id()
                + ", recovery.health=" + profile.recoveryHealth()
                + "\nhydration.thirst=" + profile.hydration().thirst() + ", hydration.quenched=" + profile.hydration().quenched()
                + "\ncategories=" + profile.categories().stream().map(DietCategory::id).toList()
                + ", variety_group=" + profile.varietyGroupFor(item) + ", traits=" + profile.traits()
                + "\nmeal_benefit=" + profile.mealBenefit().map(Identifier::toString).orElse("<none>")
                + ", overrides.hunger=" + (profile.overrides().hunger().isPresent() ? profile.overrides().hunger().getAsInt() : "<vanilla>")
                + ", overrides.saturation=" + (profile.overrides().saturation().isPresent() ? profile.overrides().saturation().getAsDouble() : "<vanilla>")
                + "\nsnapshot=" + snapshot.profileCount() + " profiles / " + snapshot.itemCount()
                + " indexed items; recovery.health supplies food recovery reserve";
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}

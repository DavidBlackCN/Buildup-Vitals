package com.davidblackcn.buildupvitals.player;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.food.benefit.MealBenefitBalance;
import com.davidblackcn.buildupvitals.food.benefit.MealBenefitState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class MealBenefitAttachments {
    public static final Codec<MealBenefitState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("type").forGetter(MealBenefitState::type),
            Codec.intRange(0, MealBenefitBalance.MAX_DURATION).fieldOf("remaining_ticks").forGetter(MealBenefitState::remainingTicks)
    ).apply(instance, (type, ticks) -> type.isEmpty() || ticks == 0 ? MealBenefitState.EMPTY : new MealBenefitState(type, ticks)));

    public static final AttachmentType<MealBenefitState> MEAL_BENEFIT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "meal_benefit"),
            builder -> builder.initializer(() -> MealBenefitState.EMPTY).persistent(CODEC));

    private MealBenefitAttachments() { }

    public static void register() { }
}

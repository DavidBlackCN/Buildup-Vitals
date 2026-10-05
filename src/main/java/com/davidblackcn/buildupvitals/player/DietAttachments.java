package com.davidblackcn.buildupvitals.player;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.diet.DietEntry;
import com.davidblackcn.buildupvitals.diet.DietMemory;
import com.davidblackcn.buildupvitals.diet.VarietyBalance;
import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class DietAttachments {
    public static final Codec<DietEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("food_id").forGetter(DietEntry::foodId),
            enumCodec(FoodQuality.values()).fieldOf("quality").forGetter(DietEntry::quality),
            enumCodec(DietCategory.values()).listOf(0, DietCategory.values().length)
                    .validate(list -> list.stream().distinct().count() == list.size()
                            ? DataResult.success(list) : DataResult.error(() -> "Duplicate diet categories"))
                    .fieldOf("categories").forGetter(DietEntry::categories),
            Identifier.CODEC.fieldOf("variety_group").forGetter(DietEntry::varietyGroup),
            Codec.LONG.fieldOf("timestamp").forGetter(DietEntry::timestamp)
    ).apply(instance, DietEntry::new));

    public static final Codec<DietMemory> CODEC = ENTRY_CODEC.listOf(0, VarietyBalance.WINDOW)
            .xmap(DietMemory::new, DietMemory::entries).fieldOf("entries").codec();

    public static final AttachmentType<DietMemory> DIET = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "diet_memory"),
            builder -> builder.initializer(() -> DietMemory.EMPTY).persistent(CODEC));

    private DietAttachments() { }

    private static <T extends Enum<T>> Codec<T> enumCodec(T[] values) {
        return Codec.STRING.comapFlatMap(name -> Arrays.stream(values)
                .filter(value -> value.name().toLowerCase(Locale.ROOT).equals(name)).findFirst()
                .map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown diet enum value: " + name)),
                value -> value.name().toLowerCase(Locale.ROOT));
    }

    public static void register() { }
}

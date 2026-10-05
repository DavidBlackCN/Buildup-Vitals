package com.davidblackcn.buildupvitals.player;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryBalance;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class RecoveryAttachments {
    private static final Codec<RecoveryState.Mode> MODE = Codec.STRING.comapFlatMap(value -> {
        for (var mode : RecoveryState.Mode.values()) {
            if (mode.name().toLowerCase(Locale.ROOT).equals(value)) {
                return DataResult.success(mode);
            }
        }
        return DataResult.error(() -> "Unknown recovery mode: " + value);
    }, mode -> mode.name().toLowerCase(Locale.ROOT));

    public static final Codec<RecoveryState> CODEC = RecordCodecBuilder.<RecoveryState>create(instance -> instance.group(
            Codec.doubleRange(0, RecoveryBalance.MAX_RESERVE)
                    .validate(value -> Double.isFinite(value) ? DataResult.success(value) : DataResult.error(() -> "Non-finite reserve"))
                    .fieldOf("reserve").forGetter(RecoveryState::reserve),
            Codec.intRange(0, RecoveryBalance.STABLE_TICKS - 1).fieldOf("progress").forGetter(RecoveryState::progress),
            MODE.fieldOf("mode").forGetter(RecoveryState::mode)
    ).apply(instance, (reserve, progress, mode) -> new RecoveryState(reserve, Math.min(progress, mode.interval() - 1), mode)));

    public static final AttachmentType<RecoveryState> RECOVERY = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "recovery"),
            builder -> builder.initializer(() -> RecoveryState.EMPTY).persistent(CODEC));

    private RecoveryAttachments() { }

    public static void register() {
        // Class initialization registers the persistent type. No copyOnDeath and no client sync.
    }
}

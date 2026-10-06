package com.davidblackcn.buildupvitals.player;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class RecoveryAttachments {
    public static final Codec<RecoveryState> CODEC = RecoveryState.CODEC;

    public static final AttachmentType<RecoveryState> RECOVERY = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "recovery"),
            builder -> builder.initializer(() -> RecoveryState.EMPTY).persistent(CODEC));

    private RecoveryAttachments() { }

    public static void register() {
        // Class initialization registers the persistent type. No copyOnDeath and no client sync.
    }
}

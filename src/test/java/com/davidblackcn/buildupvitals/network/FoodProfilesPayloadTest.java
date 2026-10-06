package com.davidblackcn.buildupvitals.network;

import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodProfilesPayloadTest {
    @Test
    void hydrationAndServerCapabilityRoundTripWithoutOptionalMod() {
        var id = Identifier.parse("test:drink");
        var profile = new TooltipProfile(FoodQuality.BASIC, 0, List.of(), Optional.empty(), id, Optional.of(id),
                new com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration(6, Integer.MAX_VALUE),
                Optional.of(com.davidblackcn.buildupvitals.food.profile.ConsumptionSpeed.FAST));
        var payload = new FoodProfilesPayload(true, true, Map.of(id, profile));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FoodProfilesPayload.CODEC.encode(buffer, payload);
            assertEquals(payload, FoodProfilesPayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
        assertThrows(IllegalArgumentException.class, () -> new FoodProfilesPayload(false, true, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new TooltipProfile(FoodQuality.BASIC, 0, List.of(), Optional.empty(), id, Optional.of(id),
                new com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration(-1, 0)));
    }

    @Test
    void completeSnapshotRoundTripsIncludingUnknownBenefitAndEmptyCategories() {
        var id = Identifier.parse("test:meal");
        var profile = new TooltipProfile(FoodQuality.FEAST, 3.25, List.of(DietCategory.PROTEIN, DietCategory.GRAIN),
                Optional.of(Identifier.parse("test:unimplemented")), id, Optional.of(id));
        for (var payload : List.of(new FoodProfilesPayload(true, Map.of(id, profile)),
                new FoodProfilesPayload(true, Map.of(id, TooltipProfile.fallback(id))),
                new FoodProfilesPayload(true, Map.of()), new FoodProfilesPayload(false, Map.of()))) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                FoodProfilesPayload.CODEC.encode(buffer, payload);
                assertEquals(payload, FoodProfilesPayload.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }

    @Test
    void invalidCountsAndTruncatedPacketsAreRejectedBeforeAllocation() {
        for (int count : new int[]{-1, FoodProfilesPayload.MAX_ITEMS + 1, 1}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeBoolean(true);
                buffer.writeBoolean(false);
                buffer.writeVarInt(count);
                assertThrows(RuntimeException.class, () -> FoodProfilesPayload.CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test
    void malformedMetadataAndUnavailableNonemptySnapshotsAreRejected() {
        var id = Identifier.parse("test:meal");
        assertThrows(IllegalArgumentException.class, () -> new TooltipProfile(FoodQuality.MEAL, Double.NaN, List.of(), Optional.empty(), id, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TooltipProfile(FoodQuality.MEAL, -1, List.of(), Optional.empty(), id, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TooltipProfile(FoodQuality.MEAL, 1, List.of(DietCategory.FRUIT, DietCategory.FRUIT), Optional.empty(), id, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new FoodProfilesPayload(false, Map.of(id, TooltipProfile.fallback(id))));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeBoolean(true);
            buffer.writeBoolean(false);
            buffer.writeVarInt(1);
            buffer.writeIdentifier(id);
            buffer.writeVarInt(100);
            assertThrows(RuntimeException.class, () -> FoodProfilesPayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}

package com.davidblackcn.buildupvitals.network;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.food.profile.ConsumptionSpeed;
import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Complete replacement snapshot; Fabric's large payload API handles transport fragmentation. */
public record FoodProfilesPayload(boolean available, boolean hydrationEnabled, Map<Identifier, TooltipProfile> profiles) implements CustomPacketPayload {
    public static final int MAX_ITEMS = 65536;
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    public static final Type<FoodProfilesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "food_profiles_v3"));
    public static final StreamCodec<FriendlyByteBuf, FoodProfilesPayload> CODEC = StreamCodec.of(FoodProfilesPayload::write, FoodProfilesPayload::read);

    public FoodProfilesPayload {
        profiles = Map.copyOf(profiles);
        if (profiles.size() > MAX_ITEMS || (!available && (!profiles.isEmpty() || hydrationEnabled))) throw new IllegalArgumentException("Invalid tooltip snapshot size/state");
    }

    public FoodProfilesPayload(boolean available, Map<Identifier, TooltipProfile> profiles) {
        this(available, false, profiles);
    }

    private static void write(FriendlyByteBuf buffer, FoodProfilesPayload payload) {
        int start = buffer.writerIndex();
        buffer.writeBoolean(payload.available);
        buffer.writeBoolean(payload.hydrationEnabled);
        buffer.writeVarInt(payload.profiles.size());
        for (var entry : payload.profiles.entrySet()) {
            var profile = entry.getValue();
            buffer.writeIdentifier(entry.getKey());
            buffer.writeVarInt(profile.quality().ordinal());
            buffer.writeDouble(profile.recovery());
            buffer.writeVarInt(profile.categories().size());
            for (var category : profile.categories()) buffer.writeVarInt(category.ordinal());
            writeOptional(buffer, profile.benefit());
            buffer.writeIdentifier(profile.group());
            writeOptional(buffer, profile.profileId());
            buffer.writeVarInt(profile.hydration().thirst());
            buffer.writeVarInt(profile.hydration().quenched());
            buffer.writeBoolean(profile.consumptionSpeed().isPresent());
            profile.consumptionSpeed().ifPresent(speed -> buffer.writeVarInt(speed.ordinal()));
            if (buffer.writerIndex() - start > MAX_BYTES) throw new EncoderException("Tooltip snapshot exceeds byte limit");
        }
    }

    private static FoodProfilesPayload read(FriendlyByteBuf buffer) {
        boolean available = buffer.readBoolean();
        boolean hydrationEnabled = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ITEMS) throw new DecoderException("Invalid tooltip item count");
        var profiles = new HashMap<Identifier, TooltipProfile>();
        for (int i = 0; i < count; i++) {
            Identifier item = buffer.readIdentifier();
            FoodQuality quality = readEnum(buffer, FoodQuality.values());
            double recovery = buffer.readDouble();
            int categoryCount = buffer.readVarInt();
            if (categoryCount < 0 || categoryCount > DietCategory.values().length) throw new DecoderException("Invalid category count");
            var categories = new ArrayList<DietCategory>();
            for (int c = 0; c < categoryCount; c++) categories.add(readEnum(buffer, DietCategory.values()));
            var benefit = readOptional(buffer);
            var group = buffer.readIdentifier();
            var profileId = readOptional(buffer);
            var hydration = new Hydration(buffer.readVarInt(), buffer.readVarInt());
            Optional<ConsumptionSpeed> speed = buffer.readBoolean() ? Optional.of(readEnum(buffer, ConsumptionSpeed.values())) : Optional.empty();
            if (profiles.put(item, new TooltipProfile(quality, recovery, categories, benefit, group, profileId, hydration, speed)) != null) {
                throw new DecoderException("Duplicate tooltip item");
            }
        }
        return new FoodProfilesPayload(available, hydrationEnabled, profiles);
    }

    private static void writeOptional(FriendlyByteBuf buffer, Optional<Identifier> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeIdentifier);
    }

    private static <T> T readEnum(FriendlyByteBuf buffer, T[] values) {
        int ordinal = buffer.readVarInt();
        if (ordinal < 0 || ordinal >= values.length) throw new DecoderException("Invalid tooltip enum ordinal");
        return values[ordinal];
    }

    private static Optional<Identifier> readOptional(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readIdentifier()) : Optional.empty();
    }

    @Override
    public Type<FoodProfilesPayload> type() { return TYPE; }
}

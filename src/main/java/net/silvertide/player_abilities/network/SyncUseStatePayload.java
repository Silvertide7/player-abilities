package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SyncUseStatePayload(Optional<ResourceLocation> usingAbilityId, int level, int totalTicks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeOptional(usingAbilityId, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(level);
        buf.writeVarInt(totalTicks);
    }

    public static SyncUseStatePayload decode(FriendlyByteBuf buf) {
        return new SyncUseStatePayload(
                buf.readOptional(FriendlyByteBuf::readResourceLocation),
                buf.readVarInt(),
                buf.readVarInt());
    }
}

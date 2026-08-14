package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AbilityReadyPayload(ResourceLocation abilityId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
    }

    public static AbilityReadyPayload decode(FriendlyByteBuf buf) {
        return new AbilityReadyPayload(buf.readResourceLocation());
    }
}

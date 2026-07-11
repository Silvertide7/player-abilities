package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TogglePassivePayload(ResourceLocation abilityId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
    }

    public static TogglePassivePayload decode(FriendlyByteBuf buf) {
        return new TogglePassivePayload(buf.readResourceLocation());
    }
}

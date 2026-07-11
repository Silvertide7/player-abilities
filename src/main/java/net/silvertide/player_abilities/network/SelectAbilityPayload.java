package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SelectAbilityPayload(ResourceLocation abilityId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
    }

    public static SelectAbilityPayload decode(FriendlyByteBuf buf) {
        return new SelectAbilityPayload(buf.readResourceLocation());
    }
}

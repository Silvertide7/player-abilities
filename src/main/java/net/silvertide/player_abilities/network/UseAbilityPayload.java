package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;

public record UseAbilityPayload() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static UseAbilityPayload decode(FriendlyByteBuf buf) {
        return new UseAbilityPayload();
    }
}

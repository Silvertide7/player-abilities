package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.api.Cooldown;

public record SyncCooldownPayload(ResourceLocation abilityId, Cooldown cooldown) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
        cooldown.encode(buf);
    }

    public static SyncCooldownPayload decode(FriendlyByteBuf buf) {
        return new SyncCooldownPayload(buf.readResourceLocation(), Cooldown.decode(buf));
    }
}

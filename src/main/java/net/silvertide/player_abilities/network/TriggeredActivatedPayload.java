package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TriggeredActivatedPayload(ResourceLocation abilityId, int cooldownTicks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
        buf.writeVarInt(cooldownTicks);
    }

    public static TriggeredActivatedPayload decode(FriendlyByteBuf buf) {
        return new TriggeredActivatedPayload(buf.readResourceLocation(), buf.readVarInt());
    }
}

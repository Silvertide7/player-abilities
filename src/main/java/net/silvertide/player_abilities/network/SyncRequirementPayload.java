package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.data.RequirementProgress;

import java.util.Optional;

public record SyncRequirementPayload(ResourceLocation abilityId, Optional<RequirementProgress> progress) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
        buf.writeOptional(progress, (b, p) -> p.encode(b));
    }

    public static SyncRequirementPayload decode(FriendlyByteBuf buf) {
        return new SyncRequirementPayload(
                buf.readResourceLocation(),
                buf.readOptional(RequirementProgress::decode));
    }
}

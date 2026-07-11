package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.data.ActiveEffect;

import java.util.HashMap;
import java.util.Map;

public record SyncEffectsPayload(Map<ResourceLocation, ActiveEffect> effects) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(effects, FriendlyByteBuf::writeResourceLocation, (b, effect) -> effect.encode(b));
    }

    public static SyncEffectsPayload decode(FriendlyByteBuf buf) {
        return new SyncEffectsPayload(
                buf.readMap(HashMap::new, FriendlyByteBuf::readResourceLocation, ActiveEffect::decode));
    }
}

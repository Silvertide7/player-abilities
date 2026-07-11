package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.config.AbilityConfig;

import java.util.HashMap;
import java.util.Map;

public record SyncAbilityConfigsPayload(Map<ResourceLocation, AbilityConfig> configs) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(configs, FriendlyByteBuf::writeResourceLocation,
                (b, config) -> b.writeJsonWithCodec(AbilityConfig.CODEC, config));
    }

    public static SyncAbilityConfigsPayload decode(FriendlyByteBuf buf) {
        return new SyncAbilityConfigsPayload(
                buf.readMap(HashMap::new, FriendlyByteBuf::readResourceLocation,
                        b -> b.readJsonWithCodec(AbilityConfig.CODEC)));
    }
}

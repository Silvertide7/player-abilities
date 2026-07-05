package net.silvertide.player_abilities.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.config.AbilityConfig;

import java.util.HashMap;
import java.util.Map;

public record SyncAbilityConfigsPayload(Map<ResourceLocation, AbilityConfig> configs) implements CustomPacketPayload {
    public static final Type<SyncAbilityConfigsPayload> TYPE = new Type<>(PlayerAbilities.id("sync_ability_configs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAbilityConfigsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.fromCodec(AbilityConfig.CODEC)),
            SyncAbilityConfigsPayload::configs,
            SyncAbilityConfigsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

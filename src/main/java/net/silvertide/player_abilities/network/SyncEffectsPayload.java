package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.data.ActiveEffect;

import java.util.HashMap;
import java.util.Map;

public record SyncEffectsPayload(Map<ResourceLocation, ActiveEffect> effects) implements CustomPacketPayload {
    public static final Type<SyncEffectsPayload> TYPE = new Type<>(PlayerAbilities.id("sync_effects"));
    public static final StreamCodec<ByteBuf, SyncEffectsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ActiveEffect.STREAM_CODEC), SyncEffectsPayload::effects,
            SyncEffectsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

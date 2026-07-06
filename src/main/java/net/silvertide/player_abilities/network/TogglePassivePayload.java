package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

public record TogglePassivePayload(ResourceLocation abilityId) implements CustomPacketPayload {
    public static final Type<TogglePassivePayload> TYPE = new Type<>(PlayerAbilities.id("toggle_passive"));
    public static final StreamCodec<ByteBuf, TogglePassivePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TogglePassivePayload::abilityId,
            TogglePassivePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

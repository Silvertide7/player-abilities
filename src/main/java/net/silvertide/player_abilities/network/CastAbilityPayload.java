package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silvertide.player_abilities.PlayerAbilities;

public record CastAbilityPayload() implements CustomPacketPayload {
    public static final CastAbilityPayload INSTANCE = new CastAbilityPayload();
    public static final Type<CastAbilityPayload> TYPE = new Type<>(PlayerAbilities.id("cast_ability"));
    public static final StreamCodec<ByteBuf, CastAbilityPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

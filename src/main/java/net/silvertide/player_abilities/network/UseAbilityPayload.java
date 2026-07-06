package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silvertide.player_abilities.PlayerAbilities;

public record UseAbilityPayload() implements CustomPacketPayload {
    public static final UseAbilityPayload INSTANCE = new UseAbilityPayload();
    public static final Type<UseAbilityPayload> TYPE = new Type<>(PlayerAbilities.id("use_ability"));
    public static final StreamCodec<ByteBuf, UseAbilityPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

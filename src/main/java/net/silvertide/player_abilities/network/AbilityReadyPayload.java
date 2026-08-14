package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

public record AbilityReadyPayload(ResourceLocation abilityId) implements CustomPacketPayload {
    public static final Type<AbilityReadyPayload> TYPE = new Type<>(PlayerAbilities.id("ability_ready"));
    public static final StreamCodec<ByteBuf, AbilityReadyPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AbilityReadyPayload::abilityId,
            AbilityReadyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

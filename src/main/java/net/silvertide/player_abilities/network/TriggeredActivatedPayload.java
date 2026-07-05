package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

public record TriggeredActivatedPayload(ResourceLocation abilityId, int cooldownTicks) implements CustomPacketPayload {
    public static final Type<TriggeredActivatedPayload> TYPE = new Type<>(PlayerAbilities.id("triggered_activated"));
    public static final StreamCodec<ByteBuf, TriggeredActivatedPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TriggeredActivatedPayload::abilityId,
            ByteBufCodecs.VAR_INT, TriggeredActivatedPayload::cooldownTicks,
            TriggeredActivatedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

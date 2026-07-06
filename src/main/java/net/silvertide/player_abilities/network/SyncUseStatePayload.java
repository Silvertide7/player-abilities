package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.Optional;

public record SyncUseStatePayload(Optional<ResourceLocation> usingAbilityId, int level,
                                   int totalTicks) implements CustomPacketPayload {
    public static final Type<SyncUseStatePayload> TYPE = new Type<>(PlayerAbilities.id("sync_use_state"));
    public static final StreamCodec<ByteBuf, SyncUseStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SyncUseStatePayload::usingAbilityId,
            ByteBufCodecs.VAR_INT, SyncUseStatePayload::level,
            ByteBufCodecs.VAR_INT, SyncUseStatePayload::totalTicks,
            SyncUseStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

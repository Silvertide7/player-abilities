package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.Optional;

public record SyncCastStatePayload(Optional<ResourceLocation> castingAbilityId, int level,
                                   int totalTicks) implements CustomPacketPayload {
    public static final Type<SyncCastStatePayload> TYPE = new Type<>(PlayerAbilities.id("sync_cast_state"));
    public static final StreamCodec<ByteBuf, SyncCastStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SyncCastStatePayload::castingAbilityId,
            ByteBufCodecs.VAR_INT, SyncCastStatePayload::level,
            ByteBufCodecs.VAR_INT, SyncCastStatePayload::totalTicks,
            SyncCastStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

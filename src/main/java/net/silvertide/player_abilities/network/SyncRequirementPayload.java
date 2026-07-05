package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.data.RequirementProgress;

import java.util.Optional;

public record SyncRequirementPayload(ResourceLocation abilityId,
                                     Optional<RequirementProgress> progress) implements CustomPacketPayload {
    public static final Type<SyncRequirementPayload> TYPE = new Type<>(PlayerAbilities.id("sync_requirement"));
    public static final StreamCodec<ByteBuf, SyncRequirementPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SyncRequirementPayload::abilityId,
            ByteBufCodecs.optional(RequirementProgress.STREAM_CODEC), SyncRequirementPayload::progress,
            SyncRequirementPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record SyncAbilitiesPayload(int playerEntityId, Map<ResourceLocation, Integer> grantedLevels,
                                   Set<ResourceLocation> disabledPassiveIds,
                                   Optional<ResourceLocation> selectedId) implements CustomPacketPayload {
    public static final Type<SyncAbilitiesPayload> TYPE = new Type<>(PlayerAbilities.id("sync_abilities"));
    public static final StreamCodec<ByteBuf, SyncAbilitiesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncAbilitiesPayload::playerEntityId,
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), SyncAbilitiesPayload::grantedLevels,
            ByteBufCodecs.collection(HashSet::new, ResourceLocation.STREAM_CODEC), SyncAbilitiesPayload::disabledPassiveIds,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SyncAbilitiesPayload::selectedId,
            SyncAbilitiesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

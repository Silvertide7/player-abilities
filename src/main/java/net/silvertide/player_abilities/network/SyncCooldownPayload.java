package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Cooldown;

public record SyncCooldownPayload(ResourceLocation abilityId, Cooldown cooldown) implements CustomPacketPayload {
    public static final Type<SyncCooldownPayload> TYPE = new Type<>(PlayerAbilities.id("sync_cooldown"));
    public static final StreamCodec<ByteBuf, SyncCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SyncCooldownPayload::abilityId,
            Cooldown.STREAM_CODEC, SyncCooldownPayload::cooldown,
            SyncCooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

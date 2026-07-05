package net.silvertide.player_abilities.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

public record SelectAbilityPayload(ResourceLocation abilityId) implements CustomPacketPayload {
    public static final Type<SelectAbilityPayload> TYPE = new Type<>(PlayerAbilities.id("select_ability"));
    public static final StreamCodec<ByteBuf, SelectAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SelectAbilityPayload::abilityId,
            SelectAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package net.silvertide.player_abilities.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record AbilityBookContent(ResourceLocation abilityId, int level) {
    public static final Codec<AbilityBookContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("ability").forGetter(AbilityBookContent::abilityId),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("level", 1).forGetter(AbilityBookContent::level)
    ).apply(instance, AbilityBookContent::new));

    public static final StreamCodec<ByteBuf, AbilityBookContent> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AbilityBookContent::abilityId,
            ByteBufCodecs.VAR_INT, AbilityBookContent::level,
            AbilityBookContent::new);
}

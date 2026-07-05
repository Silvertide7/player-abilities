package net.silvertide.player_abilities.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Cooldown(int totalTicks, int remainingTicks) {
    public static final Codec<Cooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total").forGetter(Cooldown::totalTicks),
            Codec.INT.fieldOf("remaining").forGetter(Cooldown::remainingTicks)
    ).apply(instance, Cooldown::new));
    public static final StreamCodec<ByteBuf, Cooldown> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Cooldown::totalTicks,
            ByteBufCodecs.VAR_INT, Cooldown::remainingTicks,
            Cooldown::new);

    public Cooldown decremented() {
        return new Cooldown(totalTicks, remainingTicks - 1);
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }
}

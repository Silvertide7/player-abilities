package net.silvertide.player_abilities.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

public record Cooldown(int totalTicks, int remainingTicks) {
    public static final Codec<Cooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("total").forGetter(Cooldown::totalTicks),
            Codec.INT.fieldOf("remaining").forGetter(Cooldown::remainingTicks)
    ).apply(instance, Cooldown::new));
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(totalTicks);
        buf.writeVarInt(remainingTicks);
    }

    public static Cooldown decode(FriendlyByteBuf buf) {
        return new Cooldown(buf.readVarInt(), buf.readVarInt());
    }

    public Cooldown decremented() {
        return new Cooldown(totalTicks, remainingTicks - 1);
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }
}

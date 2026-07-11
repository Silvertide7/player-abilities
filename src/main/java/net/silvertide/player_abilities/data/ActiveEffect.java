package net.silvertide.player_abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public final class ActiveEffect implements net.silvertide.player_abilities.api.AbilityEffect {
    public static final Codec<ActiveEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(ActiveEffect::getLevel),
            Codec.INT.fieldOf("total").forGetter(ActiveEffect::getTotalTicks),
            Codec.INT.fieldOf("remaining").forGetter(ActiveEffect::getRemainingTicks)
    ).apply(instance, ActiveEffect::new));
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarInt(totalTicks);
        buf.writeVarInt(remainingTicks);
    }

    public static ActiveEffect decode(FriendlyByteBuf buf) {
        return new ActiveEffect(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    private final int level;
    private final int totalTicks;
    private int remainingTicks;
    @Nullable
    private Object effectData;

    public ActiveEffect(int level, int totalTicks, int remainingTicks) {
        this.level = level;
        this.totalTicks = totalTicks;
        this.remainingTicks = remainingTicks;
    }

    public int getLevel() {
        return level;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    void decrementRemaining() {
        remainingTicks--;
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    @Nullable
    public Object getEffectData() {
        return effectData;
    }

    void setEffectData(@Nullable Object effectData) {
        this.effectData = effectData;
    }
}

package net.silvertide.player_abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class RequirementProgress {
    public static final Codec<RequirementProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("kills").forGetter(RequirementProgress::getKills),
            Codec.FLOAT.fieldOf("damage_taken").forGetter(RequirementProgress::getDamageTaken)
    ).apply(instance, RequirementProgress::new));
    public static final StreamCodec<ByteBuf, RequirementProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequirementProgress::getKills,
            ByteBufCodecs.FLOAT, RequirementProgress::getDamageTaken,
            RequirementProgress::new);

    private int kills;
    private float damageTaken;

    public RequirementProgress(int kills, float damageTaken) {
        this.kills = kills;
        this.damageTaken = damageTaken;
    }

    public int getKills() {
        return kills;
    }

    public float getDamageTaken() {
        return damageTaken;
    }

    public void addKill() {
        kills++;
    }

    public void addDamageTaken(float amount) {
        damageTaken += amount;
    }

    public boolean meets(int requiredKills, float requiredDamageTaken) {
        return kills >= requiredKills && damageTaken >= requiredDamageTaken;
    }
}

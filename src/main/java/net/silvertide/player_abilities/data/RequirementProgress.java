package net.silvertide.player_abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

public final class RequirementProgress {
    public static final Codec<RequirementProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("kills").forGetter(RequirementProgress::getKills),
            Codec.FLOAT.fieldOf("damage_taken").forGetter(RequirementProgress::getDamageTaken)
    ).apply(instance, RequirementProgress::new));
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(kills);
        buf.writeFloat(damageTaken);
    }

    public static RequirementProgress decode(FriendlyByteBuf buf) {
        return new RequirementProgress(buf.readVarInt(), buf.readFloat());
    }

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

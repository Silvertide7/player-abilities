package net.silvertide.player_abilities.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record AbilityConfig(Optional<LeveledValue<Integer>> cooldownTicks,
                            Optional<LeveledValue<Integer>> killRequirement,
                            Optional<LeveledValue<Float>> damageTakenRequirement,
                            Optional<Integer> maxLevel,
                            Optional<ResourceLocation> category,
                            Optional<PmmoUseRequirement> pmmoUseRequirement,
                            Optional<List<PmmoGrant>> pmmoGrants,
                            Optional<List<PuffishGrant>> puffishGrants) {

    public record PmmoUseRequirement(String skill, LeveledValue<Integer> level) {
        public static final Codec<PmmoUseRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("skill").forGetter(PmmoUseRequirement::skill),
                LeveledValue.codec(Codec.INT).fieldOf("level").forGetter(PmmoUseRequirement::level)
        ).apply(instance, PmmoUseRequirement::new));
    }

    public record PmmoGrant(String skill, int pmmoLevel, int abilityLevel) {
        public static final Codec<PmmoGrant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("skill").forGetter(PmmoGrant::skill),
                Codec.INT.fieldOf("level").forGetter(PmmoGrant::pmmoLevel),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("ability_level", 1).forGetter(PmmoGrant::abilityLevel)
        ).apply(instance, PmmoGrant::new));
    }

    public record PuffishGrant(ResourceLocation category, String skill, int abilityLevel) {
        public static final Codec<PuffishGrant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("category").forGetter(PuffishGrant::category),
                Codec.STRING.fieldOf("skill").forGetter(PuffishGrant::skill),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("ability_level", 1).forGetter(PuffishGrant::abilityLevel)
        ).apply(instance, PuffishGrant::new));
    }

    public static final Codec<AbilityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LeveledValue.codec(Codec.INT).optionalFieldOf("cooldown_ticks").forGetter(AbilityConfig::cooldownTicks),
            LeveledValue.codec(Codec.INT).optionalFieldOf("kill_requirement").forGetter(AbilityConfig::killRequirement),
            LeveledValue.codec(Codec.FLOAT).optionalFieldOf("damage_taken_requirement").forGetter(AbilityConfig::damageTakenRequirement),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_level").forGetter(AbilityConfig::maxLevel),
            ResourceLocation.CODEC.optionalFieldOf("category").forGetter(AbilityConfig::category),
            PmmoUseRequirement.CODEC.optionalFieldOf("pmmo_use_requirement").forGetter(AbilityConfig::pmmoUseRequirement),
            PmmoGrant.CODEC.listOf().optionalFieldOf("pmmo_grants").forGetter(AbilityConfig::pmmoGrants),
            PuffishGrant.CODEC.listOf().optionalFieldOf("puffish_grants").forGetter(AbilityConfig::puffishGrants)
    ).apply(instance, AbilityConfig::new));
}

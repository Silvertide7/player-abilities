package net.silvertide.player_abilities.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.silvertide.player_abilities.api.AttributeGrant;
import net.silvertide.player_abilities.api.EffectGrant;

import java.util.List;
import java.util.Optional;

public record AbilityConfig(Optional<Boolean> enabled,
                            Optional<LeveledValue<Integer>> cooldownTicks,
                            Optional<LeveledValue<Integer>> killRequirement,
                            Optional<LeveledValue<Float>> damageTakenRequirement,
                            Optional<LeveledValue<Integer>> useTicks,
                            Optional<LeveledValue<Integer>> effectDurationTicks,
                            Optional<Integer> maxLevel,
                            Optional<ResourceLocation> category,
                            Optional<PmmoUseRequirement> pmmoUseRequirement,
                            Optional<List<PmmoGrant>> pmmoGrants,
                            Optional<List<PuffishGrant>> puffishGrants,
                            Optional<List<AttributeGrantConfig>> attributeGrants,
                            Optional<List<EffectGrantConfig>> effectGrants) {

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

    public record AttributeGrantConfig(Holder<Attribute> attribute, LeveledValue<Double> amount,
                                       AttributeModifier.Operation operation) {
        public static final Codec<AttributeGrantConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(AttributeGrantConfig::attribute),
                LeveledValue.codec(Codec.DOUBLE).fieldOf("amount").forGetter(AttributeGrantConfig::amount),
                AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(AttributeGrantConfig::operation)
        ).apply(instance, AttributeGrantConfig::new));

        public AttributeGrant resolve(int level) {
            return new AttributeGrant(attribute, amount.resolve(level), operation);
        }
    }

    public record EffectGrantConfig(Holder<MobEffect> effect, LeveledValue<Integer> durationTicks,
                                    LeveledValue<Integer> amplifier) {
        private static final LeveledValue<Integer> NO_AMPLIFIER = new LeveledValue<>(List.of(0));

        public static final Codec<EffectGrantConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(EffectGrantConfig::effect),
                LeveledValue.codec(Codec.INT).fieldOf("duration_ticks").forGetter(EffectGrantConfig::durationTicks),
                LeveledValue.codec(Codec.INT).optionalFieldOf("amplifier", NO_AMPLIFIER).forGetter(EffectGrantConfig::amplifier)
        ).apply(instance, EffectGrantConfig::new));

        public EffectGrant resolve(int level) {
            return new EffectGrant(effect, durationTicks.resolve(level), amplifier.resolve(level));
        }
    }

    public static final Codec<AbilityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled").forGetter(AbilityConfig::enabled),
            LeveledValue.codec(Codec.INT).optionalFieldOf("cooldown_ticks").forGetter(AbilityConfig::cooldownTicks),
            LeveledValue.codec(Codec.INT).optionalFieldOf("kill_requirement").forGetter(AbilityConfig::killRequirement),
            LeveledValue.codec(Codec.FLOAT).optionalFieldOf("damage_taken_requirement").forGetter(AbilityConfig::damageTakenRequirement),
            LeveledValue.codec(Codec.INT).optionalFieldOf("use_ticks").forGetter(AbilityConfig::useTicks),
            LeveledValue.codec(Codec.INT).optionalFieldOf("effect_duration_ticks").forGetter(AbilityConfig::effectDurationTicks),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_level").forGetter(AbilityConfig::maxLevel),
            ResourceLocation.CODEC.optionalFieldOf("category").forGetter(AbilityConfig::category),
            PmmoUseRequirement.CODEC.optionalFieldOf("pmmo_use_requirement").forGetter(AbilityConfig::pmmoUseRequirement),
            PmmoGrant.CODEC.listOf().optionalFieldOf("pmmo_grants").forGetter(AbilityConfig::pmmoGrants),
            PuffishGrant.CODEC.listOf().optionalFieldOf("puffish_grants").forGetter(AbilityConfig::puffishGrants),
            AttributeGrantConfig.CODEC.listOf().optionalFieldOf("attribute_grants").forGetter(AbilityConfig::attributeGrants),
            EffectGrantConfig.CODEC.listOf().optionalFieldOf("effect_grants").forGetter(AbilityConfig::effectGrants)
    ).apply(instance, AbilityConfig::new));
}

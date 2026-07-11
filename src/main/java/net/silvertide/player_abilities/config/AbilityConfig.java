package net.silvertide.player_abilities.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraftforge.registries.ForgeRegistries;
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

    public static final Codec<AttributeModifier.Operation> OPERATION_CODEC = Codec.STRING.comapFlatMap(
            name -> switch (name) {
                case "add_value", "addition" -> DataResult.success(AttributeModifier.Operation.ADDITION);
                case "add_multiplied_base", "multiply_base" -> DataResult.success(AttributeModifier.Operation.MULTIPLY_BASE);
                case "add_multiplied_total", "multiply_total" -> DataResult.success(AttributeModifier.Operation.MULTIPLY_TOTAL);
                default -> DataResult.error(() -> "Unknown attribute modifier operation " + name);
            },
            operation -> switch (operation) {
                case ADDITION -> "add_value";
                case MULTIPLY_BASE -> "add_multiplied_base";
                case MULTIPLY_TOTAL -> "add_multiplied_total";
            });

    public record AttributeGrantConfig(Attribute attribute, LeveledValue<Double> amount,
                                       AttributeModifier.Operation operation) {
        public static final Codec<AttributeGrantConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ForgeRegistries.ATTRIBUTES.getCodec().fieldOf("attribute").forGetter(AttributeGrantConfig::attribute),
                LeveledValue.codec(Codec.DOUBLE).fieldOf("amount").forGetter(AttributeGrantConfig::amount),
                OPERATION_CODEC.fieldOf("operation").forGetter(AttributeGrantConfig::operation)
        ).apply(instance, AttributeGrantConfig::new));

        public AttributeGrant resolve(int level) {
            return new AttributeGrant(attribute, amount.resolve(level), operation);
        }
    }

    public record EffectGrantConfig(MobEffect effect, LeveledValue<Integer> durationTicks,
                                    LeveledValue<Integer> amplifier, boolean showParticles, boolean showIcon) {
        private static final LeveledValue<Integer> NO_AMPLIFIER = new LeveledValue<>(List.of(0));

        public static final Codec<EffectGrantConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ForgeRegistries.MOB_EFFECTS.getCodec().fieldOf("effect").forGetter(EffectGrantConfig::effect),
                LeveledValue.codec(Codec.INT).fieldOf("duration_ticks").forGetter(EffectGrantConfig::durationTicks),
                LeveledValue.codec(Codec.INT).optionalFieldOf("amplifier", NO_AMPLIFIER).forGetter(EffectGrantConfig::amplifier),
                Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(EffectGrantConfig::showParticles),
                Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(EffectGrantConfig::showIcon)
        ).apply(instance, EffectGrantConfig::new));

        public EffectGrant resolve(int level) {
            return new EffectGrant(effect, durationTicks.resolve(level), amplifier.resolve(level), showParticles, showIcon);
        }
    }

    private static <T> Codec<T> logged(String fieldName, Codec<T> codec) {
        return new Codec<>() {
            @Override
            public <D> com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<T, D>> decode(com.mojang.serialization.DynamicOps<D> ops, D input) {
                var result = codec.decode(ops, input);
                result.error().ifPresent(error -> net.silvertide.player_abilities.PlayerAbilities.LOGGER.warn(
                        "player_abilities config field '{}' is invalid and falls back to the ability default: {}", fieldName, error.message()));
                return result;
            }

            @Override
            public <D> com.mojang.serialization.DataResult<D> encode(T input, com.mojang.serialization.DynamicOps<D> ops, D prefix) {
                return codec.encode(input, ops, prefix);
            }
        };
    }

    public static final Codec<AbilityConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            logged("enabled", Codec.BOOL).optionalFieldOf("enabled").forGetter(AbilityConfig::enabled),
            logged("cooldown_ticks", LeveledValue.codec(Codec.INT)).optionalFieldOf("cooldown_ticks").forGetter(AbilityConfig::cooldownTicks),
            logged("kill_requirement", LeveledValue.codec(Codec.INT)).optionalFieldOf("kill_requirement").forGetter(AbilityConfig::killRequirement),
            logged("damage_taken_requirement", LeveledValue.codec(Codec.FLOAT)).optionalFieldOf("damage_taken_requirement").forGetter(AbilityConfig::damageTakenRequirement),
            logged("use_ticks", LeveledValue.codec(Codec.INT)).optionalFieldOf("use_ticks").forGetter(AbilityConfig::useTicks),
            logged("effect_duration_ticks", LeveledValue.codec(Codec.INT)).optionalFieldOf("effect_duration_ticks").forGetter(AbilityConfig::effectDurationTicks),
            logged("max_level", Codec.intRange(1, Integer.MAX_VALUE)).optionalFieldOf("max_level").forGetter(AbilityConfig::maxLevel),
            logged("category", ResourceLocation.CODEC).optionalFieldOf("category").forGetter(AbilityConfig::category),
            logged("pmmo_use_requirement", PmmoUseRequirement.CODEC).optionalFieldOf("pmmo_use_requirement").forGetter(AbilityConfig::pmmoUseRequirement),
            logged("pmmo_grants", PmmoGrant.CODEC.listOf()).optionalFieldOf("pmmo_grants").forGetter(AbilityConfig::pmmoGrants),
            logged("puffish_grants", PuffishGrant.CODEC.listOf()).optionalFieldOf("puffish_grants").forGetter(AbilityConfig::puffishGrants),
            logged("attribute_grants", AttributeGrantConfig.CODEC.listOf()).optionalFieldOf("attribute_grants").forGetter(AbilityConfig::attributeGrants),
            logged("effect_grants", EffectGrantConfig.CODEC.listOf()).optionalFieldOf("effect_grants").forGetter(AbilityConfig::effectGrants)
    ).apply(instance, AbilityConfig::new));
}

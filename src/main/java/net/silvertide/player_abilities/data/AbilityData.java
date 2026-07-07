package net.silvertide.player_abilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.Cooldown;
import net.silvertide.player_abilities.api.GatedAbility;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.TriggeredAbility;
import net.silvertide.player_abilities.config.AbilityConfigs;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AbilityData {
    public static final ResourceLocation CLIENT_SYNCED_SOURCE = PlayerAbilities.id("client_synced");

    public static final Codec<AbilityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT))
                    .optionalFieldOf("leveled_grants", Map.of())
                    .forGetter(AbilityData::grantsAsIds),
            Codec.unboundedMap(ResourceLocation.CODEC, Cooldown.CODEC)
                    .optionalFieldOf("cooldowns", Map.of())
                    .forGetter(AbilityData::cooldownsAsIds),
            Codec.unboundedMap(ResourceLocation.CODEC, ActiveEffect.CODEC)
                    .optionalFieldOf("effects", Map.of())
                    .forGetter(AbilityData::effectsAsIds),
            Codec.unboundedMap(ResourceLocation.CODEC, RequirementProgress.CODEC)
                    .optionalFieldOf("requirements", Map.of())
                    .forGetter(AbilityData::requirementsAsIds),
            ResourceLocation.CODEC.listOf().optionalFieldOf("disabled_passives", List.of())
                    .forGetter(AbilityData::disabledPassivesAsIds),
            ResourceLocation.CODEC.optionalFieldOf("selected")
                    .forGetter(abilityData -> abilityData.getSelected().map(Ability::getId))
    ).apply(instance, AbilityData::fromSerialized));

    private final Map<ResourceLocation, Map<Ability, Integer>> grants = new HashMap<>();
    private final Map<GatedAbility, Cooldown> cooldowns = new HashMap<>();
    private final Map<Ability, ActiveEffect> activeEffects = new HashMap<>();
    private final Map<GatedAbility, RequirementProgress> requirementProgress = new HashMap<>();
    private final Set<PassiveAbility> disabledPassives = new HashSet<>();
    @Nullable
    private List<TriggeredGrant> cachedTriggeredGrants;
    private int cachedTriggeredGeneration = -1;
    @Nullable
    private ActiveAbility selected;

    public record TriggeredGrant(TriggeredAbility<?> ability, int level) {
    }
    @Nullable
    private ActiveUse activeUse;
    @Nullable
    private Object pendingUseData;

    private static AbilityData fromSerialized(Map<ResourceLocation, Map<ResourceLocation, Integer>> grantLevelsBySource,
                                              Map<ResourceLocation, Cooldown> cooldownsByAbilityId,
                                              Map<ResourceLocation, ActiveEffect> effectsByAbilityId,
                                              Map<ResourceLocation, RequirementProgress> requirementsByAbilityId,
                                              List<ResourceLocation> disabledPassiveIds,
                                              Optional<ResourceLocation> selectedId) {
        AbilityData abilityData = new AbilityData();
        grantLevelsBySource.forEach((source, abilityLevels) -> abilityLevels.forEach((abilityId, level) -> {
            Ability ability = AbilityRegistry.ABILITIES.get(abilityId);
            if (ability == null) {
                PlayerAbilities.LOGGER.warn("Dropping grant of unknown ability {} from source {}", abilityId, source);
            } else {
                abilityData.setGrant(source, ability, level);
            }
        }));
        cooldownsByAbilityId.forEach((abilityId, cooldown) ->
                AbilityRegistry.getGated(abilityId).ifPresent(gated -> abilityData.cooldowns.put(gated, cooldown)));
        effectsByAbilityId.forEach((abilityId, effect) -> {
            Ability ability = AbilityRegistry.ABILITIES.get(abilityId);
            if (ability != null) {
                abilityData.activeEffects.put(ability, effect);
            }
        });
        requirementsByAbilityId.forEach((abilityId, progress) ->
                AbilityRegistry.getGated(abilityId).ifPresent(gated -> abilityData.requirementProgress.put(gated, progress)));
        disabledPassiveIds.forEach(abilityId ->
                AbilityRegistry.getPassive(abilityId).ifPresent(abilityData.disabledPassives::add));
        selectedId.flatMap(AbilityRegistry::getActive).ifPresent(active -> abilityData.selected = active);
        return abilityData;
    }

    private Map<ResourceLocation, Map<ResourceLocation, Integer>> grantsAsIds() {
        return grants.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                        abilityLevel -> abilityLevel.getKey().getId(),
                        Map.Entry::getValue))));
    }

    private Map<ResourceLocation, Cooldown> cooldownsAsIds() {
        return cooldowns.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().getId(),
                Map.Entry::getValue));
    }

    private Map<ResourceLocation, ActiveEffect> effectsAsIds() {
        return activeEffects.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().getId(),
                Map.Entry::getValue));
    }

    private Map<ResourceLocation, RequirementProgress> requirementsAsIds() {
        return requirementProgress.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().getId(),
                Map.Entry::getValue));
    }

    private List<ResourceLocation> disabledPassivesAsIds() {
        return disabledPassives.stream().map(Ability::getId).toList();
    }

    public boolean isPassiveDisabled(PassiveAbility passive) {
        return disabledPassives.contains(passive);
    }

    public boolean setPassiveDisabled(PassiveAbility passive, boolean disabled) {
        return disabled ? disabledPassives.add(passive) : disabledPassives.remove(passive);
    }

    public Set<PassiveAbility> getDisabledPassives() {
        return Collections.unmodifiableSet(disabledPassives);
    }

    public int setGrant(ResourceLocation source, Ability ability, int level) {
        cachedTriggeredGrants = null;
        Integer previousLevel = grants.computeIfAbsent(source, key -> new HashMap<>()).put(ability, level);
        return previousLevel == null ? 0 : previousLevel;
    }

    public boolean removeGrant(ResourceLocation source, Ability ability) {
        Map<Ability, Integer> abilityLevels = grants.get(source);
        if (abilityLevels == null || abilityLevels.remove(ability) == null) {
            return false;
        }
        cachedTriggeredGrants = null;
        if (abilityLevels.isEmpty()) {
            grants.remove(source);
        }
        return true;
    }

    public int getEffectiveLevel(Ability ability) {
        if (!AbilityConfigs.isEnabled(ability)) {
            return 0;
        }
        int rawLevel = getEffectiveLevelIgnoringDisabled(ability);
        return rawLevel == 0 ? 0 : Math.min(rawLevel, AbilityConfigs.maxLevel(ability));
    }

    public int getEffectiveLevelIgnoringDisabled(Ability ability) {
        int effectiveLevel = 0;
        for (Map<Ability, Integer> abilityLevels : grants.values()) {
            Integer level = abilityLevels.get(ability);
            if (level != null && level > effectiveLevel) {
                effectiveLevel = level;
            }
        }
        return effectiveLevel;
    }

    public boolean isGranted(Ability ability) {
        return getEffectiveLevel(ability) > 0;
    }

    public Map<Ability, Integer> getGrantedLevels() {
        Map<Ability, Integer> effectiveLevels = new HashMap<>();
        grants.values().forEach(abilityLevels -> abilityLevels.forEach((ability, level) -> {
            if (AbilityConfigs.isEnabled(ability)) {
                effectiveLevels.merge(ability, Math.min(level, AbilityConfigs.maxLevel(ability)), Math::max);
            }
        }));
        return effectiveLevels;
    }

    public List<TriggeredGrant> getTriggeredGrants() {
        int generation = AbilityConfigs.generation();
        List<TriggeredGrant> cached = cachedTriggeredGrants;
        if (cached == null || cachedTriggeredGeneration != generation) {
            cached = getGrantedLevels().entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof TriggeredAbility<?>)
                    .map(entry -> new TriggeredGrant((TriggeredAbility<?>) entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(grant -> grant.ability().getId().toString()))
                    .toList();
            cachedTriggeredGrants = cached;
            cachedTriggeredGeneration = generation;
        }
        return cached;
    }

    public Set<Ability> getGranted() {
        return grants.values().stream()
                .flatMap(abilityLevels -> abilityLevels.keySet().stream())
                .filter(AbilityConfigs::isEnabled)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<ActiveAbility> getGrantedActives() {
        return getGranted().stream()
                .filter(ActiveAbility.class::isInstance)
                .map(ActiveAbility.class::cast)
                .sorted(Comparator.comparing(active -> active.getId().toString()))
                .toList();
    }

    public Map<ResourceLocation, Map<Ability, Integer>> getGrantsBySource() {
        Map<ResourceLocation, Map<Ability, Integer>> grantsBySource = new HashMap<>();
        grants.forEach((source, abilityLevels) -> grantsBySource.put(source, Map.copyOf(abilityLevels)));
        return grantsBySource;
    }

    public Optional<ActiveAbility> getSelected() {
        return Optional.ofNullable(selected);
    }

    public void setSelected(@Nullable ActiveAbility ability) {
        this.selected = ability;
    }

    public Optional<ActiveEffect> getEffect(Ability ability) {
        return Optional.ofNullable(activeEffects.get(ability));
    }

    public void putEffect(Ability ability, ActiveEffect effect) {
        activeEffects.put(ability, effect);
    }

    public void removeEffect(Ability ability) {
        activeEffects.remove(ability);
    }

    public Map<Ability, ActiveEffect> getActiveEffects() {
        return Collections.unmodifiableMap(activeEffects);
    }

    public boolean isCurrentEffect(Ability ability, ActiveEffect effect) {
        return activeEffects.get(ability) == effect;
    }

    public void replaceSyncedEffects(Map<Ability, ActiveEffect> effects) {
        activeEffects.clear();
        activeEffects.putAll(effects);
    }

    public void tickEffectsClient() {
        if (activeEffects.isEmpty()) {
            return;
        }
        activeEffects.values().forEach(ActiveEffect::decrementRemaining);
        activeEffects.values().removeIf(ActiveEffect::isExpired);
    }

    public Optional<RequirementProgress> getRequirementProgress(GatedAbility ability) {
        return Optional.ofNullable(requirementProgress.get(ability));
    }

    public void putRequirementProgress(GatedAbility ability, RequirementProgress progress) {
        requirementProgress.put(ability, progress);
    }

    public void removeRequirementProgress(GatedAbility ability) {
        requirementProgress.remove(ability);
    }

    public Map<GatedAbility, RequirementProgress> getRequirementProgressMap() {
        return Collections.unmodifiableMap(requirementProgress);
    }

    public boolean hasUnmetRequirements(GatedAbility ability) {
        RequirementProgress progress = requirementProgress.get(ability);
        if (progress == null) {
            return false;
        }
        int level = getEffectiveLevel(ability);
        return !progress.meets(AbilityConfigs.killRequirement(ability, level),
                AbilityConfigs.damageTakenRequirement(ability, level));
    }

    public Optional<ActiveUse> getActiveUse() {
        return Optional.ofNullable(activeUse);
    }

    public void startUse(ActiveAbility ability, int level, int totalTicks, @Nullable Vec3 startPosition, int hurtTimeBaseline) {
        activeUse = new ActiveUse(ability, level, totalTicks, startPosition);
        activeUse.setLastHurtTime(hurtTimeBaseline);
        if (pendingUseData != null) {
            activeUse.setUseData(pendingUseData);
            pendingUseData = null;
        }
    }

    public void clearPendingUseData() {
        pendingUseData = null;
    }

    @Nullable
    public Object getUseData() {
        return activeUse != null ? activeUse.getUseData() : pendingUseData;
    }

    public void markUseCompleting() {
        if (activeUse != null) {
            activeUse.markCompleting();
        }
    }

    public void incrementUseElapsed() {
        if (activeUse != null) {
            activeUse.incrementElapsed();
        }
    }

    public void recordUseHurtBaseline(int hurtTime) {
        if (activeUse != null) {
            activeUse.setLastHurtTime(hurtTime);
        }
    }

    public void setUseData(@Nullable Object useData) {
        if (activeUse != null) {
            activeUse.setUseData(useData);
        } else {
            pendingUseData = useData;
        }
    }

    public void setEffectData(Ability ability, @Nullable Object effectData) {
        ActiveEffect effect = activeEffects.get(ability);
        if (effect != null) {
            effect.setEffectData(effectData);
        }
    }

    public void carryEffectData(ActiveEffect from, ActiveEffect to) {
        to.setEffectData(from.getEffectData());
    }

    public void decrementEffect(ActiveEffect effect) {
        effect.decrementRemaining();
    }

    public void clearUse() {
        activeUse = null;
    }

    public boolean isOnCooldown(GatedAbility ability) {
        return cooldowns.containsKey(ability);
    }

    public Optional<Cooldown> getCooldown(GatedAbility ability) {
        return Optional.ofNullable(cooldowns.get(ability));
    }

    public void setCooldown(GatedAbility ability, Cooldown cooldown) {
        cooldowns.put(ability, cooldown);
    }

    public void removeCooldown(GatedAbility ability) {
        cooldowns.remove(ability);
    }

    public Map<GatedAbility, Cooldown> getCooldowns() {
        return Map.copyOf(cooldowns);
    }

    public void tickCooldowns() {
        if (cooldowns.isEmpty()) {
            return;
        }
        cooldowns.replaceAll((ability, cooldown) -> cooldown.decremented());
        cooldowns.values().removeIf(Cooldown::isExpired);
    }

    public void replaceSyncedState(Map<Ability, Integer> grantedLevels, Set<PassiveAbility> newDisabledPassives,
                                   @Nullable ActiveAbility newSelected) {
        cachedTriggeredGrants = null;
        grants.clear();
        if (!grantedLevels.isEmpty()) {
            grants.put(CLIENT_SYNCED_SOURCE, new HashMap<>(grantedLevels));
        }
        disabledPassives.clear();
        disabledPassives.addAll(newDisabledPassives);
        selected = newSelected;
    }
}

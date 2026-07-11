package net.silvertide.player_abilities.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.event.AbilityInterruptedEvent;
import net.silvertide.player_abilities.api.event.AbilityGrantedEvent;
import net.silvertide.player_abilities.api.event.AbilityPerformEvent;
import net.silvertide.player_abilities.api.event.AbilityPerformedEvent;
import net.silvertide.player_abilities.api.event.AbilityRevokedEvent;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityCapability;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.data.ActiveUse;
import net.silvertide.player_abilities.data.ActiveEffect;
import net.silvertide.player_abilities.data.RequirementProgress;
import net.silvertide.player_abilities.network.AbilitySync;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class AbilityAPI {
    public static final int MIN_LEVEL = 1;
    private static final double STATIONARY_USE_MAX_DRIFT_SQ = 0.25;

    private AbilityAPI() {
    }

    public static boolean grant(ServerPlayer player, ResourceLocation source, Ability ability) {
        return grant(player, source, ability, MIN_LEVEL);
    }

    public static boolean grant(ServerPlayer player, ResourceLocation source, Ability ability, int level) {
        int maxLevel = AbilityConfigs.maxLevel(ability);
        int grantedLevel = Mth.clamp(level, MIN_LEVEL, maxLevel);
        AbilityData abilityData = getData(player);
        int effectiveBefore = abilityData.getEffectiveLevel(ability);
        int previousSourceLevel = abilityData.setGrant(source, ability, grantedLevel);
        if (previousSourceLevel == grantedLevel) {
            return false;
        }
        if (grantedLevel != level) {
            PlayerAbilities.LOGGER.warn("Grant of {} requested level {} outside [{}, {}]; clamping to {}",
                    ability.getId(), level, MIN_LEVEL, maxLevel, grantedLevel);
        }
        int effectiveAfter = abilityData.getEffectiveLevel(ability);
        applyEffectiveLevelChange(player, abilityData, ability, effectiveBefore, effectiveAfter);
        if (effectiveBefore == 0 && effectiveAfter > 0) {
            MinecraftForge.EVENT_BUS.post(new AbilityGrantedEvent(player, ability, effectiveAfter, source));
        }
        return true;
    }

    public static boolean revoke(ServerPlayer player, ResourceLocation source, Ability ability) {
        AbilityData abilityData = getData(player);
        int effectiveBefore = abilityData.getEffectiveLevel(ability);
        if (!abilityData.removeGrant(source, ability)) {
            return false;
        }
        int effectiveAfter = abilityData.getEffectiveLevel(ability);
        applyEffectiveLevelChange(player, abilityData, ability, effectiveBefore, effectiveAfter);
        if (effectiveBefore > 0 && effectiveAfter == 0) {
            MinecraftForge.EVENT_BUS.post(new AbilityRevokedEvent(player, ability, effectiveBefore, source));
        }
        return true;
    }

    private static void applyEffectiveLevelChange(ServerPlayer player, AbilityData abilityData, Ability ability,
                                                  int effectiveBefore, int effectiveAfter) {
        if (effectiveBefore == effectiveAfter) {
            return;
        }
        if (ability instanceof PassiveAbility passive && !abilityData.isPassiveDisabled(passive)) {
            if (effectiveBefore > 0) {
                deactivatePassive(player, passive, effectiveBefore);
            }
            if (effectiveAfter > 0) {
                activatePassive(player, passive, effectiveAfter);
            }
        }
        if (ability instanceof ActiveAbility active) {
            if (abilityData.getActiveUse().filter(use -> use.getAbility().equals(active)).isPresent()) {
                cancelUse(player);
            }
            if (effectiveAfter == 0 && abilityData.getSelected().filter(active::equals).isPresent()) {
                abilityData.setSelected(null);
            }
        }
        if (ability instanceof GatedAbility gated && effectiveAfter == 0) {
            if (abilityData.isOnCooldown(gated)) {
                abilityData.removeCooldown(gated);
                AbilitySync.syncCooldown(player, gated, new Cooldown(0, 0));
            }
            if (abilityData.getRequirementProgress(gated).isPresent()) {
                abilityData.removeRequirementProgress(gated);
                AbilitySync.syncRequirementProgress(player, gated, null);
            }
        }
        AbilitySync.syncAbilities(player);
    }

    public static boolean use(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        Optional<ActiveUse> currentUse = abilityData.getActiveUse();
        if (currentUse.isPresent()) {
            if (currentUse.get().getAbility().getUseType() == AbilityUseType.CHANNELED) {
                finishUse(player);
            } else {
                cancelUse(player);
            }
            return false;
        }
        Optional<ActiveAbility> selectedAbility = abilityData.getSelected();
        if (selectedAbility.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.player_abilities.no_ability_selected"), true);
            return false;
        }
        ActiveAbility ability = selectedAbility.get();
        int level = abilityData.getEffectiveLevel(ability);
        if (level == 0) {
            abilityData.setSelected(null);
            AbilitySync.syncAbilities(player);
            player.displayClientMessage(Component.translatable("message.player_abilities.no_ability_selected"), true);
            return false;
        }
        abilityData.clearPendingUseData();
        Component abilityName = Component.translatable(ability.getDescriptionId());
        if (!player.isCreative() && abilityData.isOnCooldown(ability)) {
            player.displayClientMessage(Component.translatable("message.player_abilities.on_cooldown", abilityName), true);
            return false;
        }
        Optional<RequirementProgress> pendingRequirements = abilityData.getRequirementProgress(ability);
        if (!player.isCreative() && pendingRequirements.isPresent()
                && !pendingRequirements.get().meets(AbilityConfigs.killRequirement(ability, level),
                AbilityConfigs.damageTakenRequirement(ability, level))) {
            player.displayClientMessage(unmetRequirementMessage(ability, level, pendingRequirements.get()), true);
            return false;
        }
        if (!ability.canUse(player, level)) {
            Component failureMessage = ability.getUseFailureMessage(player, level);
            player.displayClientMessage(failureMessage != null ? failureMessage
                    : Component.translatable("message.player_abilities.cannot_use", abilityName), true);
            return false;
        }
        if (MinecraftForge.EVENT_BUS.post(new AbilityPerformEvent(player, ability, level))) {
            return false;
        }
        if (!abilityData.isGranted(ability)) {
            return false;
        }
        if (ability.getUseType() == AbilityUseType.INSTANT) {
            ability.onUse(player, level);
            applyDeclaredEffects(player, ability, level);
            if (!abilityData.isOnCooldown(ability)) {
                applyCompletionCooldown(player, abilityData, ability, level);
            }
            resetRequirementProgress(player, abilityData, ability, level);
            MinecraftForge.EVENT_BUS.post(new AbilityPerformedEvent(player, ability, level));
            return true;
        }
        int useTicks = AbilityConfigs.useTicks(ability, level);
        ability.onUseStart(player, level);
        abilityData.startUse(ability, level, useTicks, player.position(), player.hurtTime);
        AbilitySync.syncUseState(player, ability, level, useTicks);
        if (useTicks <= 0) {
            finishUse(player);
        }
        return true;
    }

    public static void serverTick(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        tickActiveUse(player, abilityData);
        tickEffects(player, abilityData);
    }

    private static void tickActiveUse(ServerPlayer player, AbilityData abilityData) {
        Optional<ActiveUse> activeUse = abilityData.getActiveUse();
        if (activeUse.isEmpty()) {
            return;
        }
        ActiveUse use = activeUse.get();
        ActiveAbility ability = use.getAbility();
        if (ability.isInterruptedByDamage()) {
            if (player.hurtTime > use.getLastHurtTime()) {
                cancelUse(player);
                return;
            }
            abilityData.recordUseHurtBaseline(player.hurtTime);
        }
        if (ability.requiresStationary() && use.getStartPosition() != null
                && player.position().distanceToSqr(use.getStartPosition()) > STATIONARY_USE_MAX_DRIFT_SQ) {
            cancelUse(player);
            return;
        }
        abilityData.incrementUseElapsed();
        ability.onUseTick(player, use.getLevel(), use.getElapsedTicks(), use.getTotalTicks());
        if (abilityData.getActiveUse().filter(current -> current == use && !current.isCompleting()).isEmpty()) {
            return;
        }
        if (use.getElapsedTicks() >= use.getTotalTicks()) {
            finishUse(player);
        }
    }

    public static void finishUse(ServerPlayer player) {
        completeUse(player, false);
    }

    public static void cancelUse(ServerPlayer player) {
        completeUse(player, true);
    }

    private static void completeUse(ServerPlayer player, boolean cancelled) {
        AbilityData abilityData = getData(player);
        Optional<ActiveUse> activeUse = abilityData.getActiveUse();
        if (activeUse.isEmpty() || activeUse.get().isCompleting()) {
            return;
        }
        ActiveUse use = activeUse.get();
        abilityData.markUseCompleting();
        use.getAbility().onUseComplete(player, use.getLevel(), cancelled);
        if (!cancelled) {
            use.getAbility().onUseReleased(player, use.getLevel());
        }
        abilityData.clearUse();
        AbilitySync.syncUseState(player, null, 0, 0);
        if (cancelled) {
            MinecraftForge.EVENT_BUS.post(new AbilityInterruptedEvent(player, use.getAbility(), use.getLevel()));
        } else {
            applyDeclaredEffects(player, use.getAbility(), use.getLevel());
            if (!abilityData.isOnCooldown(use.getAbility())) {
                applyCompletionCooldown(player, abilityData, use.getAbility(), use.getLevel());
            }
            resetRequirementProgress(player, abilityData, use.getAbility(), use.getLevel());
            MinecraftForge.EVENT_BUS.post(new AbilityPerformedEvent(player, use.getAbility(), use.getLevel()));
        }
    }

    private static void resetRequirementProgress(ServerPlayer player, AbilityData abilityData, GatedAbility ability, int level) {
        if (player.isCreative()) {
            return;
        }
        boolean hadProgress = abilityData.getRequirementProgress(ability).isPresent();
        if (AbilityConfigs.killRequirement(ability, level) > 0 || AbilityConfigs.damageTakenRequirement(ability, level) > 0) {
            RequirementProgress progress = new RequirementProgress(0, 0);
            abilityData.putRequirementProgress(ability, progress);
            AbilitySync.syncRequirementProgress(player, ability, progress);
        } else if (hadProgress) {
            abilityData.removeRequirementProgress(ability);
            AbilitySync.syncRequirementProgress(player, ability, null);
        }
    }

    private static Component unmetRequirementMessage(GatedAbility ability, int level, RequirementProgress progress) {
        int killsMissing = AbilityConfigs.killRequirement(ability, level) - progress.getKills();
        if (killsMissing > 0) {
            return Component.translatable("message.player_abilities.need_kills", killsMissing);
        }
        float damageMissing = AbilityConfigs.damageTakenRequirement(ability, level) - progress.getDamageTaken();
        return Component.translatable("message.player_abilities.need_damage", String.format(Locale.ROOT, "%.1f", damageMissing));
    }

    public static void recordKill(ServerPlayer player) {
        updateRequirementProgress(player, RequirementProgress::addKill);
    }

    public static void recordDamageTaken(ServerPlayer player, float amount) {
        updateRequirementProgress(player, progress -> progress.addDamageTaken(amount));
    }

    private static void updateRequirementProgress(ServerPlayer player, Consumer<RequirementProgress> update) {
        AbilityData abilityData = getData(player);
        if (abilityData.getRequirementProgressMap().isEmpty()) {
            return;
        }
        for (Map.Entry<GatedAbility, RequirementProgress> entry : List.copyOf(abilityData.getRequirementProgressMap().entrySet())) {
            GatedAbility ability = entry.getKey();
            RequirementProgress progress = entry.getValue();
            int killsBefore = progress.getKills();
            int visibleDamageBefore = (int) progress.getDamageTaken();
            update.accept(progress);
            int level = abilityData.getEffectiveLevel(ability);
            if (progress.meets(AbilityConfigs.killRequirement(ability, level),
                    AbilityConfigs.damageTakenRequirement(ability, level))) {
                abilityData.removeRequirementProgress(ability);
                AbilitySync.syncRequirementProgress(player, ability, null);
            } else if (progress.getKills() != killsBefore || (int) progress.getDamageTaken() != visibleDamageBefore) {
                AbilitySync.syncRequirementProgress(player, ability, progress);
            }
        }
    }

    private static void applyDeclaredEffects(ServerPlayer player, GatedAbility ability, int level) {
        int durationTicks = AbilityConfigs.effectDurationTicks(ability, level);
        if (durationTicks > 0) {
            activateEffect(player, ability, level, durationTicks);
        }
        for (EffectGrant effectGrant : AbilityConfigs.effectGrants(ability, level)) {
            player.addEffect(new MobEffectInstance(effectGrant.effect(), effectGrant.durationTicks(), effectGrant.amplifier(),
                    false, effectGrant.showParticles(), effectGrant.showIcon()));
        }
    }

    public static void activateEffect(ServerPlayer player, Ability ability, int level, int durationTicks) {
        AbilityData abilityData = getData(player);
        Optional<ActiveEffect> existing = abilityData.getEffect(ability);
        ActiveEffect replacement = new ActiveEffect(level, durationTicks, durationTicks);
        if (existing.isPresent() && existing.get().getLevel() == level) {
            abilityData.carryEffectData(existing.get(), replacement);
            abilityData.putEffect(ability, replacement);
        } else {
            if (existing.isPresent()) {
                abilityData.removeEffect(ability);
                ability.onEffectEnd(player, existing.get().getLevel(), false);
            }
            abilityData.putEffect(ability, replacement);
            ability.onEffectStart(player, level);
        }
        AbilitySync.syncEffects(player);
    }

    public static void removeEffect(ServerPlayer player, Ability ability) {
        AbilityData abilityData = getData(player);
        abilityData.getEffect(ability).ifPresent(effect -> {
            abilityData.removeEffect(ability);
            ability.onEffectEnd(player, effect.getLevel(), false);
            AbilitySync.syncEffects(player);
        });
    }

    public static void clearEffects(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        if (abilityData.getActiveEffects().isEmpty()) {
            return;
        }
        for (Map.Entry<Ability, ActiveEffect> entry : List.copyOf(abilityData.getActiveEffects().entrySet())) {
            abilityData.removeEffect(entry.getKey());
            entry.getKey().onEffectEnd(player, entry.getValue().getLevel(), false);
        }
        AbilitySync.syncEffects(player);
    }

    private static void tickEffects(ServerPlayer player, AbilityData abilityData) {
        if (abilityData.getActiveEffects().isEmpty()) {
            return;
        }
        boolean anyExpired = false;
        for (Map.Entry<Ability, ActiveEffect> entry : List.copyOf(abilityData.getActiveEffects().entrySet())) {
            Ability ability = entry.getKey();
            ActiveEffect effect = entry.getValue();
            if (!AbilityConfigs.isEnabled(ability)) {
                removeEffect(player, ability);
                continue;
            }
            abilityData.decrementEffect(effect);
            ability.onEffectTick(player, effect.getLevel(), effect.getRemainingTicks());
            if (effect.isExpired() && abilityData.isCurrentEffect(ability, effect)) {
                abilityData.removeEffect(ability);
                ability.onEffectEnd(player, effect.getLevel(), true);
                anyExpired = true;
            }
        }
        if (anyExpired) {
            AbilitySync.syncEffects(player);
        }
    }

    public static Map<Ability, AbilityEffect> getActiveEffects(Player player) {
        return Collections.unmodifiableMap(getData(player).getActiveEffects());
    }

    @Nullable
    public static Object getEffectData(ServerPlayer player, Ability ability) {
        return getData(player).getEffect(ability).map(ActiveEffect::getEffectData).orElse(null);
    }

    public static void setEffectData(ServerPlayer player, Ability ability, @Nullable Object effectData) {
        getData(player).setEffectData(ability, effectData);
    }

    public static boolean trigger(ServerPlayer player, TriggeredAbility<?> ability) {
        AbilityData abilityData = getData(player);
        int level = abilityData.getEffectiveLevel(ability);
        if (level == 0 || (!player.isCreative() && abilityData.isOnCooldown(ability))) {
            return false;
        }
        FiringTrigger firingTrigger = new FiringTrigger(player.getUUID(), ability);
        if (!FIRING_TRIGGERS.add(firingTrigger)) {
            return false;
        }
        try {
            return runTrigger(player, abilityData, ability, level);
        } finally {
            FIRING_TRIGGERS.remove(firingTrigger);
        }
    }

    private static boolean runTrigger(ServerPlayer player, AbilityData abilityData, TriggeredAbility<?> ability, int level) {
        Optional<RequirementProgress> pendingRequirements = abilityData.getRequirementProgress(ability);
        if (!player.isCreative() && pendingRequirements.isPresent()
                && !pendingRequirements.get().meets(AbilityConfigs.killRequirement(ability, level),
                AbilityConfigs.damageTakenRequirement(ability, level))) {
            return false;
        }
        if (MinecraftForge.EVENT_BUS.post(new AbilityPerformEvent(player, ability, level))) {
            return false;
        }
        if (!abilityData.isGranted(ability)) {
            return false;
        }
        ability.onTrigger(player, level);
        applyDeclaredEffects(player, ability, level);
        if (!abilityData.isOnCooldown(ability)) {
            applyCompletionCooldown(player, abilityData, ability, level);
        }
        resetRequirementProgress(player, abilityData, ability, level);
        int cooldownTicks = abilityData.getCooldown(ability).map(Cooldown::totalTicks).orElse(0);
        AbilitySync.syncTriggered(player, ability, cooldownTicks);
        MinecraftForge.EVENT_BUS.post(new AbilityPerformedEvent(player, ability, level));
        return true;
    }

    private record FiringTrigger(java.util.UUID playerId, TriggeredAbility<?> ability) {
    }

    private static final java.util.Set<FiringTrigger> FIRING_TRIGGERS = new java.util.HashSet<>();

    public static boolean fireTrigger(AbilityTrigger<Void> trigger, ServerPlayer player) {
        return fireTrigger(trigger, player, null);
    }

    public static <T> boolean fireTrigger(AbilityTrigger<T> trigger, ServerPlayer player, T context) {
        boolean anyFired = false;
        for (AbilityData.TriggeredGrant grant : getData(player).getTriggeredGrants()) {
            if (grant.ability().getTrigger() != trigger) {
                continue;
            }
            @SuppressWarnings("unchecked")
            TriggeredAbility<T> matched = (TriggeredAbility<T>) grant.ability();
            if (matched.shouldTrigger(player, grant.level(), context) && trigger(player, matched)) {
                anyFired = true;
                if (trigger.isExclusive()) {
                    return true;
                }
            }
        }
        return anyFired;
    }

    public static void applyEnabledTransitions(ServerPlayer player, Set<Ability> newlyDisabled, Set<Ability> newlyEnabled) {
        AbilityData abilityData = getData(player);
        for (Ability ability : newlyDisabled) {
            int rawLevel = abilityData.getEffectiveLevelIgnoringDisabled(ability);
            if (rawLevel == 0) {
                continue;
            }
            if (abilityData.getActiveUse().filter(use -> use.getAbility().equals(ability)).isPresent()) {
                cancelUse(player);
            }
            if (abilityData.getSelected().filter(ability::equals).isPresent()) {
                abilityData.setSelected(null);
            }
            if (abilityData.getEffect(ability).isPresent()) {
                removeEffect(player, ability);
            }
            if (ability instanceof PassiveAbility passive && !abilityData.isPassiveDisabled(passive)) {
                deactivatePassive(player, passive, rawLevel);
            }
        }
        for (Ability ability : newlyEnabled) {
            int level = abilityData.getEffectiveLevel(ability);
            if (level > 0 && ability instanceof PassiveAbility passive && !abilityData.isPassiveDisabled(passive)) {
                activatePassive(player, passive, level);
            }
        }
        AbilitySync.syncAbilities(player);
    }

    public static boolean isPassiveEnabled(Player player, PassiveAbility passive) {
        return !getData(player).isPassiveDisabled(passive);
    }

    public static int getPassiveLevel(Player player, PassiveAbility passive) {
        AbilityData abilityData = getData(player);
        return abilityData.isPassiveDisabled(passive) ? 0 : abilityData.getEffectiveLevel(passive);
    }

    public static boolean setPassiveEnabled(ServerPlayer player, PassiveAbility passive, boolean enabled) {
        AbilityData abilityData = getData(player);
        if (!abilityData.setPassiveDisabled(passive, !enabled)) {
            return false;
        }
        int level = abilityData.getEffectiveLevel(passive);
        if (level > 0) {
            if (enabled) {
                activatePassive(player, passive, level);
            } else {
                deactivatePassive(player, passive, level);
            }
        }
        AbilitySync.syncAbilities(player);
        return true;
    }

    public static void activatePassive(ServerPlayer player, PassiveAbility passive, int level) {
        applyAttributeGrants(player, passive, level);
        passive.onActivated(player, level);
    }

    public static void deactivatePassive(ServerPlayer player, PassiveAbility passive, int level) {
        removeAttributeGrants(player, passive);
        passive.onDeactivated(player, level);
    }

    private static void applyAttributeGrants(ServerPlayer player, PassiveAbility passive, int level) {
        for (AttributeGrant attributeGrant : AbilityConfigs.attributeGrants(passive, level)) {
            AttributeInstance attributeInstance = player.getAttribute(attributeGrant.attribute());
            if (attributeInstance == null) {
                PlayerAbilities.LOGGER.warn("Passive {} declares a grant for attribute {} that players do not have",
                        passive.getId(), ForgeRegistries.ATTRIBUTES.getKey(attributeGrant.attribute()));
            } else {
                String modifierName = attributeModifierName(passive, attributeGrant);
                UUID modifierUuid = attributeModifierUuid(modifierName);
                attributeInstance.removeModifier(modifierUuid);
                attributeInstance.addPermanentModifier(new AttributeModifier(
                        modifierUuid, modifierName, attributeGrant.amount(), attributeGrant.operation()));
            }
        }
    }

    public static void removeAttributeGrants(ServerPlayer player, PassiveAbility passive) {
        String modifierNamePrefix = attributeModifierNamePrefix(passive);
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance == null) continue;
            for (AttributeModifier modifier : List.copyOf(attributeInstance.getModifiers())) {
                if (modifier.getName().startsWith(modifierNamePrefix)) {
                    attributeInstance.removeModifier(modifier.getId());
                }
            }
        }
    }

    private static String attributeModifierNamePrefix(PassiveAbility passive) {
        ResourceLocation abilityId = passive.getId();
        return abilityId.getNamespace() + ":passive/" + abilityId.getPath() + "/";
    }

    private static String attributeModifierName(PassiveAbility passive, AttributeGrant attributeGrant) {
        ResourceLocation attributeId = java.util.Objects.requireNonNull(
                ForgeRegistries.ATTRIBUTES.getKey(attributeGrant.attribute()));
        return attributeModifierNamePrefix(passive)
                + attributeId.getNamespace() + "." + attributeId.getPath()
                + "/" + operationName(attributeGrant.operation());
    }

    private static UUID attributeModifierUuid(String modifierName) {
        return UUID.nameUUIDFromBytes(modifierName.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String operationName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADDITION -> "add_value";
            case MULTIPLY_BASE -> "add_multiplied_base";
            case MULTIPLY_TOTAL -> "add_multiplied_total";
        };
    }

    private static void applyCompletionCooldown(ServerPlayer player, AbilityData abilityData, GatedAbility ability, int level) {
        if (player.isCreative()) {
            return;
        }
        int cooldownTicks = applyCooldownAttribute(player, AbilityConfigs.cooldownTicks(ability, level));
        if (cooldownTicks > 0) {
            Cooldown cooldown = new Cooldown(cooldownTicks, cooldownTicks);
            abilityData.setCooldown(ability, cooldown);
            AbilitySync.syncCooldown(player, ability, cooldown);
        }
    }

    public static void setCooldown(ServerPlayer player, GatedAbility ability, int ticks) {
        AbilityData abilityData = getData(player);
        int adjustedTicks = applyCooldownAttribute(player, ticks);
        if (adjustedTicks > 0) {
            Cooldown cooldown = new Cooldown(adjustedTicks, adjustedTicks);
            abilityData.setCooldown(ability, cooldown);
            AbilitySync.syncCooldown(player, ability, cooldown);
        } else {
            abilityData.removeCooldown(ability);
            AbilitySync.syncCooldown(player, ability, new Cooldown(0, 0));
        }
    }

    private static int applyCooldownAttribute(ServerPlayer player, int baseTicks) {
        double cooldownMultiplier = Math.max(0.0, 2.0 - player.getAttributeValue(AbilityAttributes.ABILITY_COOLDOWN.get()));
        return (int) (baseTicks * cooldownMultiplier);
    }

    @Nullable
    public static Object getUseData(ServerPlayer player) {
        return getData(player).getUseData();
    }

    public static void setUseData(ServerPlayer player, @Nullable Object useData) {
        getData(player).setUseData(useData);
    }

    public static Optional<AbilityUse> getActiveUse(Player player) {
        return getData(player).getActiveUse().map(use -> use);
    }

    public static double getAbilityPower(ServerPlayer player) {
        return player.getAttributeValue(AbilityAttributes.ABILITY_POWER.get());
    }

    public static int getLevel(Player player, Ability ability) {
        return getData(player).getEffectiveLevel(ability);
    }

    public static Map<Ability, Integer> getGrantedLevels(Player player) {
        return getData(player).getGrantedLevels();
    }

    public static List<ActiveAbility> getGrantedActives(Player player) {
        return getData(player).getGrantedActives();
    }

    public static Map<ResourceLocation, Map<Ability, Integer>> getGrantsBySource(Player player) {
        return getData(player).getGrantsBySource();
    }

    public static Optional<ActiveAbility> getSelected(Player player) {
        return getData(player).getSelected();
    }

    public static Optional<Cooldown> getCooldown(Player player, GatedAbility ability) {
        return getData(player).getCooldown(ability);
    }

    private static AbilityData getData(Player player) {
        return AbilityCapability.get(player);
    }
}

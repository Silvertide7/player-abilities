package net.silvertide.player_abilities.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.event.AbilityCastInterruptedEvent;
import net.silvertide.player_abilities.api.event.AbilityGrantedEvent;
import net.silvertide.player_abilities.api.event.AbilityPerformEvent;
import net.silvertide.player_abilities.api.event.AbilityPerformedEvent;
import net.silvertide.player_abilities.api.event.AbilityRevokedEvent;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.data.ActiveCast;
import net.silvertide.player_abilities.data.ActiveEffect;
import net.silvertide.player_abilities.data.RequirementProgress;
import net.silvertide.player_abilities.network.AbilitySync;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class AbilityAPI {
    public static final int MIN_LEVEL = 1;
    private static final double STATIONARY_CAST_MAX_DRIFT_SQ = 0.25;

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
            NeoForge.EVENT_BUS.post(new AbilityGrantedEvent(player, ability, effectiveAfter, source));
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
            NeoForge.EVENT_BUS.post(new AbilityRevokedEvent(player, ability, effectiveBefore, source));
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
            if (abilityData.getActiveCast().filter(cast -> cast.getAbility().equals(active)).isPresent()) {
                cancelCast(player);
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

    public static boolean cast(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        Optional<ActiveCast> currentCast = abilityData.getActiveCast();
        if (currentCast.isPresent()) {
            if (currentCast.get().getAbility().getUseType() == AbilityUseType.CHANNELED) {
                finishCast(player);
            } else {
                cancelCast(player);
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
        Component abilityName = Component.translatable(ability.getDescriptionId());
        if (abilityData.isOnCooldown(ability)) {
            player.displayClientMessage(Component.translatable("message.player_abilities.on_cooldown", abilityName), true);
            return false;
        }
        Optional<RequirementProgress> pendingRequirements = abilityData.getRequirementProgress(ability);
        if (pendingRequirements.isPresent()
                && !pendingRequirements.get().meets(AbilityConfigs.killRequirement(ability, level),
                AbilityConfigs.damageTakenRequirement(ability, level))) {
            player.displayClientMessage(unmetRequirementMessage(ability, level, pendingRequirements.get()), true);
            return false;
        }
        if (!ability.canCast(player, level)) {
            Component failureMessage = ability.getCastFailureMessage(player, level);
            player.displayClientMessage(failureMessage != null ? failureMessage
                    : Component.translatable("message.player_abilities.cannot_cast", abilityName), true);
            return false;
        }
        if (NeoForge.EVENT_BUS.post(new AbilityPerformEvent(player, ability, level)).isCanceled()) {
            return false;
        }
        if (!abilityData.isGranted(ability)) {
            return false;
        }
        if (ability.getUseType() == AbilityUseType.INSTANT) {
            ability.onCast(player, level);
            applyDeclaredEffects(player, ability, level);
            if (!abilityData.isOnCooldown(ability)) {
                applyCompletionCooldown(player, abilityData, ability, level);
            }
            resetRequirementProgress(player, abilityData, ability, level);
            NeoForge.EVENT_BUS.post(new AbilityPerformedEvent(player, ability, level));
            return true;
        }
        int castTicks = AbilityConfigs.castTicks(ability, level);
        ability.onCastStart(player, level);
        abilityData.startCast(ability, level, castTicks, player.position(), player.hurtTime);
        AbilitySync.syncCastState(player, ability, level, castTicks);
        if (castTicks <= 0) {
            finishCast(player);
        }
        return true;
    }

    public static void serverTick(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        tickActiveCast(player, abilityData);
        tickEffects(player, abilityData);
    }

    private static void tickActiveCast(ServerPlayer player, AbilityData abilityData) {
        Optional<ActiveCast> activeCast = abilityData.getActiveCast();
        if (activeCast.isEmpty()) {
            return;
        }
        ActiveCast cast = activeCast.get();
        ActiveAbility ability = cast.getAbility();
        if (ability.isInterruptedByDamage()) {
            if (player.hurtTime > cast.getLastHurtTime()) {
                cancelCast(player);
                return;
            }
            abilityData.recordCastHurtBaseline(player.hurtTime);
        }
        if (ability.requiresStationary() && cast.getStartPosition() != null
                && player.position().distanceToSqr(cast.getStartPosition()) > STATIONARY_CAST_MAX_DRIFT_SQ) {
            cancelCast(player);
            return;
        }
        abilityData.incrementCastElapsed();
        ability.onCastTick(player, cast.getLevel(), cast.getElapsedTicks());
        if (abilityData.getActiveCast().filter(current -> current == cast && !current.isCompleting()).isEmpty()) {
            return;
        }
        if (cast.getElapsedTicks() >= cast.getTotalTicks()) {
            finishCast(player);
        }
    }

    public static void finishCast(ServerPlayer player) {
        completeCast(player, false);
    }

    public static void cancelCast(ServerPlayer player) {
        completeCast(player, true);
    }

    private static void completeCast(ServerPlayer player, boolean cancelled) {
        AbilityData abilityData = getData(player);
        Optional<ActiveCast> activeCast = abilityData.getActiveCast();
        if (activeCast.isEmpty() || activeCast.get().isCompleting()) {
            return;
        }
        ActiveCast cast = activeCast.get();
        abilityData.markCastCompleting();
        cast.getAbility().onCastComplete(player, cast.getLevel(), cancelled);
        abilityData.clearCast();
        AbilitySync.syncCastState(player, null, 0, 0);
        if (cancelled) {
            NeoForge.EVENT_BUS.post(new AbilityCastInterruptedEvent(player, cast.getAbility(), cast.getLevel()));
        } else {
            applyDeclaredEffects(player, cast.getAbility(), cast.getLevel());
            if (!abilityData.isOnCooldown(cast.getAbility())) {
                applyCompletionCooldown(player, abilityData, cast.getAbility(), cast.getLevel());
            }
            resetRequirementProgress(player, abilityData, cast.getAbility(), cast.getLevel());
            NeoForge.EVENT_BUS.post(new AbilityPerformedEvent(player, cast.getAbility(), cast.getLevel()));
        }
    }

    private static void resetRequirementProgress(ServerPlayer player, AbilityData abilityData, GatedAbility ability, int level) {
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
            player.addEffect(new MobEffectInstance(effectGrant.effect(), effectGrant.durationTicks(), effectGrant.amplifier()));
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

    public static Map<Ability, ActiveEffect> getActiveEffects(Player player) {
        return getData(player).getActiveEffects();
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
        if (level == 0 || abilityData.isOnCooldown(ability)) {
            return false;
        }
        Optional<RequirementProgress> pendingRequirements = abilityData.getRequirementProgress(ability);
        if (pendingRequirements.isPresent()
                && !pendingRequirements.get().meets(AbilityConfigs.killRequirement(ability, level),
                AbilityConfigs.damageTakenRequirement(ability, level))) {
            return false;
        }
        if (NeoForge.EVENT_BUS.post(new AbilityPerformEvent(player, ability, level)).isCanceled()) {
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
        NeoForge.EVENT_BUS.post(new AbilityPerformedEvent(player, ability, level));
        return true;
    }

    public static boolean fireTrigger(AbilityTrigger<Void> trigger, ServerPlayer player) {
        return fireTrigger(trigger, player, null);
    }

    public static <T> boolean fireTrigger(AbilityTrigger<T> trigger, ServerPlayer player, T context) {
        boolean anyFired = false;
        for (Map.Entry<Ability, Integer> entry : getData(player).getGrantedLevels().entrySet()) {
            if (!(entry.getKey() instanceof TriggeredAbility<?> triggered) || triggered.getTrigger() != trigger) {
                continue;
            }
            @SuppressWarnings("unchecked")
            TriggeredAbility<T> matched = (TriggeredAbility<T>) triggered;
            if (matched.shouldTrigger(player, entry.getValue(), context) && trigger(player, matched)) {
                anyFired = true;
                if (trigger.isExclusive()) {
                    return true;
                }
            }
        }
        return anyFired;
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
        removeAttributeGrants(player, passive, level);
        passive.onDeactivated(player, level);
    }

    private static void applyAttributeGrants(ServerPlayer player, PassiveAbility passive, int level) {
        for (AttributeGrant attributeGrant : AbilityConfigs.attributeGrants(passive, level)) {
            AttributeInstance attributeInstance = player.getAttribute(attributeGrant.attribute());
            if (attributeInstance == null) {
                PlayerAbilities.LOGGER.warn("Passive {} declares a grant for attribute {} that players do not have",
                        passive.getId(), attributeGrant.attribute().getRegisteredName());
            } else {
                attributeInstance.addOrReplacePermanentModifier(new AttributeModifier(
                        attributeModifierId(passive, attributeGrant), attributeGrant.amount(), attributeGrant.operation()));
            }
        }
    }

    private static void removeAttributeGrants(ServerPlayer player, PassiveAbility passive, int level) {
        List<AttributeGrant> declaredEverywhere = new ArrayList<>(AbilityConfigs.attributeGrants(passive, level));
        declaredEverywhere.addAll(passive.getAttributeGrants(level));
        for (AttributeGrant attributeGrant : declaredEverywhere) {
            AttributeInstance attributeInstance = player.getAttribute(attributeGrant.attribute());
            if (attributeInstance != null) {
                attributeInstance.removeModifier(attributeModifierId(passive, attributeGrant));
            }
        }
    }

    private static ResourceLocation attributeModifierId(PassiveAbility passive, AttributeGrant attributeGrant) {
        ResourceLocation abilityId = passive.getId();
        ResourceLocation attributeId = attributeGrant.attribute().unwrapKey().orElseThrow().location();
        return ResourceLocation.fromNamespaceAndPath(abilityId.getNamespace(),
                "passive/" + abilityId.getPath() + "/" + attributeId.getNamespace() + "." + attributeId.getPath()
                        + "/" + attributeGrant.operation().getSerializedName());
    }

    private static void applyCompletionCooldown(ServerPlayer player, AbilityData abilityData, GatedAbility ability, int level) {
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
        double cooldownMultiplier = Math.max(0.0, 2.0 - player.getAttributeValue(AbilityAttributes.ABILITY_COOLDOWN));
        return (int) (baseTicks * cooldownMultiplier);
    }

    @Nullable
    public static Object getCastData(ServerPlayer player) {
        return getData(player).getActiveCast().map(ActiveCast::getCastData).orElse(null);
    }

    public static void setCastData(ServerPlayer player, @Nullable Object castData) {
        getData(player).setCastData(castData);
    }

    public static Optional<ActiveCast> getActiveCast(Player player) {
        return getData(player).getActiveCast();
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
        return player.getData(AbilityAttachments.ABILITY_DATA);
    }
}

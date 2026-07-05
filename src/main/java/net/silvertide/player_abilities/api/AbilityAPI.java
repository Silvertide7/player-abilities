package net.silvertide.player_abilities.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
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
        if (grantedLevel != level) {
            PlayerAbilities.LOGGER.warn("Grant of {} requested level {} outside [{}, {}]; clamping to {}",
                    ability.getId(), level, MIN_LEVEL, maxLevel, grantedLevel);
        }
        AbilityData abilityData = getData(player);
        int effectiveBefore = abilityData.getEffectiveLevel(ability);
        int previousSourceLevel = abilityData.setGrant(source, ability, grantedLevel);
        if (previousSourceLevel == grantedLevel) {
            return false;
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
        if (ability instanceof PassiveAbility passive) {
            if (effectiveBefore > 0) {
                passive.onDeactivated(player, effectiveBefore);
            }
            if (effectiveAfter > 0) {
                passive.onActivated(player, effectiveAfter);
            }
        }
        if (ability instanceof ActiveAbility active) {
            if (abilityData.getActiveCast().filter(cast -> cast.getAbility().equals(active)).isPresent()) {
                cancelCast(player);
            }
            if (effectiveAfter == 0) {
                if (abilityData.getSelected().filter(active::equals).isPresent()) {
                    abilityData.setSelected(null);
                }
                if (abilityData.isOnCooldown(active)) {
                    abilityData.removeCooldown(active);
                    AbilitySync.syncCooldown(player, active, new Cooldown(0, 0));
                }
                if (abilityData.getRequirementProgress(active).isPresent()) {
                    abilityData.removeRequirementProgress(active);
                    AbilitySync.syncRequirementProgress(player, active, null);
                }
            }
        }
        AbilitySync.syncAbilities(player);
    }

    public static boolean cast(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        Optional<ActiveCast> currentCast = abilityData.getActiveCast();
        if (currentCast.isPresent()) {
            if (currentCast.get().getAbility().getCastType() == AbilityUseType.CHANNELED) {
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
        if (pendingRequirements.isPresent()) {
            if (pendingRequirements.get().meets(AbilityConfigs.killRequirement(ability, level),
                    AbilityConfigs.damageTakenRequirement(ability, level))) {
                abilityData.removeRequirementProgress(ability);
                AbilitySync.syncRequirementProgress(player, ability, null);
            } else {
                player.displayClientMessage(unmetRequirementMessage(ability, level, pendingRequirements.get()), true);
                return false;
            }
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
        if (ability.getCastType() == AbilityUseType.INSTANT) {
            ability.onCast(player, level);
            activateDeclaredEffect(player, ability, level);
            if (!abilityData.isOnCooldown(ability)) {
                applyCompletionCooldown(player, abilityData, ability, level);
            }
            startRequirementProgress(player, abilityData, ability, level);
            NeoForge.EVENT_BUS.post(new AbilityPerformedEvent(player, ability, level));
            return true;
        }
        int castTicks = ability.getCastTicks(level);
        ability.onCastStart(player, level);
        ActiveCast startedCast = abilityData.startCast(ability, level, castTicks, player.position());
        startedCast.setLastHurtTime(player.hurtTime);
        AbilitySync.syncCastState(player, ability, level, castTicks);
        if (castTicks <= 0) {
            finishCast(player);
        }
        return true;
    }

    public static void tickActiveCast(ServerPlayer player) {
        AbilityData abilityData = getData(player);
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
            cast.setLastHurtTime(player.hurtTime);
        }
        if (ability.requiresStationary() && cast.getStartPosition() != null
                && player.position().distanceToSqr(cast.getStartPosition()) > STATIONARY_CAST_MAX_DRIFT_SQ) {
            cancelCast(player);
            return;
        }
        cast.incrementElapsed();
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
        cast.markCompleting();
        cast.getAbility().onCastComplete(player, cast.getLevel(), cancelled);
        abilityData.clearCast();
        AbilitySync.syncCastState(player, null, 0, 0);
        if (cancelled) {
            NeoForge.EVENT_BUS.post(new AbilityCastInterruptedEvent(player, cast.getAbility(), cast.getLevel()));
        } else {
            activateDeclaredEffect(player, cast.getAbility(), cast.getLevel());
            if (!abilityData.isOnCooldown(cast.getAbility())) {
                applyCompletionCooldown(player, abilityData, cast.getAbility(), cast.getLevel());
            }
            startRequirementProgress(player, abilityData, cast.getAbility(), cast.getLevel());
            NeoForge.EVENT_BUS.post(new AbilityPerformedEvent(player, cast.getAbility(), cast.getLevel()));
        }
    }

    private static void startRequirementProgress(ServerPlayer player, AbilityData abilityData, GatedAbility ability, int level) {
        if (AbilityConfigs.killRequirement(ability, level) > 0 || AbilityConfigs.damageTakenRequirement(ability, level) > 0) {
            RequirementProgress progress = new RequirementProgress(0, 0);
            abilityData.putRequirementProgress(ability, progress);
            AbilitySync.syncRequirementProgress(player, ability, progress);
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
            update.accept(progress);
            int level = abilityData.getEffectiveLevel(ability);
            if (progress.meets(AbilityConfigs.killRequirement(ability, level),
                    AbilityConfigs.damageTakenRequirement(ability, level))) {
                abilityData.removeRequirementProgress(ability);
                AbilitySync.syncRequirementProgress(player, ability, null);
            } else {
                AbilitySync.syncRequirementProgress(player, ability, progress);
            }
        }
    }

    private static void activateDeclaredEffect(ServerPlayer player, ActiveAbility ability, int level) {
        int durationTicks = ability.getEffectDurationTicks(level);
        if (durationTicks > 0) {
            activateEffect(player, ability, level, durationTicks);
        }
    }

    public static void activateEffect(ServerPlayer player, Ability ability, int level, int durationTicks) {
        AbilityData abilityData = getData(player);
        Optional<ActiveEffect> existing = abilityData.getEffect(ability);
        ActiveEffect replacement = new ActiveEffect(level, durationTicks, durationTicks);
        if (existing.isPresent() && existing.get().getLevel() == level) {
            replacement.setEffectData(existing.get().getEffectData());
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

    public static void tickEffects(ServerPlayer player) {
        AbilityData abilityData = getData(player);
        if (abilityData.getActiveEffects().isEmpty()) {
            return;
        }
        boolean anyExpired = false;
        for (Map.Entry<Ability, ActiveEffect> entry : List.copyOf(abilityData.getActiveEffects().entrySet())) {
            Ability ability = entry.getKey();
            ActiveEffect effect = entry.getValue();
            effect.decrementRemaining();
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

    public static boolean isEffectActive(Player player, Ability ability) {
        return getData(player).getEffect(ability).isPresent();
    }

    public static int getEffectLevel(Player player, Ability ability) {
        return getData(player).getEffect(ability).map(ActiveEffect::getLevel).orElse(0);
    }

    public static Map<Ability, ActiveEffect> getActiveEffects(Player player) {
        return getData(player).getActiveEffects();
    }

    @Nullable
    public static Object getEffectData(ServerPlayer player, Ability ability) {
        return getData(player).getEffect(ability).map(ActiveEffect::getEffectData).orElse(null);
    }

    public static void setEffectData(ServerPlayer player, Ability ability, @Nullable Object effectData) {
        getData(player).getEffect(ability).ifPresent(effect -> effect.setEffectData(effectData));
    }

    public static boolean trigger(ServerPlayer player, TriggeredAbility ability) {
        AbilityData abilityData = getData(player);
        int level = abilityData.getEffectiveLevel(ability);
        if (level == 0 || abilityData.isOnCooldown(ability)) {
            return false;
        }
        Optional<RequirementProgress> pendingRequirements = abilityData.getRequirementProgress(ability);
        if (pendingRequirements.isPresent()) {
            if (!pendingRequirements.get().meets(AbilityConfigs.killRequirement(ability, level),
                    AbilityConfigs.damageTakenRequirement(ability, level))) {
                return false;
            }
            abilityData.removeRequirementProgress(ability);
            AbilitySync.syncRequirementProgress(player, ability, null);
        }
        if (NeoForge.EVENT_BUS.post(new AbilityPerformEvent(player, ability, level)).isCanceled()) {
            return false;
        }
        if (!abilityData.isGranted(ability)) {
            return false;
        }
        ability.onTrigger(player, level);
        if (!abilityData.isOnCooldown(ability)) {
            applyCompletionCooldown(player, abilityData, ability, level);
        }
        startRequirementProgress(player, abilityData, ability, level);
        int cooldownTicks = abilityData.getCooldown(ability).map(Cooldown::totalTicks).orElse(0);
        AbilitySync.syncTriggered(player, ability, cooldownTicks);
        NeoForge.EVENT_BUS.post(new AbilityPerformedEvent(player, ability, level));
        return true;
    }

    public static boolean triggerOnLethalDamage(ServerPlayer player, float incomingDamage) {
        for (Map.Entry<Ability, Integer> entry : getData(player).getGrantedLevels().entrySet()) {
            if (entry.getKey() instanceof TriggeredAbility triggered
                    && triggered.triggersOnLethalDamage(player, entry.getValue(), incomingDamage)
                    && trigger(player, triggered)) {
                return true;
            }
        }
        return false;
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
        getData(player).getActiveCast().ifPresent(cast -> cast.setCastData(castData));
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

    public static boolean canStandOnFluid(Player player, FluidState fluidState) {
        return getData(player).anyPassiveAllowsStandingOn(player, fluidState);
    }

    private static AbilityData getData(Player player) {
        return player.getData(AbilityAttachments.ABILITY_DATA);
    }
}

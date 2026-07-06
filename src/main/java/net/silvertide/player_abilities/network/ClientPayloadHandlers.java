package net.silvertide.player_abilities.network;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.client.AbilityNotifications;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.ActiveEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handleSyncAbilities(SyncAbilitiesPayload payload, IPayloadContext context) {
        if (!(context.player().level().getEntity(payload.playerEntityId()) instanceof Player targetPlayer)) {
            return;
        }
        Map<Ability, Integer> grantedLevels = new HashMap<>();
        payload.grantedLevels().forEach((abilityId, level) -> {
            Ability ability = AbilityRegistry.ABILITIES.get(abilityId);
            if (ability != null) {
                grantedLevels.put(ability, level);
            }
        });
        Set<PassiveAbility> disabledPassives = new HashSet<>();
        payload.disabledPassiveIds().forEach(abilityId ->
                AbilityRegistry.getPassive(abilityId).ifPresent(disabledPassives::add));
        ActiveAbility selected = payload.selectedId()
                .flatMap(AbilityRegistry::getActive)
                .orElse(null);
        targetPlayer.getData(AbilityAttachments.ABILITY_DATA).replaceSyncedState(grantedLevels, disabledPassives, selected);
    }

    public static void handleSyncCooldown(SyncCooldownPayload payload, IPayloadContext context) {
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(active -> {
            var abilityData = context.player().getData(AbilityAttachments.ABILITY_DATA);
            if (payload.cooldown().isExpired()) {
                abilityData.removeCooldown(active);
            } else {
                abilityData.setCooldown(active, payload.cooldown());
            }
        });
    }

    public static void handleSyncEffects(SyncEffectsPayload payload, IPayloadContext context) {
        Map<Ability, ActiveEffect> effects = new HashMap<>();
        payload.effects().forEach((abilityId, effect) -> {
            Ability ability = AbilityRegistry.ABILITIES.get(abilityId);
            if (ability != null) {
                effects.put(ability, effect);
            }
        });
        context.player().getData(AbilityAttachments.ABILITY_DATA).replaceSyncedEffects(effects);
    }

    public static void handleSyncAbilityConfigs(SyncAbilityConfigsPayload payload, IPayloadContext context) {
        AbilityConfigs.replaceFromSync(payload.configs());
    }

    public static void handleTriggeredActivated(TriggeredActivatedPayload payload, IPayloadContext context) {
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(ability ->
                AbilityNotifications.onTriggeredActivated(ability, payload.cooldownTicks()));
    }

    public static void handleSyncRequirement(SyncRequirementPayload payload, IPayloadContext context) {
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(active -> {
            var abilityData = context.player().getData(AbilityAttachments.ABILITY_DATA);
            payload.progress().ifPresentOrElse(
                    progress -> abilityData.putRequirementProgress(active, progress),
                    () -> abilityData.removeRequirementProgress(active));
        });
    }

    public static void handleSyncUseState(SyncUseStatePayload payload, IPayloadContext context) {
        var abilityData = context.player().getData(AbilityAttachments.ABILITY_DATA);
        Optional<ActiveAbility> usingAbility = payload.usingAbilityId().flatMap(AbilityRegistry::getActive);
        if (usingAbility.isPresent()) {
            abilityData.startUse(usingAbility.get(), payload.level(), payload.totalTicks(), null, 0);
        } else {
            abilityData.clearUse();
        }
    }
}

package net.silvertide.player_abilities.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.client.AbilityNotifications;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityCapability;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.data.ActiveEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handleSyncAbilities(SyncAbilitiesPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(payload.playerEntityId()) instanceof Player targetPlayer)) {
            return;
        }
        Map<Ability, Integer> grantedLevels = new HashMap<>();
        payload.grantedLevels().forEach((abilityId, grantedLevel) -> {
            Ability ability = AbilityRegistry.abilities().getValue(abilityId);
            if (ability != null) {
                grantedLevels.put(ability, grantedLevel);
            }
        });
        Set<PassiveAbility> disabledPassives = new HashSet<>();
        payload.disabledPassiveIds().forEach(abilityId ->
                AbilityRegistry.getPassive(abilityId).ifPresent(disabledPassives::add));
        ActiveAbility selected = payload.selectedId()
                .flatMap(AbilityRegistry::getActive)
                .orElse(null);
        AbilityData targetData = AbilityCapability.find(targetPlayer);
        if (targetData == null) {
            return;
        }
        targetData.replaceSyncedState(grantedLevels, disabledPassives, selected);
    }

    public static void handleSyncCooldown(SyncCooldownPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AbilityData abilityData = AbilityCapability.find(player);
        if (abilityData == null) {
            return;
        }
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(active -> {
            if (payload.cooldown().isExpired()) {
                abilityData.removeCooldown(active);
            } else {
                abilityData.setCooldown(active, payload.cooldown());
            }
        });
    }

    public static void handleSyncEffects(SyncEffectsPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Map<Ability, ActiveEffect> effects = new HashMap<>();
        payload.effects().forEach((abilityId, effect) -> {
            Ability ability = AbilityRegistry.abilities().getValue(abilityId);
            if (ability != null) {
                effects.put(ability, effect);
            }
        });
        AbilityData abilityData = AbilityCapability.find(player);
        if (abilityData == null) {
            return;
        }
        abilityData.replaceSyncedEffects(effects);
    }

    public static void handleSyncAbilityConfigs(SyncAbilityConfigsPayload payload) {
        AbilityConfigs.replaceFromSync(payload.configs());
    }

    public static void handleTriggeredActivated(TriggeredActivatedPayload payload) {
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(ability ->
                AbilityNotifications.onTriggeredActivated(ability, payload.cooldownTicks()));
    }

    public static void handleAbilityReady(AbilityReadyPayload payload) {
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(AbilityNotifications::onAbilityReady);
    }

    public static void handleSyncRequirement(SyncRequirementPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AbilityData abilityData = AbilityCapability.find(player);
        if (abilityData == null) {
            return;
        }
        AbilityRegistry.getGated(payload.abilityId()).ifPresent(active ->
            payload.progress().ifPresentOrElse(
                    progress -> abilityData.putRequirementProgress(active, progress),
                    () -> abilityData.removeRequirementProgress(active)));
    }

    public static void handleSyncUseState(SyncUseStatePayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AbilityData abilityData = AbilityCapability.find(player);
        if (abilityData == null) {
            return;
        }
        Optional<ActiveAbility> usingAbility = payload.usingAbilityId().flatMap(AbilityRegistry::getActive);
        if (usingAbility.isPresent()) {
            abilityData.startUse(usingAbility.get(), payload.level(), payload.totalTicks(), null, 0);
        } else {
            abilityData.clearUse();
        }
    }
}

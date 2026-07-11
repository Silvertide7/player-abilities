package net.silvertide.player_abilities.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityCapability;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.network.AbilitySync;

import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class PlayerLifecycleHandler {
    private PlayerLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            reapplyPassives(serverPlayer, serverPlayer);
            AbilityAPI.getActiveEffects(serverPlayer).forEach((ability, effect) -> {
                if (AbilityConfigs.isEnabled(ability)) {
                    ability.onEffectStart(serverPlayer, effect.getLevel());
                }
            });
            AbilitySync.syncAllState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        AbilityCapability.copy(event.getOriginal(), event.getEntity());
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer newPlayer) {
            reapplyPassives(newPlayer, newPlayer);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilitySync.syncAllState(serverPlayer);
            AbilityAPI.fireTrigger(PlayerTriggers.RESPAWN, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.cancelUse(serverPlayer);
            AbilitySync.syncAllState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.cancelUse(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.cancelUse(serverPlayer);
            AbilityAPI.clearEffects(serverPlayer);
            AbilityAPI.fireTrigger(PlayerTriggers.DEATH, serverPlayer, event.getSource());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            AbilityAPI.recordKill(killer);
            AbilityAPI.fireTrigger(PlayerTriggers.KILL, killer, event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLethalDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getAmount() < serverPlayer.getHealth() + serverPlayer.getAbsorptionAmount()) {
            return;
        }
        if (AbilityAPI.fireTrigger(PlayerTriggers.LETHAL_DAMAGE, serverPlayer,
                new PlayerTriggers.DamageTaken(event.getAmount(), event.getSource()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0) {
            return;
        }
        float amount = event.getAmount();
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        ServerPlayer victim = target instanceof ServerPlayer targetPlayer ? targetPlayer : null;
        ServerPlayer attacker = source.getEntity() instanceof ServerPlayer sourcePlayer && sourcePlayer != target
                ? sourcePlayer : null;
        if (victim == null && attacker == null) {
            return;
        }
        MinecraftServer server = target.getServer();
        if (server == null) {
            return;
        }
        Float healthBefore = victim != null ? victim.getHealth() : null;
        if (victim != null) {
            AbilityAPI.recordDamageTaken(victim, amount);
        }
        server.tell(new TickTask(server.getTickCount(), () -> {
            if (victim != null && !victim.hasDisconnected()) {
                AbilityAPI.fireTrigger(PlayerTriggers.DAMAGE_TAKEN, victim,
                        new PlayerTriggers.DamageTaken(amount, source));
                AbilityAPI.fireTrigger(PlayerTriggers.HEALTH_DROPPED, victim,
                        new PlayerTriggers.HealthChange(healthBefore, Math.max(0F, healthBefore - amount),
                                victim.getMaxHealth()));
            }
            if (attacker != null && !attacker.hasDisconnected()) {
                AbilityAPI.fireTrigger(PlayerTriggers.DEALT_DAMAGE, attacker,
                        new PlayerTriggers.DamageDealt(target, amount));
            }
        }));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer trackedPlayer
                && event.getEntity() instanceof ServerPlayer receiver) {
            AbilitySync.syncAbilitiesTo(trackedPlayer, receiver);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        AbilityData abilityData = AbilityCapability.get(player);
        abilityData.tickCooldowns();
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityAPI.serverTick(serverPlayer);
        } else {
            abilityData.incrementUseElapsed();
            abilityData.tickEffectsClient();
        }
    }

    private static void reapplyPassives(ServerPlayer grantSourcePlayer, ServerPlayer targetPlayer) {
        AbilityData abilityData = AbilityCapability.get(grantSourcePlayer);
        Set<PassiveAbility> grantedPassives = abilityData.getGrantsBySource().values().stream()
                .flatMap(abilityLevels -> abilityLevels.keySet().stream())
                .filter(PassiveAbility.class::isInstance)
                .map(PassiveAbility.class::cast)
                .collect(Collectors.toSet());
        for (PassiveAbility passive : grantedPassives) {
            int rawLevel = abilityData.getEffectiveLevelIgnoringDisabled(passive);
            if (AbilityConfigs.isEnabled(passive) && AbilityAPI.isPassiveEnabled(grantSourcePlayer, passive)) {
                AbilityAPI.activatePassive(targetPlayer, passive, rawLevel);
            } else {
                AbilityAPI.removeAttributeGrants(targetPlayer, passive, rawLevel);
            }
        }
    }
}

package net.silvertide.player_abilities.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.network.AbilitySync;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class PlayerLifecycleHandler {
    private PlayerLifecycleHandler() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            reapplyPassives(serverPlayer, serverPlayer);
            AbilityAPI.getActiveEffects(serverPlayer).forEach((ability, effect) ->
                    ability.onEffectStart(serverPlayer, effect.getLevel()));
            AbilitySync.syncAllState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onDeathClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()
                && event.getEntity() instanceof ServerPlayer newPlayer
                && event.getOriginal() instanceof ServerPlayer originalPlayer) {
            reapplyPassives(originalPlayer, newPlayer);
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
            AbilityAPI.cancelCast(serverPlayer);
            AbilitySync.syncAllState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.cancelCast(serverPlayer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.DEATH, serverPlayer, event.getSource());
            AbilityAPI.cancelCast(serverPlayer);
            AbilityAPI.clearEffects(serverPlayer);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            AbilityAPI.recordKill(killer);
            AbilityAPI.fireTrigger(PlayerTriggers.KILL, killer, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            AbilityAPI.recordDamageTaken(victim, event.getNewDamage());
            AbilityAPI.fireTrigger(PlayerTriggers.DAMAGE_TAKEN, victim,
                    new PlayerTriggers.DamageTaken(event.getNewDamage(), event.getSource()));
            float healthAfter = victim.getHealth();
            float healthBefore = Math.min(victim.getMaxHealth(), healthAfter + event.getNewDamage());
            AbilityAPI.fireTrigger(PlayerTriggers.HEALTH_DROPPED, victim,
                    new PlayerTriggers.HealthChange(healthBefore, healthAfter, victim.getMaxHealth()));
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            AbilityAPI.fireTrigger(PlayerTriggers.DEALT_DAMAGE, attacker,
                    new PlayerTriggers.DamageDealt(event.getEntity(), event.getNewDamage()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getNewDamage() < serverPlayer.getHealth()) {
            return;
        }
        if (AbilityAPI.fireTrigger(PlayerTriggers.LETHAL_DAMAGE, serverPlayer,
                new PlayerTriggers.DamageTaken(event.getNewDamage(), event.getSource()))) {
            event.setNewDamage(0);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer trackedPlayer
                && event.getEntity() instanceof ServerPlayer receiver) {
            AbilitySync.syncAbilitiesTo(trackedPlayer, receiver);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        abilityData.tickCooldowns();
        if (player instanceof ServerPlayer serverPlayer) {
            AbilityAPI.serverTick(serverPlayer);
        } else {
            abilityData.incrementCastElapsed();
            abilityData.tickEffectsClient();
        }
    }

    private static void reapplyPassives(ServerPlayer grantSourcePlayer, ServerPlayer targetPlayer) {
        AbilityAPI.getGrantedLevels(grantSourcePlayer).forEach((ability, level) -> {
            if (ability instanceof PassiveAbility passive && AbilityAPI.isPassiveEnabled(grantSourcePlayer, passive)) {
                AbilityAPI.activatePassive(targetPlayer, passive, level);
            }
        });
    }
}

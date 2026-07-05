package net.silvertide.player_abilities.event;

import net.minecraft.server.level.ServerPlayer;
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
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.data.ActiveCast;
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
            AbilityAPI.cancelCast(serverPlayer);
            AbilityAPI.clearEffects(serverPlayer);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            AbilityAPI.recordKill(killer);
        }
    }

    @SubscribeEvent
    public static void onDamageTaken(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getNewDamage() > 0) {
            AbilityAPI.recordDamageTaken(serverPlayer, event.getNewDamage());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (event.getNewDamage() < serverPlayer.getHealth() + serverPlayer.getAbsorptionAmount()) {
            return;
        }
        if (AbilityAPI.triggerOnLethalDamage(serverPlayer, event.getNewDamage())) {
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
        AbilityData abilityData = event.getEntity().getData(AbilityAttachments.ABILITY_DATA);
        abilityData.tickCooldowns();
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.tickActiveCast(serverPlayer);
            AbilityAPI.tickEffects(serverPlayer);
        } else {
            abilityData.getActiveCast().ifPresent(ActiveCast::incrementElapsed);
            abilityData.tickEffectsClient();
        }
    }

    private static void reapplyPassives(ServerPlayer grantSourcePlayer, ServerPlayer targetPlayer) {
        AbilityAPI.getGrantedLevels(grantSourcePlayer).forEach((ability, level) -> {
            if (ability instanceof PassiveAbility passive) {
                passive.onActivated(targetPlayer, level);
            }
        });
    }
}

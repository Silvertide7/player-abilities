package net.silvertide.player_abilities.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.PlayerTriggers;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class PlayerTriggerHandler {
    private PlayerTriggerHandler() {
    }

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        boolean isCrit = event.getResult() == Event.Result.ALLOW
                || (event.getResult() == Event.Result.DEFAULT && event.isVanillaCritical());
        if (isCrit && event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.CRIT, serverPlayer, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.SHIELD_BLOCK, serverPlayer, event.getBlockedDamage());
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.FALL, serverPlayer, event.getDistance());
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.JUMP, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.BLOCK_BREAK, serverPlayer, event.getState());
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbilityAPI.fireTrigger(PlayerTriggers.WAKE_UP, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && event.getItem().getFoodProperties(serverPlayer) != null) {
            AbilityAPI.fireTrigger(PlayerTriggers.EAT, serverPlayer, event.getItem());
        }
    }

    @SubscribeEvent
    public static void onXpGain(PlayerXpEvent.XpChange event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getAmount() > 0) {
            AbilityAPI.fireTrigger(PlayerTriggers.XP_GAIN, serverPlayer, event.getAmount());
        }
    }
}

package net.silvertide.player_abilities.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.silvertide.player_abilities.api.AbilityTrigger;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.api.TriggeredAbility;

public final class BloodPriceAbility extends TriggeredAbility<PlayerTriggers.DamageTaken> {
    private static final int COOLDOWN_TICKS = 100;
    private static final int KILLS_TO_RECHARGE = 2;
    private static final float DAMAGE_TAKEN_TO_RECHARGE = 10.0f;
    private static final int STRENGTH_DURATION_TICKS = 200;

    @Override
    public AbilityTrigger<PlayerTriggers.DamageTaken> getTrigger() {
        return PlayerTriggers.DAMAGE_TAKEN;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public int getKillRequirement(int level) {
        return KILLS_TO_RECHARGE;
    }

    @Override
    public float getDamageTakenRequirement(int level) {
        return DAMAGE_TAKEN_TO_RECHARGE;
    }

    @Override
    public void onTrigger(ServerPlayer player, int level) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION_TICKS, 0));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4f, 1.6f);
    }
}

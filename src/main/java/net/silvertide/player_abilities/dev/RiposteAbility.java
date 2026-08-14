package net.silvertide.player_abilities.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.silvertide.player_abilities.api.AbilityTrigger;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.api.TriggeredAbility;

public final class RiposteAbility extends TriggeredAbility<Float> {
    private static final int COOLDOWN_TICKS = 80;
    private static final float MINIMUM_BLOCKED_DAMAGE = 4.0f;
    private static final int RESISTANCE_DURATION_TICKS = 120;

    @Override
    public AbilityTrigger<Float> getTrigger() {
        return PlayerTriggers.SHIELD_BLOCK;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean shouldTrigger(ServerPlayer player, int level, Float blockedDamage) {
        return blockedDamage >= MINIMUM_BLOCKED_DAMAGE;
    }

    @Override
    public void onTrigger(ServerPlayer player, int level) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_DURATION_TICKS, 0));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
    }
}

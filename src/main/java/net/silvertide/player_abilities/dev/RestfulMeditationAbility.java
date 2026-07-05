package net.silvertide.player_abilities.dev;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.AbilityUseType;

public final class RestfulMeditationAbility extends ActiveAbility {
    private static final int MAX_LEVEL = 3;
    private static final int COOLDOWN_TICKS = 200;
    private static final double MOVEMENT_BREAK_THRESHOLD_SQ = 0.25;
    private static final float HEAL_PER_PULSE = 1.0f;
    private static final int FOOD_PER_PULSE = 1;
    private static final float SATURATION_PER_PULSE = 1.0f;
    private static final int FULL_FOOD_LEVEL = 20;

    @Override
    public AbilityUseType getCastType() {
        return AbilityUseType.CHARGED;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getCastTicks(int level) {
        return byLevel(level, 200, 160, 100);
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean requiresStationary() {
        return true;
    }

    @Override
    public ResourceLocation getCategory() {
        return PlayerAbilities.id("restoration");
    }

    @Override
    public int getEffectDurationTicks(int level) {
        return byLevel(level, 600, 900, 1200);
    }

    @Override
    public void onEffectStart(ServerPlayer player, int level) {
        AbilityAPI.setEffectData(player, this, player.position());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    @Override
    public void onEffectTick(ServerPlayer player, int level, int remainingTicks) {
        if (AbilityAPI.getEffectData(player, this) instanceof Vec3 lockedPosition
                && player.position().distanceToSqr(lockedPosition) > MOVEMENT_BREAK_THRESHOLD_SQ) {
            AbilityAPI.removeEffect(player, this);
            return;
        }
        if (remainingTicks % byLevel(level, 40, 30, 20) == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(HEAL_PER_PULSE);
        }
        if (remainingTicks % byLevel(level, 60, 45, 30) == 0 && player.getFoodData().getFoodLevel() < FULL_FOOD_LEVEL) {
            player.getFoodData().eat(FOOD_PER_PULSE, SATURATION_PER_PULSE);
        }
    }

    @Override
    public void onEffectEnd(ServerPlayer player, int level, boolean expired) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.5f, 1.0f);
    }
}

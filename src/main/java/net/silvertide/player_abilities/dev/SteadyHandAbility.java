package net.silvertide.player_abilities.dev;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.silvertide.player_abilities.api.AbilityUseType;
import net.silvertide.player_abilities.api.ActiveAbility;

public final class SteadyHandAbility extends ActiveAbility {
    private static final int COOLDOWN_TICKS = 60;
    private static final int NIGHT_VISION_DURATION_TICKS = 400;

    @Override
    public AbilityUseType getUseType() {
        return AbilityUseType.INSTANT;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean canUse(ServerPlayer player, int level) {
        return player.isCrouching() && player.onGround();
    }

    @Override
    public Component getUseFailureMessage(ServerPlayer player, int level) {
        return Component.translatable("message.player_abilities_dev.steady_hand_needs_footing");
    }

    @Override
    public void onUse(ServerPlayer player, int level) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.6f, 1.0f);
    }
}

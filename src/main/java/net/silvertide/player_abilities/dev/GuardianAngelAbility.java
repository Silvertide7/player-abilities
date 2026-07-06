package net.silvertide.player_abilities.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.silvertide.player_abilities.api.AbilityTrigger;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.api.TriggeredAbility;

public final class GuardianAngelAbility extends TriggeredAbility<PlayerTriggers.DamageTaken> {
    private static final int COOLDOWN_TICKS = 1200;

    @Override
    public AbilityTrigger<PlayerTriggers.DamageTaken> getTrigger() {
        return PlayerTriggers.LETHAL_DAMAGE;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public void onTrigger(ServerPlayer player, int level) {
        player.setHealth(player.getMaxHealth());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}

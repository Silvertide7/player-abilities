package net.silvertide.player_abilities.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.silvertide.player_abilities.api.AbilityTrigger;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.api.TriggeredAbility;

public final class LastStandAbility extends TriggeredAbility<PlayerTriggers.DamageTaken> {
    private static final int COOLDOWN_TICKS = 600;
    private static final float HEALTH_RESTORED_ON_SURVIVAL = 4.0f;

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
        player.setHealth(HEALTH_RESTORED_ON_SURVIVAL);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.4f);
    }
}

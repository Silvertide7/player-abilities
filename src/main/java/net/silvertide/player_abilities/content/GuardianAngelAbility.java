package net.silvertide.player_abilities.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.silvertide.player_abilities.api.TriggeredAbility;

public final class GuardianAngelAbility extends TriggeredAbility {
    private static final int COOLDOWN_TICKS = 1200;

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean triggersOnLethalDamage(ServerPlayer player, int level, float incomingDamage) {
        return true;
    }

    @Override
    public void onTrigger(ServerPlayer player, int level) {
        player.setHealth(player.getMaxHealth());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}

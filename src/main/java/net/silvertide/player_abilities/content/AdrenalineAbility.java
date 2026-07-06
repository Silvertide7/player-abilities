package net.silvertide.player_abilities.content;

import net.minecraft.world.effect.MobEffects;
import net.silvertide.player_abilities.api.AbilityTrigger;
import net.silvertide.player_abilities.api.EffectGrant;
import net.silvertide.player_abilities.api.PlayerTriggers;
import net.silvertide.player_abilities.api.TriggeredAbility;

import java.util.List;

public final class AdrenalineAbility extends TriggeredAbility<PlayerTriggers.DamageTaken> {
    private static final int COOLDOWN_TICKS = 1200;
    private static final int SPEED_DURATION_TICKS = 200;

    @Override
    public AbilityTrigger<PlayerTriggers.DamageTaken> getTrigger() {
        return PlayerTriggers.DAMAGE_TAKEN;
    }

    @Override
    public int getCooldownTicks(int level) {
        return COOLDOWN_TICKS;
    }

    @Override
    public List<EffectGrant> getEffectGrants(int level) {
        return List.of(new EffectGrant(MobEffects.MOVEMENT_SPEED, SPEED_DURATION_TICKS, 0));
    }
}

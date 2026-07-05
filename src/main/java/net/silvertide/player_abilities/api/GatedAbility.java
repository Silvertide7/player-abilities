package net.silvertide.player_abilities.api;

import net.silvertide.player_abilities.config.AbilityConfig;

import java.util.Optional;

public abstract class GatedAbility extends Ability {
    public abstract int getCooldownTicks(int level);

    public int getKillRequirement(int level) {
        return 0;
    }

    public float getDamageTakenRequirement(int level) {
        return 0;
    }

    public Optional<AbilityConfig.PmmoUseRequirement> getDefaultPmmoUseRequirement() {
        return Optional.empty();
    }
}

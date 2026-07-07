package net.silvertide.player_abilities.api;

import org.jetbrains.annotations.Nullable;

public interface AbilityUse {
    ActiveAbility getAbility();

    int getLevel();

    int getElapsedTicks();

    int getTotalTicks();

    @Nullable
    Object getUseData();
}

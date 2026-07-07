package net.silvertide.player_abilities.api;

import org.jetbrains.annotations.Nullable;

public interface AbilityEffect {
    int getLevel();

    int getTotalTicks();

    int getRemainingTicks();

    @Nullable
    Object getEffectData();
}

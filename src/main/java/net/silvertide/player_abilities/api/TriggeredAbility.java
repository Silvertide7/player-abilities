package net.silvertide.player_abilities.api;

import net.minecraft.server.level.ServerPlayer;

public abstract class TriggeredAbility<T> extends GatedAbility {
    public abstract AbilityTrigger<T> getTrigger();

    public boolean shouldTrigger(ServerPlayer player, int level, T context) {
        return true;
    }

    public void onTrigger(ServerPlayer player, int level) {
    }
}

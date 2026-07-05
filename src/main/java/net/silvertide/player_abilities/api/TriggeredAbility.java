package net.silvertide.player_abilities.api;

import net.minecraft.server.level.ServerPlayer;

public abstract class TriggeredAbility extends GatedAbility {
    public abstract void onTrigger(ServerPlayer player, int level);

    public boolean triggersOnLethalDamage(ServerPlayer player, int level, float incomingDamage) {
        return false;
    }
}

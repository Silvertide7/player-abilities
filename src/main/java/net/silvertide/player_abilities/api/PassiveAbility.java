package net.silvertide.player_abilities.api;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public abstract class PassiveAbility extends Ability {
    public List<AttributeGrant> getAttributeGrants(int level) {
        return List.of();
    }

    public void onActivated(ServerPlayer player, int level) {
    }

    public void onDeactivated(ServerPlayer player, int level) {
    }
}

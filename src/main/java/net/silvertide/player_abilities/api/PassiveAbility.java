package net.silvertide.player_abilities.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;

public abstract class PassiveAbility extends Ability {
    public void onActivated(ServerPlayer player, int level) {
    }

    public void onDeactivated(ServerPlayer player, int level) {
    }

    public boolean canStandOnFluid(Player player, FluidState fluidState, int level) {
        return false;
    }
}

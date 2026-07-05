package net.silvertide.player_abilities.content;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.silvertide.player_abilities.api.PassiveAbility;

public final class WaterWalkingAbility extends PassiveAbility {
    @Override
    public boolean canStandOnFluid(Player player, FluidState fluidState, int level) {
        return fluidState.is(FluidTags.WATER) && !player.isShiftKeyDown();
    }
}

package net.silvertide.player_abilities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.silvertide.player_abilities.api.GatedAbility;

public class AbilityPerformedEvent extends AbilityEvent {
    public AbilityPerformedEvent(ServerPlayer player, GatedAbility ability, int level) {
        super(player, ability, level);
    }

    @Override
    public GatedAbility getAbility() {
        return (GatedAbility) super.getAbility();
    }
}

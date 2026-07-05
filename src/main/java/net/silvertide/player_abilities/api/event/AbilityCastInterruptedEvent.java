package net.silvertide.player_abilities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.silvertide.player_abilities.api.ActiveAbility;

public class AbilityCastInterruptedEvent extends AbilityEvent {
    public AbilityCastInterruptedEvent(ServerPlayer player, ActiveAbility ability, int level) {
        super(player, ability, level);
    }

    @Override
    public ActiveAbility getAbility() {
        return (ActiveAbility) super.getAbility();
    }
}

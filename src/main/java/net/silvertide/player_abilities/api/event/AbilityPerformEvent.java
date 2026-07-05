package net.silvertide.player_abilities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.silvertide.player_abilities.api.GatedAbility;

public class AbilityPerformEvent extends AbilityEvent implements ICancellableEvent {
    public AbilityPerformEvent(ServerPlayer player, GatedAbility ability, int level) {
        super(player, ability, level);
    }

    @Override
    public GatedAbility getAbility() {
        return (GatedAbility) super.getAbility();
    }
}

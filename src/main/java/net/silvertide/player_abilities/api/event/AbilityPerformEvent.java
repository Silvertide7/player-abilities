package net.silvertide.player_abilities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.silvertide.player_abilities.api.GatedAbility;

@Cancelable
public class AbilityPerformEvent extends AbilityEvent {
    public AbilityPerformEvent(ServerPlayer player, GatedAbility ability, int level) {
        super(player, ability, level);
    }

    @Override
    public GatedAbility getAbility() {
        return (GatedAbility) super.getAbility();
    }
}

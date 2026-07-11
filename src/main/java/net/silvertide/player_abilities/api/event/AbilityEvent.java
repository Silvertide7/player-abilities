package net.silvertide.player_abilities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.silvertide.player_abilities.api.Ability;

public abstract class AbilityEvent extends PlayerEvent {
    private final Ability ability;
    private final int level;

    protected AbilityEvent(ServerPlayer player, Ability ability, int level) {
        super(player);
        this.ability = ability;
        this.level = level;
    }

    public Ability getAbility() {
        return ability;
    }

    public int getLevel() {
        return level;
    }
}

package net.silvertide.player_abilities.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.silvertide.player_abilities.api.Ability;

public class AbilityGrantedEvent extends AbilityEvent {
    private final ResourceLocation source;

    public AbilityGrantedEvent(ServerPlayer player, Ability ability, int level, ResourceLocation source) {
        super(player, ability, level);
        this.source = source;
    }

    public ResourceLocation getSource() {
        return source;
    }
}

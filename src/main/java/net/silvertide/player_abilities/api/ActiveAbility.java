package net.silvertide.player_abilities.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public abstract class ActiveAbility extends GatedAbility {
    public abstract AbilityUseType getUseType();

    public int getUseTicks(int level) {
        return 0;
    }

    public boolean canUse(ServerPlayer player, int level) {
        return true;
    }

    public void onUse(ServerPlayer player, int level) {
    }

    public void onUseStart(ServerPlayer player, int level) {
    }

    public void onUseTick(ServerPlayer player, int level, int elapsedTicks) {
    }

    public void onUseComplete(ServerPlayer player, int level, boolean cancelled) {
    }

    public boolean isInterruptedByDamage() {
        return true;
    }

    public boolean requiresStationary() {
        return false;
    }

    @Nullable
    public Component getUseFailureMessage(ServerPlayer player, int level) {
        return null;
    }
}

package net.silvertide.player_abilities.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public abstract class ActiveAbility extends GatedAbility {
    public abstract AbilityUseType getUseType();

    public int getCastTicks(int level) {
        return 0;
    }

    public boolean canCast(ServerPlayer player, int level) {
        return true;
    }

    public void onCast(ServerPlayer player, int level) {
    }

    public void onCastStart(ServerPlayer player, int level) {
    }

    public void onCastTick(ServerPlayer player, int level, int elapsedTicks) {
    }

    public void onCastComplete(ServerPlayer player, int level, boolean cancelled) {
    }

    public boolean isInterruptedByDamage() {
        return true;
    }

    public boolean requiresStationary() {
        return false;
    }

    @Nullable
    public Component getCastFailureMessage(ServerPlayer player, int level) {
        return null;
    }
}

package net.silvertide.player_abilities.data;

import net.minecraft.world.phys.Vec3;
import net.silvertide.player_abilities.api.ActiveAbility;
import org.jetbrains.annotations.Nullable;

public final class ActiveUse {
    private final ActiveAbility ability;
    private final int level;
    private final int totalTicks;
    @Nullable
    private final Vec3 startPosition;
    private int elapsedTicks;
    private int lastHurtTime;
    private boolean completing;
    @Nullable
    private Object useData;

    ActiveUse(ActiveAbility ability, int level, int totalTicks, @Nullable Vec3 startPosition) {
        this.ability = ability;
        this.level = level;
        this.totalTicks = totalTicks;
        this.startPosition = startPosition;
    }

    public ActiveAbility getAbility() {
        return ability;
    }

    public int getLevel() {
        return level;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    @Nullable
    public Vec3 getStartPosition() {
        return startPosition;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    void incrementElapsed() {
        elapsedTicks++;
    }

    public int getLastHurtTime() {
        return lastHurtTime;
    }

    void setLastHurtTime(int lastHurtTime) {
        this.lastHurtTime = lastHurtTime;
    }

    public boolean isCompleting() {
        return completing;
    }

    void markCompleting() {
        completing = true;
    }

    @Nullable
    public Object getUseData() {
        return useData;
    }

    void setUseData(@Nullable Object useData) {
        this.useData = useData;
    }
}

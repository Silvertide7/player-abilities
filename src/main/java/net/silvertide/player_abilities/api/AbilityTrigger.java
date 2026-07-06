package net.silvertide.player_abilities.api;

public final class AbilityTrigger<T> {
    private final String name;
    private final boolean exclusive;

    public AbilityTrigger(String name, boolean exclusive) {
        this.name = name;
        this.exclusive = exclusive;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Override
    public String toString() {
        return "AbilityTrigger[" + name + "]";
    }
}

package net.silvertide.player_abilities.api;

import net.minecraft.world.effect.MobEffect;

public record EffectGrant(MobEffect effect, int durationTicks, int amplifier, boolean showParticles,
                          boolean showIcon) {
    public EffectGrant(MobEffect effect, int durationTicks, int amplifier) {
        this(effect, durationTicks, amplifier, true, true);
    }
}

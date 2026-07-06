package net.silvertide.player_abilities.api;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public record EffectGrant(Holder<MobEffect> effect, int durationTicks, int amplifier) {
}

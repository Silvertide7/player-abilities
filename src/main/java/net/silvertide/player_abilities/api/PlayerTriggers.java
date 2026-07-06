package net.silvertide.player_abilities.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PlayerTriggers {
    public static final AbilityTrigger<DamageTaken> LETHAL_DAMAGE = new AbilityTrigger<>("lethal_damage", true);
    public static final AbilityTrigger<DamageTaken> DAMAGE_TAKEN = new AbilityTrigger<>("damage_taken", false);
    public static final AbilityTrigger<DamageDealt> DEALT_DAMAGE = new AbilityTrigger<>("dealt_damage", false);
    public static final AbilityTrigger<HealthChange> HEALTH_DROPPED = new AbilityTrigger<>("health_dropped", false);
    public static final AbilityTrigger<LivingEntity> KILL = new AbilityTrigger<>("kill", false);
    public static final AbilityTrigger<DamageSource> DEATH = new AbilityTrigger<>("death", false);
    public static final AbilityTrigger<Entity> CRIT = new AbilityTrigger<>("crit", false);
    public static final AbilityTrigger<Float> SHIELD_BLOCK = new AbilityTrigger<>("shield_block", false);
    public static final AbilityTrigger<Float> FALL = new AbilityTrigger<>("fall", false);
    public static final AbilityTrigger<Void> JUMP = new AbilityTrigger<>("jump", false);
    public static final AbilityTrigger<Void> RESPAWN = new AbilityTrigger<>("respawn", false);
    public static final AbilityTrigger<BlockState> BLOCK_BREAK = new AbilityTrigger<>("block_break", false);
    public static final AbilityTrigger<Void> WAKE_UP = new AbilityTrigger<>("wake_up", false);
    public static final AbilityTrigger<ItemStack> EAT = new AbilityTrigger<>("eat", false);
    public static final AbilityTrigger<Integer> XP_GAIN = new AbilityTrigger<>("xp_gain", false);

    public record DamageTaken(float amount, DamageSource source) {
    }

    public record DamageDealt(LivingEntity target, float amount) {
    }

    public record HealthChange(float healthBefore, float healthAfter, float maxHealth) {
        public boolean droppedBelow(float healthFraction) {
            float threshold = maxHealth * healthFraction;
            return healthBefore > threshold && healthAfter <= threshold;
        }
    }

    private PlayerTriggers() {
    }
}

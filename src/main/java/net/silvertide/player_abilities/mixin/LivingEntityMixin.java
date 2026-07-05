package net.silvertide.player_abilities.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.silvertide.player_abilities.api.AbilityAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void player_abilities$passiveFluidWalking(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && AbilityAPI.canStandOnFluid(player, fluidState)) {
            cir.setReturnValue(true);
        }
    }
}

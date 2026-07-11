package net.silvertide.player_abilities.dev;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.AbilityRegistry;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DevAbilities {
    public static final String DEV_NAMESPACE = "player_abilities_dev";

    private DevAbilities() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(AbilityRegistry.ABILITY_REGISTRY_KEY, helper -> {
            helper.register(new ResourceLocation(DEV_NAMESPACE, "restful_meditation"),
                    new RestfulMeditationAbility());
            helper.register(new ResourceLocation(DEV_NAMESPACE, "swift_step"),
                    new SwiftStepAbility());
            helper.register(new ResourceLocation(DEV_NAMESPACE, "guardian_angel"),
                    new GuardianAngelAbility());
            helper.register(new ResourceLocation(DEV_NAMESPACE, "adrenaline"),
                    new AdrenalineAbility());
        });
    }
}

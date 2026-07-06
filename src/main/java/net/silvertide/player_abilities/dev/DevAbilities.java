package net.silvertide.player_abilities.dev;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.AbilityRegistry;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class DevAbilities {
    public static final String DEV_NAMESPACE = "player_abilities_dev";

    private DevAbilities() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(AbilityRegistry.ABILITY_REGISTRY_KEY, helper -> {
            helper.register(ResourceLocation.fromNamespaceAndPath(DEV_NAMESPACE, "restful_meditation"),
                    new RestfulMeditationAbility());
            helper.register(ResourceLocation.fromNamespaceAndPath(DEV_NAMESPACE, "swift_step"),
                    new SwiftStepAbility());
        });
    }
}

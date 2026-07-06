package net.silvertide.player_abilities.content;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;

public final class BuiltInAbilities {
    private static final DeferredRegister<Ability> ABILITIES =
            DeferredRegister.create(AbilityRegistry.ABILITY_REGISTRY_KEY, PlayerAbilities.MOD_ID);

    static {
        ABILITIES.register("guardian_angel", GuardianAngelAbility::new);
        ABILITIES.register("adrenaline", AdrenalineAbility::new);
    }

    private BuiltInAbilities() {
    }

    public static void register(IEventBus modEventBus) {
        ABILITIES.register(modEventBus);
    }
}

package net.silvertide.player_abilities.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.Optional;

public final class AbilityRegistry {
    public static final ResourceKey<Registry<Ability>> ABILITY_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(PlayerAbilities.MOD_ID, "abilities"));
    public static final Registry<Ability> ABILITIES = new RegistryBuilder<>(ABILITY_REGISTRY_KEY).create();

    private AbilityRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((NewRegistryEvent event) -> event.register(ABILITIES));
    }

    public static Optional<ActiveAbility> getActive(ResourceLocation abilityId) {
        return ABILITIES.get(abilityId) instanceof ActiveAbility active ? Optional.of(active) : Optional.empty();
    }

    public static Optional<GatedAbility> getGated(ResourceLocation abilityId) {
        return ABILITIES.get(abilityId) instanceof GatedAbility gated ? Optional.of(gated) : Optional.empty();
    }
}

package net.silvertide.player_abilities.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.Optional;
import java.util.function.Supplier;

public final class AbilityRegistry {
    public static final ResourceKey<Registry<Ability>> ABILITY_REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(PlayerAbilities.MOD_ID, "abilities"));

    private static final DeferredRegister<Ability> DEFERRED = DeferredRegister.create(ABILITY_REGISTRY_KEY, PlayerAbilities.MOD_ID);
    private static final Supplier<IForgeRegistry<Ability>> REGISTRY =
            DEFERRED.makeRegistry(() -> new RegistryBuilder<Ability>().disableSaving());

    private AbilityRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        DEFERRED.register(modEventBus);
    }

    public static IForgeRegistry<Ability> abilities() {
        return REGISTRY.get();
    }

    public static Optional<ActiveAbility> getActive(ResourceLocation abilityId) {
        return abilities().getValue(abilityId) instanceof ActiveAbility active ? Optional.of(active) : Optional.empty();
    }

    public static Optional<GatedAbility> getGated(ResourceLocation abilityId) {
        return abilities().getValue(abilityId) instanceof GatedAbility gated ? Optional.of(gated) : Optional.empty();
    }

    public static Optional<PassiveAbility> getPassive(ResourceLocation abilityId) {
        return abilities().getValue(abilityId) instanceof PassiveAbility passive ? Optional.of(passive) : Optional.empty();
    }
}

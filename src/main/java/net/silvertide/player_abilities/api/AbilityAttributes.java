package net.silvertide.player_abilities.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.player_abilities.PlayerAbilities;

public final class AbilityAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, PlayerAbilities.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> ABILITY_COOLDOWN = ATTRIBUTES.register("ability_cooldown",
            () -> new RangedAttribute("attribute.player_abilities.ability_cooldown", 1.0, 0.0, 10.0).setSyncable(true));

    private AbilityAttributes() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(AbilityAttributes::modifyEntityAttributes);
    }

    private static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ABILITY_COOLDOWN);
    }
}

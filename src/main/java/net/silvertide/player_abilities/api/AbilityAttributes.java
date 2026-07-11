package net.silvertide.player_abilities.api;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.player_abilities.PlayerAbilities;

public final class AbilityAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, PlayerAbilities.MOD_ID);

    public static final RegistryObject<Attribute> ABILITY_COOLDOWN = ATTRIBUTES.register("ability_cooldown",
            () -> new RangedAttribute("attribute.player_abilities.ability_cooldown", 1.0, 0.0, 10.0).setSyncable(true));

    public static final RegistryObject<Attribute> ABILITY_POWER = ATTRIBUTES.register("ability_power",
            () -> new RangedAttribute("attribute.player_abilities.ability_power", 1.0, 0.0, 10.0).setSyncable(true));

    private AbilityAttributes() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(AbilityAttributes::modifyEntityAttributes);
    }

    private static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ABILITY_COOLDOWN.get());
        event.add(EntityType.PLAYER, ABILITY_POWER.get());
    }
}

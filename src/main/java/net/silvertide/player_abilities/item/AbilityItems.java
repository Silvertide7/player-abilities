package net.silvertide.player_abilities.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.TriggeredAbility;

import java.util.Comparator;

public final class AbilityItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PlayerAbilities.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlayerAbilities.MOD_ID);

    public static final RegistryObject<AbilityBookItem> ABILITY_BOOK =
            ITEMS.register("ability_book", () -> new AbilityBookItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<CreativeModeTab> ABILITIES_TAB =
            TABS.register("abilities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.player_abilities.abilities"))
                    .icon(() -> new ItemStack(ABILITY_BOOK.get()))
                    .displayItems((parameters, output) -> AbilityRegistry.abilities().getValues().stream()
                            .sorted(Comparator.comparingInt(AbilityItems::kindOrder)
                                    .thenComparing(ability -> ability.getId().toString()))
                            .forEach(ability -> output.accept(AbilityBookItem.createFor(ability, 1))))
                    .build());

    private static int kindOrder(Ability ability) {
        if (ability instanceof TriggeredAbility) {
            return 2;
        }
        return ability instanceof PassiveAbility ? 1 : 0;
    }

    private AbilityItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}

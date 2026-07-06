package net.silvertide.player_abilities.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.TriggeredAbility;

import java.util.Comparator;

public final class AbilityItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlayerAbilities.MOD_ID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PlayerAbilities.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlayerAbilities.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AbilityBookContent>> ABILITY_BOOK_CONTENT =
            DATA_COMPONENTS.registerComponentType("ability_book_content", builder -> builder
                    .persistent(AbilityBookContent.CODEC)
                    .networkSynchronized(AbilityBookContent.STREAM_CODEC));

    public static final DeferredItem<AbilityBookItem> ABILITY_BOOK =
            ITEMS.registerItem("ability_book", AbilityBookItem::new, new Item.Properties().stacksTo(16));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ABILITIES_TAB =
            TABS.register("abilities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.player_abilities.abilities"))
                    .icon(() -> new ItemStack(ABILITY_BOOK.get()))
                    .displayItems((parameters, output) -> AbilityRegistry.ABILITIES.stream()
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
        DATA_COMPONENTS.register(modEventBus);
        TABS.register(modEventBus);
    }
}

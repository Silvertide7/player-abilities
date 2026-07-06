package net.silvertide.player_abilities.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.TriggeredAbility;
import net.silvertide.player_abilities.item.AbilityBookContent;
import net.silvertide.player_abilities.item.AbilityItems;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class AbilityBookItemModel {
    private AbilityBookItemModel() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(AbilityItems.ABILITY_BOOK.get(), PlayerAbilities.id("kind"),
                (stack, level, entity, seed) -> {
                    AbilityBookContent content = stack.get(AbilityItems.ABILITY_BOOK_CONTENT.get());
                    if (content == null) {
                        return 0.0f;
                    }
                    Ability ability = AbilityRegistry.ABILITIES.get(content.abilityId());
                    if (ability instanceof TriggeredAbility) {
                        return 2.0f;
                    }
                    return ability instanceof PassiveAbility ? 1.0f : 0.0f;
                }));
    }
}

package net.silvertide.player_abilities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.client.AbilityClientAPI;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;

import java.util.List;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class ClientAbilityInput {
    private static final Component NO_ABILITIES = Component.translatable("hud.player_abilities.no_abilities");

    private ClientAbilityInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        while (AbilityKeyMappings.USE.consumeClick()) {
            AbilityClientAPI.use();
        }
        while (AbilityKeyMappings.CYCLE.consumeClick()) {
            selectNext(abilityData);
        }
        while (AbilityKeyMappings.WHEEL.consumeClick()) {
            if (Minecraft.getInstance().screen == null) {
                if (abilityData.getGrantedActives().isEmpty()) {
                    player.displayClientMessage(NO_ABILITIES, true);
                } else {
                    Minecraft.getInstance().setScreen(new AbilityWheelScreen(player));
                }
            }
        }
        while (AbilityKeyMappings.BOOK.consumeClick()) {
            if (Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new AbilityBookScreen(player));
            }
        }
    }

    private static void selectNext(AbilityData abilityData) {
        List<ActiveAbility> grantedActives = abilityData.getGrantedActives();
        if (grantedActives.isEmpty()) {
            return;
        }
        int currentIndex = abilityData.getSelected().map(grantedActives::indexOf).orElse(-1);
        AbilityClientAPI.select(grantedActives.get((currentIndex + 1) % grantedActives.size()));
    }
}

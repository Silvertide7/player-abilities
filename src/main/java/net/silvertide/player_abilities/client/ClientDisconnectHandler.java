package net.silvertide.player_abilities.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.config.AbilityConfigs;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class ClientDisconnectHandler {
    private ClientDisconnectHandler() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AbilityConfigs.clearSyncedConfigs();
    }
}

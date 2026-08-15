package net.silvertide.player_abilities.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.config.AbilityConfigs;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class ClientDisconnectHandler {
    private ClientDisconnectHandler() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AbilityConfigs.clearSyncedConfigs();
    }
}

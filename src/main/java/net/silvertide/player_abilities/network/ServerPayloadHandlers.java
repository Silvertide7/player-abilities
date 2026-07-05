package net.silvertide.player_abilities.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;

public final class ServerPayloadHandlers {
    private ServerPayloadHandlers() {
    }

    public static void handleCast(CastAbilityPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            AbilityAPI.cast(player);
        }
    }

    public static void handleSelect(SelectAbilityPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        AbilityRegistry.getActive(payload.abilityId())
                .filter(abilityData::isGranted)
                .ifPresentOrElse(active -> {
                    AbilityAPI.cancelCast(player);
                    abilityData.setSelected(active);
                    AbilitySync.syncAbilities(player);
                }, () -> AbilitySync.syncAbilities(player));
    }
}

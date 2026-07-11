package net.silvertide.player_abilities.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.data.AbilityCapability;
import net.silvertide.player_abilities.data.AbilityData;

import java.util.function.Supplier;

public final class ServerPayloadHandlers {
    private ServerPayloadHandlers() {
    }

    public static void handleUse(UseAbilityPayload payload, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) {
            AbilityAPI.use(player);
        }
    }

    public static void handleTogglePassive(TogglePassivePayload payload, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }
        AbilityData abilityData = AbilityCapability.get(player);
        AbilityRegistry.getPassive(payload.abilityId())
                .filter(abilityData::isGranted)
                .ifPresent(passive -> AbilityAPI.setPassiveEnabled(player, passive, abilityData.isPassiveDisabled(passive)));
    }

    public static void handleSelect(SelectAbilityPayload payload, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }
        AbilityData abilityData = AbilityCapability.get(player);
        AbilityRegistry.getActive(payload.abilityId())
                .filter(abilityData::isGranted)
                .ifPresentOrElse(active -> {
                    AbilityAPI.cancelUse(player);
                    abilityData.setSelected(active);
                    AbilitySync.syncAbilities(player);
                }, () -> AbilitySync.syncAbilities(player));
    }
}

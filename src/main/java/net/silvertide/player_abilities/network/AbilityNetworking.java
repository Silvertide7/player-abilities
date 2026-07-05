package net.silvertide.player_abilities.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.player_abilities.PlayerAbilities;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class AbilityNetworking {
    private AbilityNetworking() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PlayerAbilities.MOD_ID).versioned("1");
        registrar.playToServer(CastAbilityPayload.TYPE, CastAbilityPayload.STREAM_CODEC, ServerPayloadHandlers::handleCast);
        registrar.playToServer(SelectAbilityPayload.TYPE, SelectAbilityPayload.STREAM_CODEC, ServerPayloadHandlers::handleSelect);
        registrar.playToClient(SyncAbilitiesPayload.TYPE, SyncAbilitiesPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncAbilities);
        registrar.playToClient(SyncCooldownPayload.TYPE, SyncCooldownPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncCooldown);
        registrar.playToClient(SyncCastStatePayload.TYPE, SyncCastStatePayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncCastState);
        registrar.playToClient(SyncEffectsPayload.TYPE, SyncEffectsPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncEffects);
        registrar.playToClient(SyncRequirementPayload.TYPE, SyncRequirementPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncRequirement);
        registrar.playToClient(TriggeredActivatedPayload.TYPE, TriggeredActivatedPayload.STREAM_CODEC, ClientPayloadHandlers::handleTriggeredActivated);
        registrar.playToClient(SyncAbilityConfigsPayload.TYPE, SyncAbilityConfigsPayload.STREAM_CODEC, ClientPayloadHandlers::handleSyncAbilityConfigs);
    }
}

package net.silvertide.player_abilities.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.silvertide.player_abilities.PlayerAbilities;

public final class AbilityNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PlayerAbilities.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private AbilityNetworking() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(UseAbilityPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UseAbilityPayload::encode).decoder(UseAbilityPayload::decode)
                .consumerMainThread(ServerPayloadHandlers::handleUse).add();
        CHANNEL.messageBuilder(SelectAbilityPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectAbilityPayload::encode).decoder(SelectAbilityPayload::decode)
                .consumerMainThread(ServerPayloadHandlers::handleSelect).add();
        CHANNEL.messageBuilder(TogglePassivePayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TogglePassivePayload::encode).decoder(TogglePassivePayload::decode)
                .consumerMainThread(ServerPayloadHandlers::handleTogglePassive).add();

        CHANNEL.messageBuilder(SyncAbilitiesPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAbilitiesPayload::encode).decoder(SyncAbilitiesPayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSyncAbilities(msg, ctx))).add();
        CHANNEL.messageBuilder(SyncCooldownPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCooldownPayload::encode).decoder(SyncCooldownPayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSyncCooldown(msg, ctx))).add();
        CHANNEL.messageBuilder(SyncUseStatePayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncUseStatePayload::encode).decoder(SyncUseStatePayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSyncUseState(msg, ctx))).add();
        CHANNEL.messageBuilder(SyncEffectsPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncEffectsPayload::encode).decoder(SyncEffectsPayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSyncEffects(msg, ctx))).add();
        CHANNEL.messageBuilder(SyncRequirementPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncRequirementPayload::encode).decoder(SyncRequirementPayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSyncRequirement(msg, ctx))).add();
        CHANNEL.messageBuilder(TriggeredActivatedPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TriggeredActivatedPayload::encode).decoder(TriggeredActivatedPayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleTriggeredActivated(msg, ctx))).add();
        CHANNEL.messageBuilder(SyncAbilityConfigsPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAbilityConfigsPayload::encode).decoder(SyncAbilityConfigsPayload::decode)
                .consumerMainThread((msg, ctx) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.handleSyncAbilityConfigs(msg, ctx))).add();
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayersTrackingEntityAndSelf(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}

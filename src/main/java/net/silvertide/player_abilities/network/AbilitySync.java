package net.silvertide.player_abilities.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.Cooldown;
import net.silvertide.player_abilities.api.GatedAbility;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.data.ActiveEffect;
import net.silvertide.player_abilities.data.RequirementProgress;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AbilitySync {
    private AbilitySync() {
    }

    public static void syncAbilities(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, buildAbilitiesPayload(player));
    }

    public static void syncAbilitiesTo(ServerPlayer trackedPlayer, ServerPlayer receiver) {
        PacketDistributor.sendToPlayer(receiver, buildAbilitiesPayload(trackedPlayer));
    }

    public static void syncCooldown(ServerPlayer player, GatedAbility ability, Cooldown cooldown) {
        PacketDistributor.sendToPlayer(player, new SyncCooldownPayload(ability.getId(), cooldown));
    }

    public static void syncCastState(ServerPlayer player, @Nullable ActiveAbility castingAbility, int level, int totalTicks) {
        PacketDistributor.sendToPlayer(player, new SyncCastStatePayload(
                Optional.ofNullable(castingAbility).map(Ability::getId), level, totalTicks));
    }

    public static void syncEffects(ServerPlayer player) {
        Map<ResourceLocation, ActiveEffect> effects = player.getData(AbilityAttachments.ABILITY_DATA)
                .getActiveEffects().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));
        PacketDistributor.sendToPlayer(player, new SyncEffectsPayload(effects));
    }

    public static void syncRequirementProgress(ServerPlayer player, GatedAbility ability, @Nullable RequirementProgress progress) {
        PacketDistributor.sendToPlayer(player, new SyncRequirementPayload(ability.getId(), Optional.ofNullable(progress)));
    }

    public static void syncTriggered(ServerPlayer player, GatedAbility ability, int cooldownTicks) {
        PacketDistributor.sendToPlayer(player, new TriggeredActivatedPayload(ability.getId(), cooldownTicks));
    }

    public static void syncAllState(ServerPlayer player) {
        syncAbilities(player);
        syncEffects(player);
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        abilityData.getCooldowns().forEach((ability, cooldown) -> syncCooldown(player, ability, cooldown));
        abilityData.getRequirementProgressMap().forEach((ability, progress) -> syncRequirementProgress(player, ability, progress));
    }

    private static SyncAbilitiesPayload buildAbilitiesPayload(ServerPlayer player) {
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        Map<ResourceLocation, Integer> grantedLevels = abilityData.getGrantedLevels().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));
        return new SyncAbilitiesPayload(player.getId(), grantedLevels,
                abilityData.getSelected().map(Ability::getId));
    }
}

package net.silvertide.player_abilities.compat.pmmo;

import harmonised.pmmo.api.APIUtils;
import harmonised.pmmo.api.events.XpEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.event.AbilityPerformEvent;
import net.silvertide.player_abilities.config.AbilityConfig;
import net.silvertide.player_abilities.config.AbilityConfigs;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PmmoCompat {
    static final String SOURCE_PATH_PREFIX = "pmmo/";

    private record LeveledAbility(Ability ability, int level) {
    }

    private PmmoCompat() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(PmmoCompat::onAbilityPerform);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PmmoCompat::onXpEvent);
        NeoForge.EVENT_BUS.addListener(PmmoCompat::onDatapackSync);
    }

    private static void onAbilityPerform(AbilityPerformEvent event) {
        Optional<AbilityConfig.PmmoUseRequirement> useRequirement = AbilityConfigs.pmmoUseRequirement(event.getAbility());
        if (useRequirement.isEmpty()) {
            return;
        }
        AbilityConfig.PmmoUseRequirement requirement = useRequirement.get();
        int requiredLevel = requirement.level().resolve(event.getLevel());
        if (APIUtils.getLevel(requirement.skill(), event.getEntity()) < requiredLevel) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable(
                    "message.player_abilities.pmmo_requirement", requirement.skill(), requiredLevel), true);
        }
    }

    private static void onXpEvent(XpEvent event) {
        if (!event.isLevelUp() && !event.isLevelDown()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduleReconcile(player);
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(PmmoCompat::scheduleReconcile);
    }

    private static void scheduleReconcile(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (!player.hasDisconnected()) {
                reconcile(player);
            }
        }));
    }

    private static void reconcile(ServerPlayer player) {
        Map<ResourceLocation, LeveledAbility> expectedBySource = new HashMap<>();
        AbilityRegistry.ABILITIES.forEach(ability -> {
            for (AbilityConfig.PmmoGrant grant : AbilityConfigs.pmmoGrants(ability)) {
                if (APIUtils.getLevel(grant.skill(), player) >= grant.pmmoLevel()) {
                    expectedBySource.merge(sourceFor(grant.skill(), ability),
                            new LeveledAbility(ability, grant.abilityLevel()),
                            (existing, incoming) -> incoming.level() > existing.level() ? incoming : existing);
                }
            }
        });
        AbilityAPI.getGrantsBySource(player).forEach((source, abilityLevels) -> {
            if (!source.getNamespace().equals(PlayerAbilities.MOD_ID)
                    || !source.getPath().startsWith(SOURCE_PATH_PREFIX)) {
                return;
            }
            LeveledAbility expected = expectedBySource.get(source);
            for (Ability ability : abilityLevels.keySet()) {
                if (expected == null || expected.ability() != ability) {
                    AbilityAPI.revoke(player, source, ability);
                }
            }
        });
        expectedBySource.forEach((source, leveledAbility) ->
                AbilityAPI.grant(player, source, leveledAbility.ability(), leveledAbility.level()));
    }

    private static ResourceLocation sourceFor(String skill, Ability ability) {
        String sanitizedSkill = skill.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_")
                + "-" + Integer.toHexString(skill.hashCode());
        return PlayerAbilities.id(SOURCE_PATH_PREFIX + sanitizedSkill
                + "/" + ability.getId().getNamespace() + "/" + ability.getId().getPath());
    }
}

package net.silvertide.player_abilities.compat.puffish_skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.config.AbilityConfig;
import net.silvertide.player_abilities.config.AbilityConfigs;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PuffishSkillsCompat {
    static final String SOURCE_PATH_PREFIX = "puffish_skills/";

    private record LeveledAbility(Ability ability, int level) {
    }

    private PuffishSkillsCompat() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(PuffishSkillsCompat::onDatapackSync);
        SkillsAPI.registerSkillUnlockEvent(PuffishSkillsCompat::onSkillChanged);
        SkillsAPI.registerSkillLockEvent(PuffishSkillsCompat::onSkillChanged);
    }

    private static void onSkillChanged(ServerPlayer player, ResourceLocation category, String skill) {
        SkillKey changedKey = new SkillKey(category, skill);
        for (Ability ability : AbilityRegistry.ABILITIES) {
            for (AbilityConfig.PuffishGrant grant : AbilityConfigs.puffishGrants(ability)) {
                if (new SkillKey(grant.category(), grant.skill()).equals(changedKey)) {
                    reconcile(player);
                    return;
                }
            }
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(PuffishSkillsCompat::scheduleReconcile);
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
            for (AbilityConfig.PuffishGrant grant : AbilityConfigs.puffishGrants(ability)) {
                if (hasUnlocked(player, new SkillKey(grant.category(), grant.skill()))) {
                    expectedBySource.merge(sourceFor(grant, ability),
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

    private static boolean hasUnlocked(ServerPlayer player, SkillKey skillKey) {
        Optional<Category> category = SkillsAPI.getCategory(skillKey.category());
        if (category.isEmpty()) {
            PlayerAbilities.LOGGER.warn("player_abilities puffish_grants references unknown Pufferfish category '{}'", skillKey.category());
            return false;
        }
        Optional<Skill> skill = category.get().getSkill(skillKey.skill());
        if (skill.isEmpty()) {
            PlayerAbilities.LOGGER.warn("player_abilities puffish_grants references unknown Pufferfish skill '{}' in category '{}'",
                    skillKey.skill(), skillKey.category());
            return false;
        }
        return skill.get().getState(player) == Skill.State.UNLOCKED;
    }

    private static ResourceLocation sourceFor(AbilityConfig.PuffishGrant grant, Ability ability) {
        String sanitizedSkill = grant.skill().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_")
                + "-" + Integer.toHexString(grant.skill().hashCode());
        return PlayerAbilities.id(SOURCE_PATH_PREFIX + grant.category().getNamespace()
                + "/" + grant.category().getPath() + "/" + sanitizedSkill
                + "/" + ability.getId().getNamespace() + "/" + ability.getId().getPath());
    }
}

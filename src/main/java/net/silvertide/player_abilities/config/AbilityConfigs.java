package net.silvertide.player_abilities.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.AttributeGrant;
import net.silvertide.player_abilities.api.EffectGrant;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.GatedAbility;
import net.silvertide.player_abilities.network.SyncAbilityConfigsPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class AbilityConfigs extends SimpleJsonResourceReloadListener {
    public static final String DATAPACK_DIRECTORY = "ability_config";
    private static final AbilityConfigs INSTANCE = new AbilityConfigs();

    private static volatile Map<Ability, AbilityConfig> configs = Map.of();

    private AbilityConfigs() {
        super(new Gson(), DATAPACK_DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> rawEntries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Ability, AbilityConfig> parsed = new HashMap<>();
        rawEntries.forEach((abilityId, json) -> {
            Ability ability = AbilityRegistry.ABILITIES.get(abilityId);
            if (ability == null) {
                PlayerAbilities.LOGGER.warn("Skipping ability_config for unknown ability {}", abilityId);
                return;
            }
            AbilityConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> PlayerAbilities.LOGGER.warn(
                            "Skipping malformed ability_config for {}: {}", abilityId, error))
                    .ifPresent(config -> parsed.put(ability, config));
        });
        configs = Map.copyOf(parsed);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncAbilityConfigsPayload payload = buildSyncPayload();
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private static SyncAbilityConfigsPayload buildSyncPayload() {
        Map<ResourceLocation, AbilityConfig> byId = new HashMap<>();
        configs.forEach((ability, config) -> byId.put(ability.getId(), config));
        return new SyncAbilityConfigsPayload(byId);
    }

    public static void replaceFromSync(Map<ResourceLocation, AbilityConfig> configsById) {
        Map<Ability, AbilityConfig> resolved = new HashMap<>();
        configsById.forEach((abilityId, config) -> {
            Ability ability = AbilityRegistry.ABILITIES.get(abilityId);
            if (ability != null) {
                resolved.put(ability, config);
            }
        });
        configs = Map.copyOf(resolved);
    }

    public static Map<Ability, AbilityConfig> all() {
        return configs;
    }

    public static int cooldownTicks(GatedAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.cooldownTicks().isPresent()
                ? config.cooldownTicks().get().resolve(level)
                : ability.getCooldownTicks(level);
    }

    public static int killRequirement(GatedAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.killRequirement().isPresent()
                ? config.killRequirement().get().resolve(level)
                : ability.getKillRequirement(level);
    }

    public static float damageTakenRequirement(GatedAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.damageTakenRequirement().isPresent()
                ? config.damageTakenRequirement().get().resolve(level)
                : ability.getDamageTakenRequirement(level);
    }

    public static int useTicks(ActiveAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.useTicks().isPresent()
                ? config.useTicks().get().resolve(level)
                : ability.getUseTicks(level);
    }

    public static int effectDurationTicks(GatedAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.effectDurationTicks().isPresent()
                ? config.effectDurationTicks().get().resolve(level)
                : ability.getEffectDurationTicks(level);
    }

    public static List<AttributeGrant> attributeGrants(PassiveAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.attributeGrants().isPresent()
                ? config.attributeGrants().get().stream().map(grantConfig -> grantConfig.resolve(level)).toList()
                : ability.getAttributeGrants(level);
    }

    public static List<EffectGrant> effectGrants(GatedAbility ability, int level) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.effectGrants().isPresent()
                ? config.effectGrants().get().stream().map(grantConfig -> grantConfig.resolve(level)).toList()
                : ability.getEffectGrants(level);
    }

    public static int maxLevel(Ability ability) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.maxLevel().isPresent()
                ? config.maxLevel().get()
                : ability.getMaxLevel();
    }

    public static ResourceLocation category(Ability ability) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.category().isPresent()
                ? config.category().get()
                : ability.getCategory();
    }

    public static Optional<AbilityConfig.PmmoUseRequirement> pmmoUseRequirement(GatedAbility ability) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.pmmoUseRequirement().isPresent()
                ? config.pmmoUseRequirement()
                : ability.getDefaultPmmoUseRequirement();
    }

    public static List<AbilityConfig.PmmoGrant> pmmoGrants(Ability ability) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.pmmoGrants().isPresent()
                ? config.pmmoGrants().get()
                : ability.getDefaultPmmoGrants();
    }

    public static List<AbilityConfig.PuffishGrant> puffishGrants(Ability ability) {
        AbilityConfig config = configs.get(ability);
        return config != null && config.puffishGrants().isPresent()
                ? config.puffishGrants().get()
                : ability.getDefaultPuffishGrants();
    }
}

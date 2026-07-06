package net.silvertide.player_abilities.api;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.config.AbilityConfig;

import java.util.List;

public abstract class Ability {
    public static final ResourceLocation GENERAL_CATEGORY = PlayerAbilities.id("general");

    private ResourceLocation cachedId;
    private String cachedDescriptionId;

    public ResourceLocation getId() {
        if (cachedId == null) {
            cachedId = AbilityRegistry.ABILITIES.getKey(this);
        }
        return cachedId;
    }

    public String getDescriptionId() {
        if (cachedDescriptionId == null) {
            cachedDescriptionId = Util.makeDescriptionId("ability", getId());
        }
        return cachedDescriptionId;
    }

    public int getMaxLevel() {
        return 1;
    }

    public ResourceLocation getCategory() {
        return GENERAL_CATEGORY;
    }

    public List<AbilityConfig.PmmoGrant> getDefaultPmmoGrants() {
        return List.of();
    }

    public List<AbilityConfig.PuffishGrant> getDefaultPuffishGrants() {
        return List.of();
    }

    public void onEffectStart(ServerPlayer player, int level) {
    }

    public void onEffectTick(ServerPlayer player, int level, int remainingTicks) {
    }

    public void onEffectEnd(ServerPlayer player, int level, boolean expired) {
    }

    protected static int byLevel(int level, int... values) {
        return values[Mth.clamp(level, 1, values.length) - 1];
    }

    protected static float byLevel(int level, float... values) {
        return values[Mth.clamp(level, 1, values.length) - 1];
    }
}

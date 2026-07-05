package net.silvertide.player_abilities.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.Cooldown;
import net.silvertide.player_abilities.config.AbilityClientConfig;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;
import net.silvertide.player_abilities.data.ActiveCast;
import net.silvertide.player_abilities.data.ActiveEffect;
import net.silvertide.player_abilities.data.RequirementProgress;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class AbilityHudOverlay implements LayeredDraw.Layer {
    private static final int MARGIN = 4;
    private static final int CELL_SIZE = 22;
    private static final int ICON_SIZE = 20;
    private static final int CAST_BAR_WIDTH = 110;
    private static final int CAST_BAR_HEIGHT = 11;
    private static final int CELL_BACKGROUND = 0x90101018;
    private static final int CELL_BORDER = 0xB04A4A5A;
    private static final int COOLDOWN_OVERLAY = 0xC0202028;
    private static final int CONTEXTUAL_TICKS = 300;
    private static final int CAST_BAR_BACKGROUND = 0x90101018;
    private static final int CAST_BAR_FILL = 0xE050C8B4;
    private static final int TEXT_PRIMARY = 0xFFFFFF;
    private static final int TEXT_MUTED = 0xA0A0B0;
    private static final float TEXT_SCALE = 0.8f;
    private static final int STATUS_LINE_OFFSET = 10;
    private static final int READY_NOTICE_COLOR = 0xFF66DD66;
    private static final Component READY = Component.translatable("hud.player_abilities.ready");

    private final Map<Ability, EffectLine> effectLineCache = new HashMap<>();
    private int lastCooldownHalfSeconds = -1;
    private String lastCooldownText = "";
    private Ability lastRequirementAbility;
    private long lastRequirementKey = -1;
    private String lastRequirementText = "";
    private int contextualTicksRemaining;
    private int lastContextualTick = -1;
    private ActiveAbility lastContextualSelected;

    private record EffectLine(int seconds, Component text) {
    }

    private AbilityHudOverlay() {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(PlayerAbilities.id("ability_hud"), new AbilityHudOverlay());
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        ActiveAbility ability = abilityData.getSelected().orElse(null);

        abilityData.getActiveCast().ifPresent(cast -> renderCastBar(guiGraphics, minecraft, cast));
        renderNotifications(guiGraphics, minecraft);

        AbilityClientConfig.HudDisplay displayMode = AbilityClientConfig.HUD_DISPLAY.get();
        if (displayMode == AbilityClientConfig.HudDisplay.NEVER) {
            return;
        }
        boolean windowed = displayMode == AbilityClientConfig.HudDisplay.CONTEXTUAL
                || displayMode == AbilityClientConfig.HudDisplay.MINIMIZE;
        int cellX = MARGIN;
        int cellY = guiGraphics.guiHeight() - MARGIN - CELL_SIZE;
        if (windowed && !isContextuallyVisible(player, abilityData, ability)) {
            if (displayMode == AbilityClientConfig.HudDisplay.MINIMIZE && ability != null) {
                renderIconCell(guiGraphics, minecraft, ability, cellX, cellY);
            }
            return;
        }
        int effectsBottom = ability != null ? cellY - 4 : guiGraphics.guiHeight() - MARGIN;
        renderEffects(guiGraphics, minecraft, abilityData, effectsBottom);
        if (ability != null) {
            renderSelectedCell(guiGraphics, minecraft, abilityData, ability, cellX, cellY);
        }
    }

    private boolean isContextuallyVisible(LocalPlayer player, AbilityData abilityData, ActiveAbility selected) {
        if (player.tickCount != lastContextualTick) {
            lastContextualTick = player.tickCount;
            boolean activity = selected != lastContextualSelected
                    || abilityData.getActiveCast().isPresent()
                    || (selected != null && (abilityData.isOnCooldown(selected)
                    || abilityData.getRequirementProgress(selected).isPresent()));
            lastContextualSelected = selected;
            if (activity) {
                contextualTicksRemaining = CONTEXTUAL_TICKS;
            } else if (contextualTicksRemaining > 0) {
                contextualTicksRemaining--;
            }
        }
        return contextualTicksRemaining > 0;
    }

    private void renderIconCell(GuiGraphics guiGraphics, Minecraft minecraft, ActiveAbility ability, int cellX, int cellY) {
        guiGraphics.fill(cellX - 1, cellY - 1, cellX + CELL_SIZE + 1, cellY + CELL_SIZE + 1, CELL_BORDER);
        guiGraphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, CELL_BACKGROUND);
        AbilityIcons.render(guiGraphics, minecraft.font, ability,
                cellX + (CELL_SIZE - ICON_SIZE) / 2, cellY + (CELL_SIZE - ICON_SIZE) / 2, ICON_SIZE);
    }

    private void renderCooldownShade(GuiGraphics guiGraphics, AbilityData abilityData, ActiveAbility ability, int cellX, int cellY) {
        abilityData.getCooldown(ability).ifPresent(activeCooldown -> {
            float remainingFraction = activeCooldown.totalTicks() <= 0 ? 0.0f
                    : (float) activeCooldown.remainingTicks() / activeCooldown.totalTicks();
            int overlayHeight = Mth.ceil(CELL_SIZE * remainingFraction);
            guiGraphics.fill(cellX, cellY + CELL_SIZE - overlayHeight, cellX + CELL_SIZE, cellY + CELL_SIZE, COOLDOWN_OVERLAY);
        });
    }

    private void renderSelectedCell(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData abilityData,
                                    ActiveAbility ability, int cellX, int cellY) {
        renderIconCell(guiGraphics, minecraft, ability, cellX, cellY);
        Optional<Cooldown> cooldown = abilityData.getCooldown(ability);
        renderCooldownShade(guiGraphics, abilityData, ability, cellX, cellY);
        int textX = cellX + CELL_SIZE + 4;
        String requirementsText = requirementsText(abilityData, ability);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textX, cellY + 4, 0);
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        guiGraphics.drawString(minecraft.font,
                AbilityIcons.nameWithLevel(ability, abilityData.getEffectiveLevel(ability)), 0, 0, TEXT_PRIMARY);
        if (cooldown.isPresent()) {
            guiGraphics.drawString(minecraft.font, cooldownText(cooldown.get()) + requirementsText, 0, STATUS_LINE_OFFSET, TEXT_MUTED);
        } else if (!requirementsText.isEmpty()) {
            guiGraphics.drawString(minecraft.font, requirementsText.strip(), 0, STATUS_LINE_OFFSET, TEXT_MUTED);
        } else {
            guiGraphics.drawString(minecraft.font, READY, 0, STATUS_LINE_OFFSET, TEXT_MUTED);
        }
        guiGraphics.pose().popPose();
    }

    private String requirementsText(AbilityData abilityData, ActiveAbility ability) {
        Optional<RequirementProgress> progress = abilityData.getRequirementProgress(ability);
        if (progress.isEmpty()) {
            lastRequirementAbility = ability;
            lastRequirementKey = -1;
            lastRequirementText = "";
            return "";
        }
        int level = abilityData.getEffectiveLevel(ability);
        int killRequirement = AbilityConfigs.killRequirement(ability, level);
        float damageRequirement = AbilityConfigs.damageTakenRequirement(ability, level);
        int kills = Math.min(progress.get().getKills(), killRequirement);
        int damage = (int) Math.min(progress.get().getDamageTaken(), damageRequirement);
        long key = (long) kills << 20 | damage;
        if (ability != lastRequirementAbility || key != lastRequirementKey) {
            lastRequirementAbility = ability;
            lastRequirementKey = key;
            StringBuilder text = new StringBuilder();
            if (killRequirement > 0 && kills < killRequirement) {
                text.append(' ').append(kills).append('/').append(killRequirement).append(" kills");
            }
            if (damageRequirement > 0 && progress.get().getDamageTaken() < damageRequirement) {
                text.append(' ').append(damage).append('/').append((int) damageRequirement).append(" dmg");
            }
            lastRequirementText = text.toString();
        }
        return lastRequirementText;
    }

    private String cooldownText(Cooldown cooldown) {
        int halfSeconds = cooldown.remainingTicks() / 2;
        if (halfSeconds != lastCooldownHalfSeconds) {
            lastCooldownHalfSeconds = halfSeconds;
            lastCooldownText = String.format(Locale.ROOT, "%.1fs", cooldown.remainingTicks() / 20.0f);
        }
        return lastCooldownText;
    }

    private void renderNotifications(GuiGraphics guiGraphics, Minecraft minecraft) {
        int bannerY = guiGraphics.guiHeight() / 3;
        for (AbilityNotifications.Notice notice : AbilityNotifications.activatedNotices()) {
            AbilityIcons.render(guiGraphics, minecraft.font, notice.ability(), MARGIN, bannerY, ICON_SIZE);
            int textX = MARGIN + ICON_SIZE + 4;
            guiGraphics.drawString(minecraft.font,
                    Component.translatable("hud.player_abilities.triggered_activated", AbilityIcons.name(notice.ability())),
                    textX, bannerY + 2, TEXT_PRIMARY);
            guiGraphics.drawString(minecraft.font,
                    Component.translatable("hud.player_abilities.cooldown_duration",
                            String.format(Locale.ROOT, "%.0f", notice.cooldownTicks() / 20.0f)),
                    textX, bannerY + 2 + minecraft.font.lineHeight, TEXT_MUTED);
            bannerY += ICON_SIZE + 6;
        }
        int readyY = guiGraphics.guiHeight() - MARGIN - CELL_SIZE - 6 - minecraft.font.lineHeight;
        for (AbilityNotifications.Notice notice : AbilityNotifications.readyNotices()) {
            guiGraphics.drawString(minecraft.font,
                    Component.translatable("hud.player_abilities.ability_ready", AbilityIcons.name(notice.ability())),
                    MARGIN, readyY, READY_NOTICE_COLOR);
            readyY -= minecraft.font.lineHeight + 2;
        }
    }

    private void renderCastBar(GuiGraphics guiGraphics, Minecraft minecraft, ActiveCast cast) {
        int totalTicks = cast.getTotalTicks();
        if (totalTicks <= 0) {
            return;
        }
        float progress = Math.min(1.0f, (float) cast.getElapsedTicks() / totalTicks);
        int remainingTicks = Math.max(0, totalTicks - cast.getElapsedTicks());
        int centerX = guiGraphics.guiWidth() / 2;
        int barX = centerX - CAST_BAR_WIDTH / 2;
        int barY = guiGraphics.guiHeight() / 2 + guiGraphics.guiHeight() / 8;
        guiGraphics.drawCenteredString(minecraft.font,
                AbilityIcons.nameWithLevel(cast.getAbility(), cast.getLevel()),
                centerX, barY - minecraft.font.lineHeight - 3, TEXT_PRIMARY);
        guiGraphics.fill(barX - 1, barY - 1, barX + CAST_BAR_WIDTH + 1, barY + CAST_BAR_HEIGHT + 1, CELL_BORDER);
        guiGraphics.fill(barX, barY, barX + CAST_BAR_WIDTH, barY + CAST_BAR_HEIGHT, CAST_BAR_BACKGROUND);
        guiGraphics.fill(barX, barY, barX + Mth.ceil(CAST_BAR_WIDTH * progress), barY + CAST_BAR_HEIGHT, CAST_BAR_FILL);
        String remainingText = String.format(Locale.ROOT, "%.1fs", remainingTicks / 20.0f);
        guiGraphics.drawCenteredString(minecraft.font, remainingText, centerX,
                barY + (CAST_BAR_HEIGHT - minecraft.font.lineHeight) / 2 + 1, TEXT_PRIMARY);
    }

    private void renderEffects(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData abilityData, int bottomY) {
        Map<Ability, ActiveEffect> effects = abilityData.getActiveEffects();
        if (effects.isEmpty()) {
            if (!effectLineCache.isEmpty()) {
                effectLineCache.clear();
            }
            return;
        }
        effectLineCache.keySet().retainAll(effects.keySet());
        int lineY = bottomY - minecraft.font.lineHeight;
        for (Map.Entry<Ability, ActiveEffect> entry : effects.entrySet()) {
            guiGraphics.drawString(minecraft.font,
                    effectLine(entry.getKey(), entry.getValue()), MARGIN, lineY, TEXT_MUTED);
            lineY -= minecraft.font.lineHeight + 1;
        }
    }

    private Component effectLine(Ability ability, ActiveEffect effect) {
        int totalSeconds = effect.getRemainingTicks() / 20;
        EffectLine cached = effectLineCache.get(ability);
        if (cached == null || cached.seconds() != totalSeconds) {
            String timer = String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60);
            cached = new EffectLine(totalSeconds, AbilityIcons.name(ability).copy().append(" " + timer));
            effectLineCache.put(ability, cached);
        }
        return cached.text();
    }
}

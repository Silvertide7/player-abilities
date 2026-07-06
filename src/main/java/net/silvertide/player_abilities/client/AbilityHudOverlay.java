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
import net.silvertide.player_abilities.data.ActiveUse;
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
    private static final int USE_BAR_WIDTH = 110;
    private static final int USE_BAR_HEIGHT = 11;
    private static final int CELL_BACKGROUND = 0x90101018;
    private static final int CELL_BORDER = 0xB04A4A5A;
    private static final int COOLDOWN_OVERLAY = 0xC0202028;
    private static final int CONTEXTUAL_TICKS = 300;
    private static final int USE_BAR_BACKGROUND = 0x90101018;
    private static final int USE_BAR_FILL = 0xE050C8B4;
    private static final int TEXT_PRIMARY = 0xFFFFFF;
    private static final int TEXT_MUTED = 0xA0A0B0;
    private static final float TEXT_SCALE = 0.8f;
    private static final float EFFECT_TEXT_SCALE = 0.7f;
    private static final int EFFECT_ICON_SIZE = 10;
    private static final int STATUS_LINE_OFFSET = 10;
    private static final int READY_NOTICE_COLOR = 0xFF66DD66;
    private static final Component READY = Component.translatable("hud.player_abilities.ready");

    private final Map<Ability, EffectLine> effectLineCache = new HashMap<>();
    private int lastCooldownHalfSeconds = -1;
    private String lastCooldownText = "";
    private int lastUseHalfSeconds = -1;
    private String lastUseText = "";
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

        abilityData.getActiveUse().ifPresent(use -> renderUseBar(guiGraphics, minecraft, use));
        renderNotifications(guiGraphics, minecraft);

        AbilityClientConfig.HudDisplay displayMode = AbilityClientConfig.HUD_DISPLAY.get();
        if (displayMode == AbilityClientConfig.HudDisplay.NEVER) {
            return;
        }
        boolean windowed = displayMode == AbilityClientConfig.HudDisplay.CONTEXTUAL
                || displayMode == AbilityClientConfig.HudDisplay.MINIMIZE;
        AbilityClientConfig.HudPosition position = AbilityClientConfig.HUD_POSITION.get();
        boolean rightSide = position.isRight();
        boolean topSide = position.isTop();
        int cellX = rightSide ? guiGraphics.guiWidth() - MARGIN - CELL_SIZE : MARGIN;
        int cellY = topSide ? MARGIN : guiGraphics.guiHeight() - MARGIN - CELL_SIZE;
        if (windowed && !isContextuallyVisible(player, abilityData, ability)) {
            if (displayMode == AbilityClientConfig.HudDisplay.MINIMIZE && ability != null) {
                renderIconCell(guiGraphics, minecraft, ability, cellX, cellY);
                renderCooldownShade(guiGraphics, abilityData.getCooldown(ability), cellX, cellY);
            }
            return;
        }
        int stackCursor;
        if (ability != null) {
            stackCursor = topSide ? cellY + CELL_SIZE + 4 : cellY - 4;
        } else {
            stackCursor = topSide ? MARGIN : guiGraphics.guiHeight() - MARGIN;
        }
        stackCursor = renderReadyNotices(guiGraphics, minecraft, stackCursor, rightSide, topSide);
        renderEffects(guiGraphics, minecraft, abilityData, stackCursor, rightSide, topSide);
        if (ability != null) {
            renderSelectedCell(guiGraphics, minecraft, abilityData, ability, cellX, cellY, rightSide);
        }
    }

    private boolean isContextuallyVisible(LocalPlayer player, AbilityData abilityData, ActiveAbility selected) {
        if (player.tickCount < lastContextualTick) {
            lastContextualTick = -1;
            lastContextualSelected = null;
            contextualTicksRemaining = 0;
        }
        if (player.tickCount != lastContextualTick) {
            lastContextualTick = player.tickCount;
            boolean activity = selected != lastContextualSelected || abilityData.getActiveUse().isPresent();
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

    private void renderCooldownShade(GuiGraphics guiGraphics, Optional<Cooldown> cooldown, int cellX, int cellY) {
        cooldown.ifPresent(activeCooldown -> {
            float remainingFraction = activeCooldown.totalTicks() <= 0 ? 0.0f
                    : (float) activeCooldown.remainingTicks() / activeCooldown.totalTicks();
            int overlayHeight = Mth.ceil(CELL_SIZE * remainingFraction);
            guiGraphics.fill(cellX, cellY + CELL_SIZE - overlayHeight, cellX + CELL_SIZE, cellY + CELL_SIZE, COOLDOWN_OVERLAY);
        });
    }

    private void renderSelectedCell(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData abilityData,
                                    ActiveAbility ability, int cellX, int cellY, boolean rightSide) {
        renderIconCell(guiGraphics, minecraft, ability, cellX, cellY);
        Optional<Cooldown> cooldown = abilityData.getCooldown(ability);
        renderCooldownShade(guiGraphics, cooldown, cellX, cellY);
        int textAnchorX = rightSide ? cellX - 4 : cellX + CELL_SIZE + 4;
        String requirementsText = requirementsText(abilityData, ability);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textAnchorX, cellY + 4, 0);
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        drawAligned(guiGraphics, minecraft,
                AbilityIcons.nameWithLevel(ability, abilityData.getEffectiveLevel(ability)), 0, TEXT_PRIMARY, rightSide);
        if (cooldown.isPresent()) {
            String statusLine = requirementsText.isEmpty() ? cooldownText(cooldown.get())
                    : cooldownText(cooldown.get()) + " " + requirementsText;
            drawAligned(guiGraphics, minecraft, statusLine, STATUS_LINE_OFFSET, TEXT_MUTED, rightSide);
        } else if (!requirementsText.isEmpty()) {
            drawAligned(guiGraphics, minecraft, requirementsText, STATUS_LINE_OFFSET, TEXT_MUTED, rightSide);
        } else {
            drawAligned(guiGraphics, minecraft, READY, STATUS_LINE_OFFSET, TEXT_MUTED, rightSide);
        }
        guiGraphics.pose().popPose();
    }

    private void drawAligned(GuiGraphics guiGraphics, Minecraft minecraft, Component text, int y, int color, boolean rightSide) {
        guiGraphics.drawString(minecraft.font, text, rightSide ? -minecraft.font.width(text) : 0, y, color);
    }

    private void drawAligned(GuiGraphics guiGraphics, Minecraft minecraft, String text, int y, int color, boolean rightSide) {
        guiGraphics.drawString(minecraft.font, text, rightSide ? -minecraft.font.width(text) : 0, y, color);
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
            lastRequirementText = AbilityIcons.requirementFragment(progress.get(), killRequirement, damageRequirement);
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
            guiGraphics.drawString(minecraft.font, notice.title(), textX, bannerY + 2, TEXT_PRIMARY);
            guiGraphics.drawString(minecraft.font, notice.subtitle(), textX, bannerY + 2 + minecraft.font.lineHeight, TEXT_MUTED);
            bannerY += ICON_SIZE + 6;
        }
    }

    private int renderReadyNotices(GuiGraphics guiGraphics, Minecraft minecraft, int stackCursor,
                                   boolean rightSide, boolean topSide) {
        for (AbilityNotifications.Notice notice : AbilityNotifications.readyNotices()) {
            int lineY = topSide ? stackCursor : stackCursor - minecraft.font.lineHeight;
            int lineX = rightSide ? guiGraphics.guiWidth() - MARGIN - minecraft.font.width(notice.title()) : MARGIN;
            guiGraphics.drawString(minecraft.font, notice.title(), lineX, lineY, READY_NOTICE_COLOR);
            stackCursor += (topSide ? 1 : -1) * (minecraft.font.lineHeight + 2);
        }
        return stackCursor;
    }

    private void renderUseBar(GuiGraphics guiGraphics, Minecraft minecraft, ActiveUse use) {
        int totalTicks = use.getTotalTicks();
        if (totalTicks <= 0) {
            return;
        }
        float progress = Math.min(1.0f, (float) use.getElapsedTicks() / totalTicks);
        int remainingTicks = Math.max(0, totalTicks - use.getElapsedTicks());
        int centerX = guiGraphics.guiWidth() / 2;
        int barX = centerX - USE_BAR_WIDTH / 2;
        int barY = guiGraphics.guiHeight() / 2 + guiGraphics.guiHeight() / 8;
        guiGraphics.drawCenteredString(minecraft.font,
                AbilityIcons.nameWithLevel(use.getAbility(), use.getLevel()),
                centerX, barY - minecraft.font.lineHeight - 3, TEXT_PRIMARY);
        guiGraphics.fill(barX - 1, barY - 1, barX + USE_BAR_WIDTH + 1, barY + USE_BAR_HEIGHT + 1, CELL_BORDER);
        guiGraphics.fill(barX, barY, barX + USE_BAR_WIDTH, barY + USE_BAR_HEIGHT, USE_BAR_BACKGROUND);
        guiGraphics.fill(barX, barY, barX + Mth.ceil(USE_BAR_WIDTH * progress), barY + USE_BAR_HEIGHT, USE_BAR_FILL);
        int useHalfSeconds = remainingTicks / 2;
        if (useHalfSeconds != lastUseHalfSeconds) {
            lastUseHalfSeconds = useHalfSeconds;
            lastUseText = String.format(Locale.ROOT, "%.1fs", remainingTicks / 20.0f);
        }
        guiGraphics.drawCenteredString(minecraft.font, lastUseText, centerX,
                barY + (USE_BAR_HEIGHT - minecraft.font.lineHeight) / 2 + 1, TEXT_PRIMARY);
    }

    private void renderEffects(GuiGraphics guiGraphics, Minecraft minecraft, AbilityData abilityData,
                               int stackCursor, boolean rightSide, boolean topSide) {
        Map<Ability, ActiveEffect> effects = abilityData.getActiveEffects();
        if (effects.isEmpty()) {
            if (!effectLineCache.isEmpty()) {
                effectLineCache.clear();
            }
            return;
        }
        effectLineCache.keySet().retainAll(effects.keySet());
        for (Map.Entry<Ability, ActiveEffect> entry : effects.entrySet()) {
            int lineY = topSide ? stackCursor : stackCursor - EFFECT_ICON_SIZE;
            int iconX = rightSide ? guiGraphics.guiWidth() - MARGIN - EFFECT_ICON_SIZE : MARGIN;
            AbilityIcons.render(guiGraphics, minecraft.font, entry.getKey(), iconX, lineY, EFFECT_ICON_SIZE);
            Component text = effectLine(entry.getKey(), entry.getValue());
            float textX = rightSide ? iconX - 2 - minecraft.font.width(text) * EFFECT_TEXT_SCALE
                    : iconX + EFFECT_ICON_SIZE + 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(textX, lineY + 2, 0);
            guiGraphics.pose().scale(EFFECT_TEXT_SCALE, EFFECT_TEXT_SCALE, 1.0f);
            guiGraphics.drawString(minecraft.font, text, 0, 0, TEXT_MUTED);
            guiGraphics.pose().popPose();
            stackCursor += (topSide ? 1 : -1) * (EFFECT_ICON_SIZE + 2);
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

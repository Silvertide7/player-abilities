package net.silvertide.player_abilities.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.GatedAbility;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.TriggeredAbility;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AbilityBookScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_PADDING = 10;
    private static final int ICON_SIZE = 16;
    private static final int ROW_GAP = 6;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_PADDING = 8;
    private static final int PANEL_BACKGROUND = 0xD8101018;
    private static final int PANEL_BORDER = 0xB04A4A5A;
    private static final int TAB_SELECTED_BG = 0xC03A3A4A;
    private static final int TAB_UNSELECTED_BG = 0x80181820;
    private static final int HEADER_COLOR = 0xFF50C8B4;
    private static final int TEXT_PRIMARY = 0xFFFFFF;
    private static final int TEXT_MUTED = 0xA0A0B0;
    private static final int STATUS_PENDING = 0xFFCC66;
    private static final int MAX_DISPLAYED_LEVEL = 100;
    private static final int TOOLTIP_WIDTH = 180;
    private static final Component READY = Component.translatable("hud.player_abilities.ready");

    private enum Tab {
        ACTIVE(ActiveAbility.class, "gui.player_abilities.section_active"),
        PASSIVE(PassiveAbility.class, "gui.player_abilities.section_passive"),
        TRIGGERED(TriggeredAbility.class, "gui.player_abilities.section_triggered");

        private final Class<? extends Ability> kind;
        private final String labelKey;

        Tab(Class<? extends Ability> kind, String labelKey) {
            this.kind = kind;
            this.labelKey = labelKey;
        }
    }

    private record TabButton(Tab tab, Component label, int leftX, int rightX) {
    }

    private sealed interface Row {
        record Header(Component title) implements Row {
        }

        record Entry(Ability ability, int level) implements Row {
        }
    }

    private final AbilityData abilityData;
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private Map<Ability, Integer> grantedLevels = Map.of();
    private int selectedTab;
    private double scrollOffset;
    private int contentHeight;
    private int panelX;
    private int panelTop;
    private int panelBottom;
    private int tabTop;
    private int contentTop;

    public AbilityBookScreen(LocalPlayer player) {
        super(Component.translatable("gui.player_abilities.book_title"));
        this.abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelTop = 24;
        panelBottom = height - 24;
        tabTop = panelTop + PANEL_PADDING;
        contentTop = tabTop + TAB_HEIGHT + 4;
        grantedLevels = abilityData.getGrantedLevels();
        tabButtons.clear();
        int tabX = panelX + PANEL_PADDING;
        for (Tab tab : Tab.values()) {
            if (grantedLevels.keySet().stream().noneMatch(tab.kind::isInstance)) {
                continue;
            }
            Component label = Component.translatable(tab.labelKey);
            int tabWidth = font.width(label) + TAB_PADDING * 2;
            tabButtons.add(new TabButton(tab, label, tabX, tabX + tabWidth));
            tabX += tabWidth + 2;
        }
        selectedTab = Mth.clamp(selectedTab, 0, Math.max(0, tabButtons.size() - 1));
        buildRows();
    }

    private void buildRows() {
        rows.clear();
        scrollOffset = 0;
        if (tabButtons.isEmpty()) {
            contentHeight = 0;
            return;
        }
        Tab tab = tabButtons.get(selectedTab).tab();
        Map<ResourceLocation, List<Map.Entry<Ability, Integer>>> byCategory = new HashMap<>();
        grantedLevels.entrySet().stream()
                .filter(entry -> tab.kind.isInstance(entry.getKey()))
                .forEach(entry -> byCategory
                        .computeIfAbsent(AbilityConfigs.category(entry.getKey()), key -> new ArrayList<>())
                        .add(entry));
        List<ResourceLocation> categories = new ArrayList<>(byCategory.keySet());
        categories.sort(Comparator.comparing(AbilityIcons::categoryLabel, String.CASE_INSENSITIVE_ORDER));
        for (ResourceLocation category : categories) {
            rows.add(new Row.Header(Component.literal(AbilityIcons.categoryLabel(category))));
            List<Map.Entry<Ability, Integer>> entries = byCategory.get(category);
            entries.sort(Comparator.comparing(entry -> AbilityIcons.name(entry.getKey()).getString(),
                    String.CASE_INSENSITIVE_ORDER));
            for (Map.Entry<Ability, Integer> entry : entries) {
                rows.add(new Row.Entry(entry.getKey(), entry.getValue()));
            }
        }
        contentHeight = rows.stream().mapToInt(this::rowHeight).sum();
    }

    private int rowHeight(Row row) {
        if (row instanceof Row.Header) {
            return font.lineHeight + 8;
        }
        return Math.max(ICON_SIZE, font.lineHeight) + ROW_GAP;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(panelX - 1, panelTop - 1, panelX + PANEL_WIDTH + 1, panelBottom + 1, PANEL_BORDER);
        guiGraphics.fill(panelX, panelTop, panelX + PANEL_WIDTH, panelBottom, PANEL_BACKGROUND);
        guiGraphics.drawCenteredString(font, title, width / 2, panelTop - 14, TEXT_PRIMARY);
        if (tabButtons.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("hud.player_abilities.no_abilities"),
                    width / 2, (panelTop + panelBottom) / 2, TEXT_MUTED);
            return;
        }
        for (int i = 0; i < tabButtons.size(); i++) {
            TabButton tabButton = tabButtons.get(i);
            boolean selected = i == selectedTab;
            guiGraphics.fill(tabButton.leftX(), tabTop, tabButton.rightX(), tabTop + TAB_HEIGHT,
                    selected ? TAB_SELECTED_BG : TAB_UNSELECTED_BG);
            guiGraphics.drawCenteredString(font, tabButton.label(),
                    (tabButton.leftX() + tabButton.rightX()) / 2, tabTop + (TAB_HEIGHT - font.lineHeight) / 2 + 1,
                    selected ? TEXT_PRIMARY : TEXT_MUTED);
        }
        int viewBottom = panelBottom - PANEL_PADDING;
        int viewHeight = viewBottom - contentTop;
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - viewHeight));
        guiGraphics.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, viewBottom);
        int rowY = contentTop - (int) scrollOffset;
        Row.Entry hoveredEntry = null;
        for (Row row : rows) {
            int rowHeight = rowHeight(row);
            if (rowY + rowHeight >= contentTop && rowY <= viewBottom) {
                renderRow(guiGraphics, row, panelX + PANEL_PADDING, rowY);
                if (row instanceof Row.Entry entry && mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                        && mouseY >= Math.max(rowY, contentTop) && mouseY < Math.min(rowY + rowHeight, viewBottom)) {
                    hoveredEntry = entry;
                }
            }
            rowY += rowHeight;
        }
        guiGraphics.disableScissor();
        if (hoveredEntry != null) {
            guiGraphics.renderTooltip(font, buildTooltip(hoveredEntry), mouseX, mouseY);
        }
    }

    private List<FormattedCharSequence> buildTooltip(Row.Entry entry) {
        Ability ability = entry.ability();
        int level = entry.level();
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(AbilityIcons.nameWithLevel(ability, level).getVisualOrderText());
        AbilityIcons.description(ability).ifPresent(description ->
                lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH)));
        int maxLevel = AbilityConfigs.maxLevel(ability);
        lines.add(Component.translatable("gui.player_abilities.tooltip_level",
                        level, maxLevel > MAX_DISPLAYED_LEVEL ? "-" : String.valueOf(maxLevel))
                .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        lines.add(Component.translatable(kindKey(ability)).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        lines.add(Component.literal(AbilityIcons.categoryLabel(AbilityConfigs.category(ability)))
                .withStyle(ChatFormatting.AQUA).getVisualOrderText());
        if (ability instanceof GatedAbility gated) {
            int cooldownTicks = AbilityConfigs.cooldownTicks(gated, level);
            if (cooldownTicks > 0) {
                lines.add(Component.translatable("gui.player_abilities.tooltip_cooldown",
                        String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0f)).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            }
            int killRequirement = AbilityConfigs.killRequirement(gated, level);
            if (killRequirement > 0) {
                lines.add(Component.translatable("gui.player_abilities.tooltip_kill_requirement", killRequirement)
                        .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            }
            float damageRequirement = AbilityConfigs.damageTakenRequirement(gated, level);
            if (damageRequirement > 0) {
                lines.add(Component.translatable("gui.player_abilities.tooltip_damage_requirement", (int) damageRequirement)
                        .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            }
        }
        return lines;
    }

    private String kindKey(Ability ability) {
        if (ability instanceof TriggeredAbility) {
            return "gui.player_abilities.section_triggered";
        }
        if (ability instanceof PassiveAbility) {
            return "gui.player_abilities.section_passive";
        }
        return "gui.player_abilities.section_active";
    }

    private void renderRow(GuiGraphics guiGraphics, Row row, int rowX, int rowY) {
        if (row instanceof Row.Header header) {
            guiGraphics.drawString(font, header.title(), rowX, rowY + 4, HEADER_COLOR);
            return;
        }
        Row.Entry entry = (Row.Entry) row;
        AbilityIcons.render(guiGraphics, font, entry.ability(), rowX, rowY, ICON_SIZE);
        int textX = rowX + ICON_SIZE + 6;
        guiGraphics.drawString(font, AbilityIcons.nameWithLevel(entry.ability(), entry.level()), textX, rowY + 4, TEXT_PRIMARY);
        if (entry.ability() instanceof GatedAbility gated) {
            String status = statusText(gated, entry.level());
            if (status.isEmpty()) {
                guiGraphics.drawString(font, READY, rowX + PANEL_WIDTH - PANEL_PADDING * 2 - font.width(READY), rowY + 4, TEXT_MUTED);
            } else {
                guiGraphics.drawString(font, status, rowX + PANEL_WIDTH - PANEL_PADDING * 2 - font.width(status), rowY + 4, STATUS_PENDING);
            }
        }
    }

    private String statusText(GatedAbility ability, int level) {
        StringBuilder status = new StringBuilder();
        abilityData.getCooldown(ability).ifPresent(cooldown ->
                status.append(String.format(Locale.ROOT, "%.0fs", cooldown.remainingTicks() / 20.0f)));
        abilityData.getRequirementProgress(ability).ifPresent(progress -> {
            int requiredKills = AbilityConfigs.killRequirement(ability, level);
            float requiredDamage = AbilityConfigs.damageTakenRequirement(ability, level);
            if (requiredKills > 0 && progress.getKills() < requiredKills) {
                appendStatus(status, Math.min(progress.getKills(), requiredKills) + "/" + requiredKills + " kills");
            }
            if (requiredDamage > 0 && progress.getDamageTaken() < requiredDamage) {
                appendStatus(status, (int) progress.getDamageTaken() + "/" + (int) requiredDamage + " dmg");
            }
        });
        return status.toString();
    }

    private static void appendStatus(StringBuilder status, String part) {
        if (!status.isEmpty()) {
            status.append(' ');
        }
        status.append(part);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= tabTop && mouseY <= tabTop + TAB_HEIGHT) {
            for (int i = 0; i < tabButtons.size(); i++) {
                TabButton tabButton = tabButtons.get(i);
                if (mouseX >= tabButton.leftX() && mouseX <= tabButton.rightX()) {
                    if (i != selectedTab) {
                        selectedTab = i;
                        buildRows();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * (font.lineHeight + 1) * 3;
        return true;
    }
}

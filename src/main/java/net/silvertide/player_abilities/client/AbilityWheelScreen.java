package net.silvertide.player_abilities.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.client.AbilityClientAPI;
import net.silvertide.player_abilities.config.AbilityClientConfig;
import net.silvertide.player_abilities.config.AbilityConfigs;
import net.silvertide.player_abilities.data.AbilityCapability;
import net.silvertide.player_abilities.data.AbilityData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class AbilityWheelScreen extends Screen {
    private static final int ABILITIES_PER_PAGE = 10;
    private static final float INNER_RADIUS = 34.0f;
    private static final float OUTER_RADIUS = 78.0f;
    private static final float HUB_RADIUS = 27.0f;
    private static final int HUB_ICON_SIZE = 24;
    private static final float SECTOR_GAP_RADIANS = 0.035f;
    private static final float SUBDIVISION_RADIANS = 0.15f;
    private static final int MIN_TICKS_BEFORE_RELEASE_SELECTS = 3;
    private static final int SCROLL_PAGE_COOLDOWN_TICKS = 4;
    private static final int COOLDOWN_MARKER_COLOR = 0xE0FFCC66;
    private static final float[] SECTOR_COLOR = {0.06f, 0.06f, 0.09f, 0.60f};
    private static final float[] HOVERED_COLOR = {0.18f, 0.72f, 0.63f, 0.55f};
    private static final float[] SELECTED_RING_COLOR = {0.31f, 0.78f, 0.71f, 0.90f};
    private static final float[] HUB_COLOR = {0.04f, 0.04f, 0.07f, 0.70f};
    private static final Component NO_ABILITIES = Component.translatable("hud.player_abilities.no_abilities");
    private static final float PAGE_ARROW_MARGIN = 16.0f;
    private static final float PAGE_ARROW_HALF_WIDTH = 5.0f;
    private static final float PAGE_ARROW_HALF_HEIGHT = 7.0f;
    private static final float PAGE_ARROW_HITBOX_HALF = 11.0f;
    private static final float[] PAGE_ARROW_COLOR = {0.63f, 0.66f, 0.69f, 0.85f};
    private static final float[] PAGE_ARROW_HOVERED_COLOR = {0.18f, 0.72f, 0.63f, 1.0f};

    private static int pageIndex;

    private record WheelPage(String label, List<ActiveAbility> abilities) {
    }

    private final List<WheelPage> pages = new ArrayList<>();
    private String pageIndicator = "";
    private int hoveredIndex = -1;
    private int ticksOpen;
    private int lastScrollPageTick = -SCROLL_PAGE_COOLDOWN_TICKS;

    public AbilityWheelScreen(LocalPlayer player) {
        super(Component.empty());
        List<ActiveAbility> grantedActives = AbilityCapability.get(player).getGrantedActives();
        if (AbilityClientConfig.WHEEL_GROUP_BY_CATEGORY.get()) {
            Map<ResourceLocation, List<ActiveAbility>> byCategory = new TreeMap<>();
            for (ActiveAbility ability : grantedActives) {
                byCategory.computeIfAbsent(AbilityConfigs.category(ability), key -> new ArrayList<>()).add(ability);
            }
            byCategory.forEach((category, abilities) -> addChunkedPages(AbilityIcons.categoryLabel(category), abilities));
        } else {
            addChunkedPages("", grantedActives);
        }
        pageIndex = pages.isEmpty() ? 0 : Mth.clamp(pageIndex, 0, pages.size() - 1);
        updatePageIndicator();
    }

    private void addChunkedPages(String label, List<ActiveAbility> abilities) {
        for (int fromIndex = 0; fromIndex < abilities.size(); fromIndex += ABILITIES_PER_PAGE) {
            pages.add(new WheelPage(label,
                    List.copyOf(abilities.subList(fromIndex, Math.min(fromIndex + ABILITIES_PER_PAGE, abilities.size())))));
        }
    }

    private int pageCount() {
        return pages.size();
    }

    private List<ActiveAbility> currentPage() {
        return pages.isEmpty() ? List.of() : pages.get(pageIndex).abilities();
    }

    private void updatePageIndicator() {
        if (pages.isEmpty()) {
            pageIndicator = "";
            return;
        }
        String label = pages.get(pageIndex).label();
        String position = pages.size() > 1 ? (pageIndex + 1) + "/" + pages.size() : "";
        pageIndicator = label.isEmpty() ? position
                : position.isEmpty() ? label : label + " " + position;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticksOpen++;
    }

    @Override
    public void removed() {
        ClientAbilityInput.suppressWheelReopenUntilKeyReleased();
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            onClose();
            return;
        }
        AbilityData abilityData = AbilityCapability.get(player);
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        List<ActiveAbility> pageAbilities = currentPage();
        boolean multiplePages = pageCount() > 1;
        int hoveredArrow = multiplePages ? hoveredArrowDirection(mouseX, mouseY, centerX, centerY) : 0;
        hoveredIndex = hoveredArrow != 0 ? -1
                : computeHoveredIndex(pageAbilities.size(), mouseX - centerX, mouseY - centerY);
        ActiveAbility selected = abilityData.getSelected().orElse(null);
        VertexConsumer buffer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = guiGraphics.pose().last().pose();
        drawSectors(buffer, pose, centerX, centerY, pageAbilities, selected);
        drawHubDisc(buffer, pose, centerX, centerY);
        if (multiplePages) {
            drawPageArrow(buffer, pose, centerX - OUTER_RADIUS - PAGE_ARROW_MARGIN, centerY, false, hoveredArrow < 0);
            drawPageArrow(buffer, pose, centerX + OUTER_RADIUS + PAGE_ARROW_MARGIN, centerY, true, hoveredArrow > 0);
        }
        guiGraphics.bufferSource().endBatch(RenderType.gui());
        drawSectorContents(guiGraphics, minecraft, centerX, centerY, pageAbilities, abilityData);
        drawHubContents(guiGraphics, minecraft, centerX, centerY, selected);
        if (hoveredIndex >= 0 && hoveredIndex < pageAbilities.size()) {
            ActiveAbility hovered = pageAbilities.get(hoveredIndex);
            guiGraphics.drawCenteredString(minecraft.font,
                    AbilityIcons.nameWithLevel(hovered, abilityData.getEffectiveLevel(hovered)),
                    (int) centerX, (int) (centerY - OUTER_RADIUS - 16), 0xFFFFFF);
        }
        if (!pageIndicator.isEmpty()) {
            guiGraphics.drawCenteredString(minecraft.font, pageIndicator,
                    (int) centerX, (int) (centerY + OUTER_RADIUS + 8), 0xA0A0B0);
        }
    }

    private int computeHoveredIndex(int sectorCount, float deltaX, float deltaY) {
        if (sectorCount == 0 || deltaX * deltaX + deltaY * deltaY < INNER_RADIUS * INNER_RADIUS) {
            return -1;
        }
        float sectorRadians = (float) (Math.PI * 2) / sectorCount;
        float angle = (float) Math.atan2(deltaY, deltaX) + Mth.HALF_PI + sectorRadians / 2.0f;
        angle = (angle % ((float) Math.PI * 2) + (float) Math.PI * 2) % ((float) Math.PI * 2);
        return (int) (angle / sectorRadians) % sectorCount;
    }

    private void drawSectors(VertexConsumer buffer, Matrix4f pose, float centerX, float centerY,
                             List<ActiveAbility> pageAbilities, ActiveAbility selected) {
        int sectorCount = pageAbilities.size();
        if (sectorCount == 0) {
            return;
        }
        float sectorRadians = (float) (Math.PI * 2) / sectorCount;
        for (int sector = 0; sector < sectorCount; sector++) {
            float beginRadians = sector * sectorRadians - Mth.HALF_PI - sectorRadians / 2.0f + SECTOR_GAP_RADIANS;
            float endRadians = (sector + 1) * sectorRadians - Mth.HALF_PI - sectorRadians / 2.0f - SECTOR_GAP_RADIANS;
            float[] color = sector == hoveredIndex ? HOVERED_COLOR : SECTOR_COLOR;
            drawArcBand(buffer, pose, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, beginRadians, endRadians, color, color[3] * 0.35f);
            if (pageAbilities.get(sector).equals(selected)) {
                drawArcBand(buffer, pose, centerX, centerY, OUTER_RADIUS + 1.5f, OUTER_RADIUS + 4.0f,
                        beginRadians, endRadians, SELECTED_RING_COLOR, SELECTED_RING_COLOR[3]);
            }
        }
    }

    private void drawArcBand(VertexConsumer buffer, Matrix4f pose, float centerX, float centerY,
                             float innerRadius, float outerRadius, float beginRadians, float endRadians,
                             float[] color, float outerAlpha) {
        int steps = Math.max(2, Mth.ceil((endRadians - beginRadians) / SUBDIVISION_RADIANS));
        float stepRadians = (endRadians - beginRadians) / steps;
        for (int step = 0; step < steps; step++) {
            float a0 = beginRadians + step * stepRadians;
            float a1 = a0 + stepRadians;
            float cos0 = Mth.cos(a0);
            float sin0 = Mth.sin(a0);
            float cos1 = Mth.cos(a1);
            float sin1 = Mth.sin(a1);
            buffer.vertex(pose, centerX + cos0 * innerRadius, centerY + sin0 * innerRadius, 0)
                    .color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.vertex(pose, centerX + cos1 * innerRadius, centerY + sin1 * innerRadius, 0)
                    .color(color[0], color[1], color[2], color[3]).endVertex();
            buffer.vertex(pose, centerX + cos1 * outerRadius, centerY + sin1 * outerRadius, 0)
                    .color(color[0], color[1], color[2], outerAlpha).endVertex();
            buffer.vertex(pose, centerX + cos0 * outerRadius, centerY + sin0 * outerRadius, 0)
                    .color(color[0], color[1], color[2], outerAlpha).endVertex();
        }
    }

    private void drawHubDisc(VertexConsumer buffer, Matrix4f pose, float centerX, float centerY) {
        int steps = 32;
        float stepRadians = (float) (Math.PI * 2) / steps;
        for (int step = 0; step < steps; step++) {
            float a0 = step * stepRadians;
            float a1 = a0 + stepRadians;
            float rim0X = centerX + Mth.cos(a0) * HUB_RADIUS;
            float rim0Y = centerY + Mth.sin(a0) * HUB_RADIUS;
            float rim1X = centerX + Mth.cos(a1) * HUB_RADIUS;
            float rim1Y = centerY + Mth.sin(a1) * HUB_RADIUS;
            buffer.vertex(pose, centerX, centerY, 0)
                    .color(HUB_COLOR[0], HUB_COLOR[1], HUB_COLOR[2], HUB_COLOR[3]).endVertex();
            buffer.vertex(pose, rim0X, rim0Y, 0)
                    .color(HUB_COLOR[0], HUB_COLOR[1], HUB_COLOR[2], HUB_COLOR[3] * 0.85f).endVertex();
            buffer.vertex(pose, rim1X, rim1Y, 0)
                    .color(HUB_COLOR[0], HUB_COLOR[1], HUB_COLOR[2], HUB_COLOR[3] * 0.85f).endVertex();
            buffer.vertex(pose, rim1X, rim1Y, 0)
                    .color(HUB_COLOR[0], HUB_COLOR[1], HUB_COLOR[2], HUB_COLOR[3] * 0.85f).endVertex();
        }
    }

    private void drawSectorContents(GuiGraphics guiGraphics, Minecraft minecraft, float centerX, float centerY,
                                    List<ActiveAbility> pageAbilities, AbilityData abilityData) {
        int sectorCount = pageAbilities.size();
        if (sectorCount == 0) {
            guiGraphics.drawCenteredString(minecraft.font,
                    NO_ABILITIES,
                    (int) centerX, (int) (centerY - OUTER_RADIUS - 16), 0xA0A0B0);
            return;
        }
        float sectorRadians = (float) (Math.PI * 2) / sectorCount;
        float contentRadius = (INNER_RADIUS + OUTER_RADIUS) / 2.0f;
        for (int sector = 0; sector < sectorCount; sector++) {
            float midRadians = sector * sectorRadians - Mth.HALF_PI;
            int contentX = (int) (centerX + Mth.cos(midRadians) * contentRadius);
            int contentY = (int) (centerY + Mth.sin(midRadians) * contentRadius);
            ActiveAbility ability = pageAbilities.get(sector);
            AbilityIcons.render(guiGraphics, minecraft.font, ability, contentX - 8, contentY - 8, 16);
            if (abilityData.isOnCooldown(ability) || abilityData.hasUnmetRequirements(ability)) {
                guiGraphics.fill(contentX + 7, contentY - 11, contentX + 11, contentY - 7, COOLDOWN_MARKER_COLOR);
            }
        }
    }

    private void drawHubContents(GuiGraphics guiGraphics, Minecraft minecraft, float centerX, float centerY,
                                 ActiveAbility selected) {
        if (selected != null) {
            AbilityIcons.render(guiGraphics, minecraft.font, selected,
                    (int) centerX - HUB_ICON_SIZE / 2, (int) centerY - HUB_ICON_SIZE / 2, HUB_ICON_SIZE);
        }
    }

    private void changePage(int delta) {
        pageIndex = Math.floorMod(pageIndex + delta, pageCount());
        hoveredIndex = -1;
        updatePageIndicator();
    }

    private int hoveredArrowDirection(double mouseX, double mouseY, float centerX, float centerY) {
        if (isInArrow(mouseX, mouseY, centerX - OUTER_RADIUS - PAGE_ARROW_MARGIN, centerY)) {
            return -1;
        }
        if (isInArrow(mouseX, mouseY, centerX + OUTER_RADIUS + PAGE_ARROW_MARGIN, centerY)) {
            return 1;
        }
        return 0;
    }

    private static boolean isInArrow(double mouseX, double mouseY, float arrowCenterX, float arrowCenterY) {
        return Math.abs(mouseX - arrowCenterX) <= PAGE_ARROW_HITBOX_HALF
                && Math.abs(mouseY - arrowCenterY) <= PAGE_ARROW_HITBOX_HALF;
    }

    private void drawPageArrow(VertexConsumer buffer, Matrix4f pose, float centerX, float centerY,
                               boolean pointsRight, boolean hovered) {
        float[] color = hovered ? PAGE_ARROW_HOVERED_COLOR : PAGE_ARROW_COLOR;
        float baseX = pointsRight ? centerX - PAGE_ARROW_HALF_WIDTH : centerX + PAGE_ARROW_HALF_WIDTH;
        float tipX = pointsRight ? centerX + PAGE_ARROW_HALF_WIDTH : centerX - PAGE_ARROW_HALF_WIDTH;
        buffer.vertex(pose, baseX, centerY - PAGE_ARROW_HALF_HEIGHT, 0).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(pose, baseX, centerY + PAGE_ARROW_HALF_HEIGHT, 0).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(pose, tipX, centerY, 0).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(pose, tipX, centerY, 0).color(color[0], color[1], color[2], color[3]).endVertex();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (pageCount() > 1 && ticksOpen - lastScrollPageTick >= SCROLL_PAGE_COOLDOWN_TICKS) {
            changePage(scrollY < 0 ? 1 : -1);
            lastScrollPageTick = ticksOpen;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && pageCount() > 1) {
            int arrow = hoveredArrowDirection(mouseX, mouseY, width / 2.0f, height / 2.0f);
            if (arrow != 0) {
                changePage(arrow);
                return true;
            }
        }
        if (button == 0) {
            hoveredIndex = computeHoveredIndex(currentPage().size(),
                    (float) (mouseX - width / 2.0), (float) (mouseY - height / 2.0));
            selectHoveredAndClose();
        } else if (button == 1) {
            onClose();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (AbilityKeyMappings.WHEEL.matchesMouse(button) && ticksOpen >= MIN_TICKS_BEFORE_RELEASE_SELECTS) {
            selectHoveredAndClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (AbilityKeyMappings.WHEEL.matches(keyCode, scanCode) && ticksOpen >= MIN_TICKS_BEFORE_RELEASE_SELECTS) {
            selectHoveredAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void selectHoveredAndClose() {
        List<ActiveAbility> pageAbilities = currentPage();
        if (hoveredIndex >= 0 && hoveredIndex < pageAbilities.size()) {
            AbilityClientAPI.select(pageAbilities.get(hoveredIndex));
        }
        onClose();
    }
}

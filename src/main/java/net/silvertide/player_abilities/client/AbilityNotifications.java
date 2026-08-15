package net.silvertide.player_abilities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.GatedAbility;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.data.AbilityData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class AbilityNotifications {
    private static final int ACTIVATED_DISPLAY_TICKS = 100;
    private static final int READY_DISPLAY_TICKS = 60;

    public static final class Notice {
        private final Ability ability;
        private final Component title;
        private final Component subtitle;
        private int displayTicksRemaining;

        private Notice(Ability ability, Component title, Component subtitle, int displayTicksRemaining) {
            this.ability = ability;
            this.title = title;
            this.subtitle = subtitle;
            this.displayTicksRemaining = displayTicksRemaining;
        }

        public Ability ability() {
            return ability;
        }

        public Component title() {
            return title;
        }

        public Component subtitle() {
            return subtitle;
        }
    }

    private static final List<Notice> ACTIVATED_NOTICES = new ArrayList<>();
    private static final List<Notice> READY_NOTICES = new ArrayList<>();

    private AbilityNotifications() {
    }

    public static void onTriggeredActivated(GatedAbility ability, int cooldownTicks) {
        Component title = Component.translatable("hud.player_abilities.triggered_activated", AbilityIcons.name(ability));
        Component subtitle = Component.translatable("hud.player_abilities.cooldown_duration",
                AbilityIcons.formatCooldown(cooldownTicks));
        ACTIVATED_NOTICES.add(new Notice(ability, title, subtitle, ACTIVATED_DISPLAY_TICKS));
    }

    public static void onAbilityReady(GatedAbility ability) {
        Component title = Component.translatable("hud.player_abilities.ability_ready", AbilityIcons.name(ability));
        READY_NOTICES.add(new Notice(ability, title, Component.empty(), READY_DISPLAY_TICKS));
    }

    public static List<Notice> activatedNotices() {
        return ACTIVATED_NOTICES;
    }

    public static List<Notice> readyNotices() {
        return READY_NOTICES;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            clearAll();
            return;
        }
        tickDown(ACTIVATED_NOTICES);
        tickDown(READY_NOTICES);
        if (!READY_NOTICES.isEmpty()) {
            AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
            READY_NOTICES.removeIf(notice -> !abilityData.isGranted(notice.ability()));
        }
    }

    private static void clearAll() {
        if (!ACTIVATED_NOTICES.isEmpty()) {
            ACTIVATED_NOTICES.clear();
        }
        if (!READY_NOTICES.isEmpty()) {
            READY_NOTICES.clear();
        }
    }

    private static void tickDown(List<Notice> notices) {
        if (notices.isEmpty()) {
            return;
        }
        Iterator<Notice> iterator = notices.iterator();
        while (iterator.hasNext()) {
            Notice notice = iterator.next();
            if (--notice.displayTicksRemaining <= 0) {
                iterator.remove();
            }
        }
    }
}

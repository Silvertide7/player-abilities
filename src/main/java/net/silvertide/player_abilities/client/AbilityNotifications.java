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
import java.util.Locale;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class AbilityNotifications {
    public static final int ACTIVATED_DISPLAY_TICKS = 100;
    public static final int READY_DISPLAY_TICKS = 60;

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

    private static final List<Notice> activatedNotices = new ArrayList<>();
    private static final List<Notice> readyNotices = new ArrayList<>();
    private static final List<GatedAbility> awaitingReady = new ArrayList<>();

    private AbilityNotifications() {
    }

    public static void onTriggeredActivated(GatedAbility ability, int cooldownTicks) {
        Component title = Component.translatable("hud.player_abilities.triggered_activated", AbilityIcons.name(ability));
        Component subtitle = Component.translatable("hud.player_abilities.cooldown_duration",
                String.format(Locale.ROOT, "%.0f", cooldownTicks / 20.0f));
        activatedNotices.add(new Notice(ability, title, subtitle, ACTIVATED_DISPLAY_TICKS));
        if (cooldownTicks > 0 && !awaitingReady.contains(ability)) {
            awaitingReady.add(ability);
        }
    }

    public static List<Notice> activatedNotices() {
        return activatedNotices;
    }

    public static List<Notice> readyNotices() {
        return readyNotices;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            clearAll();
            return;
        }
        tickDown(activatedNotices);
        tickDown(readyNotices);
        if (awaitingReady.isEmpty()) {
            return;
        }
        AbilityData abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        Iterator<GatedAbility> awaiting = awaitingReady.iterator();
        while (awaiting.hasNext()) {
            GatedAbility ability = awaiting.next();
            if (abilityData.getCooldown(ability).isEmpty() && !abilityData.hasUnmetRequirements(ability)) {
                Component title = Component.translatable("hud.player_abilities.ability_ready", AbilityIcons.name(ability));
                readyNotices.add(new Notice(ability, title, Component.empty(), READY_DISPLAY_TICKS));
                awaiting.remove();
            }
        }
    }

    private static void clearAll() {
        if (!activatedNotices.isEmpty()) {
            activatedNotices.clear();
        }
        if (!readyNotices.isEmpty()) {
            readyNotices.clear();
        }
        if (!awaitingReady.isEmpty()) {
            awaitingReady.clear();
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

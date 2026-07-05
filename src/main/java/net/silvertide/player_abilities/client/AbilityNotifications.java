package net.silvertide.player_abilities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.GatedAbility;
import net.silvertide.player_abilities.data.AbilityAttachments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class AbilityNotifications {
    public static final int ACTIVATED_DISPLAY_TICKS = 100;
    public static final int READY_DISPLAY_TICKS = 60;

    public static final class Notice {
        private final Ability ability;
        private final int cooldownTicks;
        private int displayTicksRemaining;

        private Notice(Ability ability, int cooldownTicks, int displayTicksRemaining) {
            this.ability = ability;
            this.cooldownTicks = cooldownTicks;
            this.displayTicksRemaining = displayTicksRemaining;
        }

        public Ability ability() {
            return ability;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }
    }

    private static final List<Notice> activatedNotices = new ArrayList<>();
    private static final List<Notice> readyNotices = new ArrayList<>();
    private static final List<Ability> awaitingReady = new ArrayList<>();

    private AbilityNotifications() {
    }

    public static void onTriggeredActivated(Ability ability, int cooldownTicks) {
        activatedNotices.add(new Notice(ability, cooldownTicks, ACTIVATED_DISPLAY_TICKS));
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
        tickDown(activatedNotices);
        tickDown(readyNotices);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || awaitingReady.isEmpty()) {
            return;
        }
        var abilityData = player.getData(AbilityAttachments.ABILITY_DATA);
        Iterator<Ability> awaiting = awaitingReady.iterator();
        while (awaiting.hasNext()) {
            Ability ability = awaiting.next();
            if (ability instanceof GatedAbility gated && abilityData.getCooldown(gated).isEmpty()) {
                readyNotices.add(new Notice(ability, 0, READY_DISPLAY_TICKS));
                awaiting.remove();
            }
        }
    }

    private static void tickDown(List<Notice> notices) {
        Iterator<Notice> iterator = notices.iterator();
        while (iterator.hasNext()) {
            Notice notice = iterator.next();
            if (--notice.displayTicksRemaining <= 0) {
                iterator.remove();
            }
        }
    }
}

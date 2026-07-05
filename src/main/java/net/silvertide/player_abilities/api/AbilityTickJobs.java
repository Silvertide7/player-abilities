package net.silvertide.player_abilities.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class AbilityTickJobs {
    public interface TickJob {
        boolean tick(ServerPlayer player);
    }

    public interface JobHandle {
        void cancel();
    }

    private static final class ScheduledJob implements JobHandle {
        private final TickJob job;
        private final int intervalTicks;
        private int ticksUntilRun = 1;
        private volatile boolean cancelled;

        private ScheduledJob(TickJob job, int intervalTicks) {
            this.job = job;
            this.intervalTicks = intervalTicks;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private static final Map<UUID, List<ScheduledJob>> JOBS_BY_PLAYER = new HashMap<>();

    private AbilityTickJobs() {
    }

    public static JobHandle schedule(ServerPlayer player, int intervalTicks, TickJob job) {
        ScheduledJob scheduled = new ScheduledJob(job, Math.max(1, intervalTicks));
        JOBS_BY_PLAYER.computeIfAbsent(player.getUUID(), key -> new ArrayList<>()).add(scheduled);
        return scheduled;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS_BY_PLAYER.isEmpty()) {
            return;
        }
        for (UUID playerId : List.copyOf(JOBS_BY_PLAYER.keySet())) {
            List<ScheduledJob> jobs = JOBS_BY_PLAYER.get(playerId);
            if (jobs == null) {
                continue;
            }
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.hasDisconnected() || player.isRemoved()) {
                JOBS_BY_PLAYER.remove(playerId);
                continue;
            }
            for (int i = 0; i < jobs.size(); i++) {
                ScheduledJob scheduled = jobs.get(i);
                if (scheduled.cancelled || --scheduled.ticksUntilRun > 0) {
                    continue;
                }
                scheduled.ticksUntilRun = scheduled.intervalTicks;
                if (!scheduled.job.tick(player)) {
                    scheduled.cancelled = true;
                }
            }
            jobs.removeIf(scheduled -> scheduled.cancelled);
            if (jobs.isEmpty()) {
                JOBS_BY_PLAYER.remove(playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        JOBS_BY_PLAYER.clear();
    }
}

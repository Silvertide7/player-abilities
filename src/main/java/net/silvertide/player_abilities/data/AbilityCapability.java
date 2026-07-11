package net.silvertide.player_abilities.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.player_abilities.PlayerAbilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class AbilityCapability {
    public static final Capability<AbilityData> ABILITY_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    private static final net.minecraft.resources.ResourceLocation ID = PlayerAbilities.id("ability_data");

    public static AbilityData get(Player player) {
        return player.getCapability(ABILITY_DATA).orElseThrow(
                () -> new IllegalStateException("Player " + player.getGameProfile().getName() + " is missing ability data"));
    }

    public static void copy(Player from, Player to) {
        from.reviveCaps();
        get(to).loadFrom(get(from));
        from.invalidateCaps();
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider provider = new Provider();
            event.addCapability(ID, provider);
            event.addListener(provider.optional::invalidate);
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final AbilityData data = new AbilityData();
        private final LazyOptional<AbilityData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            return capability == ABILITY_DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) AbilityData.CODEC.encodeStart(NbtOps.INSTANCE, data)
                    .resultOrPartial(PlayerAbilities.LOGGER::error)
                    .orElseGet(CompoundTag::new);
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            AbilityData.CODEC.parse(NbtOps.INSTANCE, tag)
                    .resultOrPartial(PlayerAbilities.LOGGER::error)
                    .ifPresent(data::loadFrom);
        }
    }

    private AbilityCapability() {
    }
}

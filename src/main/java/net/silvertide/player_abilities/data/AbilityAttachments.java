package net.silvertide.player_abilities.data;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.function.Supplier;

public final class AbilityAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PlayerAbilities.MOD_ID);

    public static final Supplier<AttachmentType<AbilityData>> ABILITY_DATA = ATTACHMENT_TYPES.register(
            "ability_data",
            () -> AttachmentType.builder(AbilityData::new).serialize(AbilityData.CODEC).copyOnDeath().build());

    private AbilityAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}

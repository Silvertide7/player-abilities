package net.silvertide.player_abilities.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record SyncAbilitiesPayload(int playerEntityId, Map<ResourceLocation, Integer> grantedLevels,
                                   Set<ResourceLocation> disabledPassiveIds,
                                   Optional<ResourceLocation> selectedId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerEntityId);
        buf.writeMap(grantedLevels, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarInt);
        buf.writeCollection(disabledPassiveIds, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(selectedId, FriendlyByteBuf::writeResourceLocation);
    }

    public static SyncAbilitiesPayload decode(FriendlyByteBuf buf) {
        return new SyncAbilitiesPayload(
                buf.readVarInt(),
                buf.readMap(HashMap::new, FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarInt),
                buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation),
                buf.readOptional(FriendlyByteBuf::readResourceLocation));
    }
}

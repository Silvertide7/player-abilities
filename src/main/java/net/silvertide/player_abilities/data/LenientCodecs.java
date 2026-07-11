package net.silvertide.player_abilities.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LenientCodecs {
    public static <V> Codec<Map<ResourceLocation, V>> resourceLocationMap(Codec<V> valueCodec, String context) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<ResourceLocation, V>, T>> decode(DynamicOps<T> ops, T input) {
                Map<ResourceLocation, V> decoded = new LinkedHashMap<>();
                var asMap = ops.getMap(input);
                if (asMap.result().isEmpty()) {
                    PlayerAbilities.LOGGER.warn("Discarding {} data that is not a map", context);
                    return DataResult.success(Pair.of(decoded, input));
                }
                asMap.result().get().entries().forEach(entry -> {
                    ResourceLocation key = ops.getStringValue(entry.getFirst()).result()
                            .map(ResourceLocation::tryParse).orElse(null);
                    if (key == null) {
                        PlayerAbilities.LOGGER.warn("Skipping {} entry with invalid key {}", context, entry.getFirst());
                        return;
                    }
                    valueCodec.parse(ops, entry.getSecond())
                            .resultOrPartial(error -> PlayerAbilities.LOGGER.warn(
                                    "Skipping {} entry {}: {}", context, key, error))
                            .ifPresent(value -> decoded.put(key, value));
                });
                return DataResult.success(Pair.of(decoded, input));
            }

            @Override
            public <T> DataResult<T> encode(Map<ResourceLocation, V> input, DynamicOps<T> ops, T prefix) {
                return Codec.unboundedMap(ResourceLocation.CODEC, valueCodec).encode(input, ops, prefix);
            }
        };
    }

    public static <V> Codec<List<V>> list(Codec<V> elementCodec, String context) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<List<V>, T>> decode(DynamicOps<T> ops, T input) {
                List<V> decoded = new ArrayList<>();
                var asStream = ops.getStream(input);
                if (asStream.result().isEmpty()) {
                    PlayerAbilities.LOGGER.warn("Discarding {} data that is not a list", context);
                    return DataResult.success(Pair.of(decoded, input));
                }
                asStream.result().get().forEach(element ->
                        elementCodec.parse(ops, element)
                                .resultOrPartial(error -> PlayerAbilities.LOGGER.warn(
                                        "Skipping {} entry: {}", context, error))
                                .ifPresent(decoded::add));
                return DataResult.success(Pair.of(decoded, input));
            }

            @Override
            public <T> DataResult<T> encode(List<V> input, DynamicOps<T> ops, T prefix) {
                return elementCodec.listOf().encode(input, ops, prefix);
            }
        };
    }

    private LenientCodecs() {}
}

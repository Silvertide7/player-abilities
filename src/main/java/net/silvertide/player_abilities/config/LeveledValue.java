package net.silvertide.player_abilities.config;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;

import java.util.List;

public record LeveledValue<T>(List<T> perLevel) {
    public static <T> Codec<LeveledValue<T>> codec(Codec<T> elementCodec) {
        return Codec.either(elementCodec, elementCodec.listOf(1, Integer.MAX_VALUE)).xmap(
                either -> either.map(single -> new LeveledValue<>(List.of(single)), LeveledValue::new),
                leveledValue -> leveledValue.perLevel().size() == 1
                        ? Either.left(leveledValue.perLevel().getFirst())
                        : Either.right(leveledValue.perLevel()));
    }

    public T resolve(int level) {
        return perLevel.get(Mth.clamp(level, 1, perLevel.size()) - 1);
    }
}

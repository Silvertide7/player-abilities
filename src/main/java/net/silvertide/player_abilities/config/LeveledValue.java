package net.silvertide.player_abilities.config;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

import java.util.List;

public record LeveledValue<T>(List<T> perLevel) {
    public static <T> Codec<LeveledValue<T>> codec(Codec<T> elementCodec) {
        return Codec.either(elementCodec, elementCodec.listOf()).comapFlatMap(
                either -> either.map(
                        single -> DataResult.success(new LeveledValue<>(List.of(single))),
                        list -> list.isEmpty()
                                ? DataResult.error(() -> "Leveled value list must not be empty")
                                : DataResult.success(new LeveledValue<>(list))),
                leveledValue -> leveledValue.perLevel().size() == 1
                        ? Either.left(leveledValue.perLevel().get(0))
                        : Either.right(leveledValue.perLevel()));
    }

    public T resolve(int level) {
        return perLevel.get(Mth.clamp(level, 1, perLevel.size()) - 1);
    }
}

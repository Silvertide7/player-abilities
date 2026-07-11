package net.silvertide.player_abilities.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.silvertide.player_abilities.PlayerAbilities;
import org.jetbrains.annotations.Nullable;

public record AbilityBookContent(ResourceLocation abilityId, int level) {
    private static final String TAG_KEY = "ability_book_content";

    public static final Codec<AbilityBookContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("ability").forGetter(AbilityBookContent::abilityId),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("level", 1).forGetter(AbilityBookContent::level)
    ).apply(instance, AbilityBookContent::new));

    @Nullable
    public static AbilityBookContent of(ItemStack stack) {
        if (stack.getTag() == null || !stack.getTag().contains(TAG_KEY)) {
            return null;
        }
        return CODEC.parse(NbtOps.INSTANCE, stack.getTag().get(TAG_KEY))
                .resultOrPartial(PlayerAbilities.LOGGER::error)
                .orElse(null);
    }

    public void applyTo(ItemStack stack) {
        Tag encoded = CODEC.encodeStart(NbtOps.INSTANCE, this)
                .resultOrPartial(PlayerAbilities.LOGGER::error)
                .orElse(null);
        if (encoded != null) {
            stack.getOrCreateTag().put(TAG_KEY, encoded);
        }
    }
}

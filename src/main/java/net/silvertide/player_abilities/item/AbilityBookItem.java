package net.silvertide.player_abilities.item;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.api.TriggeredAbility;
import net.silvertide.player_abilities.config.AbilityConfigs;

import java.util.List;

public class AbilityBookItem extends Item {
    public static final ResourceLocation BOOK_SOURCE = PlayerAbilities.id("book");

    public AbilityBookItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFor(Ability ability, int level) {
        ItemStack stack = new ItemStack(AbilityItems.ABILITY_BOOK.get());
        stack.set(AbilityItems.ABILITY_BOOK_CONTENT.get(), new AbilityBookContent(ability.getId(), level));
        return stack;
    }

    public static String kindTranslationKey(Ability ability) {
        if (ability instanceof TriggeredAbility) {
            return "gui.player_abilities.section_triggered";
        }
        if (ability instanceof PassiveAbility) {
            return "gui.player_abilities.section_passive";
        }
        return "gui.player_abilities.section_active";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        AbilityBookContent content = stack.get(AbilityItems.ABILITY_BOOK_CONTENT.get());
        if (content == null) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        Ability ability = AbilityRegistry.ABILITIES.get(content.abilityId());
        if (ability == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.player_abilities.book_unknown_ability", content.abilityId().toString()), true);
            return InteractionResultHolder.fail(stack);
        }
        Component abilityName = Component.translatable(ability.getDescriptionId());
        if (!AbilityConfigs.isEnabled(ability)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.player_abilities.book_ability_disabled", abilityName), true);
            return InteractionResultHolder.fail(stack);
        }
        int grantLevel = Math.min(content.level(), AbilityConfigs.maxLevel(ability));
        if (AbilityAPI.getLevel(serverPlayer, ability) >= grantLevel) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.player_abilities.book_already_known", abilityName), true);
            return InteractionResultHolder.fail(stack);
        }
        AbilityAPI.grant(serverPlayer, BOOK_SOURCE, ability, grantLevel);
        serverPlayer.displayClientMessage(
                Component.translatable("message.player_abilities.book_learned", abilityName), true);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        AbilityBookContent content = stack.get(AbilityItems.ABILITY_BOOK_CONTENT.get());
        if (content != null) {
            Ability ability = AbilityRegistry.ABILITIES.get(content.abilityId());
            if (ability != null) {
                return Component.translatable("item.player_abilities.ability_book.named",
                        Component.translatable(ability.getDescriptionId()));
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AbilityBookContent content = stack.get(AbilityItems.ABILITY_BOOK_CONTENT.get());
        if (content == null) {
            return;
        }
        Ability ability = AbilityRegistry.ABILITIES.get(content.abilityId());
        if (ability == null) {
            tooltip.add(Component.literal(content.abilityId().toString()).withStyle(ChatFormatting.DARK_RED));
            return;
        }
        tooltip.add(Component.translatable(kindTranslationKey(ability)).withStyle(ChatFormatting.AQUA));
        if (content.level() > 1) {
            tooltip.add(Component.translatable("item.player_abilities.ability_book.level", content.level())
                    .withStyle(ChatFormatting.GRAY));
        }
        String descriptionKey = ability.getDescriptionId() + ".description";
        if (Language.getInstance().has(descriptionKey)) {
            tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
        }
    }
}

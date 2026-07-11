package net.silvertide.player_abilities.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;
import net.silvertide.player_abilities.api.AbilityAPI;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.config.AbilityConfigs;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID)
public final class AbilityCommands {
    public static final ResourceLocation DEFAULT_COMMAND_SOURCE = PlayerAbilities.id("command");

    private static final DynamicCommandExceptionType UNKNOWN_ABILITY =
            new DynamicCommandExceptionType(abilityId -> Component.literal("Unknown ability: " + abilityId));

    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(AbilityRegistry.abilities().getKeys(), builder);

    private AbilityCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("playerabilities")
                .requires(stack -> stack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests(ABILITY_SUGGESTIONS)
                                        .executes(context -> grant(context, AbilityAPI.MIN_LEVEL, DEFAULT_COMMAND_SOURCE))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(AbilityAPI.MIN_LEVEL))
                                                .executes(context -> grant(context, IntegerArgumentType.getInteger(context, "level"), DEFAULT_COMMAND_SOURCE))
                                                .then(Commands.argument("source", ResourceLocationArgument.id())
                                                        .executes(context -> grant(context, IntegerArgumentType.getInteger(context, "level"),
                                                                ResourceLocationArgument.getId(context, "source"))))))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests(ABILITY_SUGGESTIONS)
                                        .executes(context -> revoke(context, DEFAULT_COMMAND_SOURCE))
                                        .then(Commands.argument("source", ResourceLocationArgument.id())
                                                .executes(context -> revoke(context, ResourceLocationArgument.getId(context, "source"))))))));
    }

    private static int grant(CommandContext<CommandSourceStack> context, int level, ResourceLocation grantSource) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Ability ability = resolveAbility(context);
        int grantedLevel = Math.min(level, AbilityConfigs.maxLevel(ability));
        if (AbilityAPI.grant(player, grantSource, ability, grantedLevel)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Granted " + ability.getId() + " level " + grantedLevel + " to " + player.getName().getString() + " (source: " + grantSource + ")"), true);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFailure(Component.literal(
                player.getName().getString() + " already has " + ability.getId() + " at level " + grantedLevel + " from source " + grantSource));
        return 0;
    }

    private static int revoke(CommandContext<CommandSourceStack> context, ResourceLocation grantSource) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Ability ability = resolveAbility(context);
        if (AbilityAPI.revoke(player, grantSource, ability)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Revoked " + ability.getId() + " from " + player.getName().getString() + " (source: " + grantSource + ")"), true);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFailure(Component.literal(
                player.getName().getString() + " does not have " + ability.getId() + " from source " + grantSource));
        return 0;
    }

    private static Ability resolveAbility(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation abilityId = ResourceLocationArgument.getId(context, "ability");
        Ability ability = AbilityRegistry.abilities().getValue(abilityId);
        if (ability == null) {
            throw UNKNOWN_ABILITY.create(abilityId);
        }
        return ability;
    }
}

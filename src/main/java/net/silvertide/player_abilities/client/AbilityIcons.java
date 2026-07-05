package net.silvertide.player_abilities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.silvertide.player_abilities.PlayerAbilities;
import net.silvertide.player_abilities.api.Ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT)
public final class AbilityIcons {
    private static final Map<Ability, Optional<ResourceLocation>> ICON_CACHE = new HashMap<>();
    private static final Map<Ability, String> INITIALS_CACHE = new HashMap<>();
    private static final Map<Ability, Component> NAME_CACHE = new HashMap<>();
    private static final Map<LeveledNameKey, Component> LEVELED_NAME_CACHE = new HashMap<>();
    private static final Map<Ability, Optional<Component>> DESCRIPTION_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, String> CATEGORY_LABEL_CACHE = new HashMap<>();

    private AbilityIcons() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            ICON_CACHE.clear();
            DESCRIPTION_CACHE.clear();
            CATEGORY_LABEL_CACHE.clear();
        });
    }

    public static Optional<ResourceLocation> icon(Ability ability) {
        return ICON_CACHE.computeIfAbsent(ability, keyedAbility -> {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                    keyedAbility.getId().getNamespace(),
                    "textures/ability/" + keyedAbility.getId().getPath() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
                    ? Optional.of(texture) : Optional.empty();
        });
    }

    public static String initials(Ability ability) {
        return INITIALS_CACHE.computeIfAbsent(ability, keyedAbility -> {
            String[] words = keyedAbility.getId().getPath().split("_");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(2, words.length); i++) {
                if (!words[i].isEmpty()) {
                    initials.append(Character.toUpperCase(words[i].charAt(0)));
                }
            }
            return initials.toString();
        });
    }

    public static Component name(Ability ability) {
        return NAME_CACHE.computeIfAbsent(ability, keyedAbility -> Component.translatable(keyedAbility.getDescriptionId()));
    }

    public static String categoryLabel(ResourceLocation category) {
        return CATEGORY_LABEL_CACHE.computeIfAbsent(category, key -> {
            String translationKey = "ability_category." + key.getNamespace() + "." + key.getPath().replace('/', '.');
            if (I18n.exists(translationKey)) {
                return I18n.get(translationKey);
            }
            String[] words = key.getPath().replace('/', '_').split("_");
            StringBuilder label = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                if (!label.isEmpty()) {
                    label.append(' ');
                }
                label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return label.toString();
        });
    }

    public static Optional<Component> description(Ability ability) {
        return DESCRIPTION_CACHE.computeIfAbsent(ability, keyedAbility -> {
            String descriptionKey = keyedAbility.getDescriptionId() + ".description";
            return I18n.exists(descriptionKey)
                    ? Optional.of(Component.translatable(descriptionKey))
                    : Optional.empty();
        });
    }

    public static Component nameWithLevel(Ability ability, int level) {
        if (level <= 1) {
            return name(ability);
        }
        return LEVELED_NAME_CACHE.computeIfAbsent(new LeveledNameKey(ability, level),
                key -> name(key.ability()).copy().append(" " + key.level()));
    }

    public static void render(GuiGraphics guiGraphics, Font font, Ability ability, int x, int y, int size) {
        Optional<ResourceLocation> icon = icon(ability);
        if (icon.isPresent()) {
            guiGraphics.blit(icon.get(), x, y, 0, 0, size, size, size, size);
        } else {
            guiGraphics.drawCenteredString(font, initials(ability), x + size / 2, y + (size - font.lineHeight) / 2 + 1, 0xFFFFFF);
        }
    }

    private record LeveledNameKey(Ability ability, int level) {
    }
}

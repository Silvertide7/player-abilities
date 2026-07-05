package net.silvertide.player_abilities;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.silvertide.player_abilities.api.AbilityAttributes;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.compat.pmmo.PmmoCompat;
import net.silvertide.player_abilities.compat.puffish_skills.PuffishSkillsCompat;
import net.silvertide.player_abilities.config.AbilityClientConfig;
import net.silvertide.player_abilities.content.BuiltInAbilities;
import net.silvertide.player_abilities.data.AbilityAttachments;
import org.slf4j.Logger;

@Mod(PlayerAbilities.MOD_ID)
public class PlayerAbilities {
    public static final String MOD_ID = "player_abilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public PlayerAbilities(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, AbilityClientConfig.SPEC);
        AbilityRegistry.register(modEventBus);
        AbilityAttachments.register(modEventBus);
        AbilityAttributes.register(modEventBus);
        BuiltInAbilities.register(modEventBus);
        if (ModList.get().isLoaded("pmmo")) {
            PmmoCompat.init();
        }
        if (ModList.get().isLoaded("puffish_skills")) {
            PuffishSkillsCompat.init();
        }
    }
}

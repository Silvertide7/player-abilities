package net.silvertide.player_abilities;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silvertide.player_abilities.api.AbilityAttributes;
import net.silvertide.player_abilities.api.AbilityRegistry;
import net.silvertide.player_abilities.compat.pmmo.PmmoCompat;
import net.silvertide.player_abilities.compat.puffish_skills.PuffishSkillsCompat;
import net.silvertide.player_abilities.config.AbilityClientConfig;
import net.silvertide.player_abilities.item.AbilityItems;
import net.silvertide.player_abilities.network.AbilityNetworking;
import org.slf4j.Logger;

@Mod(PlayerAbilities.MOD_ID)
public class PlayerAbilities {
    public static final String MOD_ID = "player_abilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public PlayerAbilities() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AbilityClientConfig.SPEC);
        AbilityNetworking.register();
        AbilityRegistry.register(modEventBus);
        AbilityAttributes.register(modEventBus);
        AbilityItems.register(modEventBus);
        if (ModList.get().isLoaded("pmmo")) {
            PmmoCompat.init();
        }
        if (ModList.get().isLoaded("puffish_skills")) {
            PuffishSkillsCompat.init();
        }
    }
}

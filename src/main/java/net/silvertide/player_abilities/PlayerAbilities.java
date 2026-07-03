package net.silvertide.player_abilities;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(PlayerAbilities.MOD_ID)
public class PlayerAbilities {
    public static final String MOD_ID = "player_abilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlayerAbilities(IEventBus modEventBus, ModContainer modContainer) {
    }
}

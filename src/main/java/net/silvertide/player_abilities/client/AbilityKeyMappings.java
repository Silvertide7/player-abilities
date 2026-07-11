package net.silvertide.player_abilities.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.silvertide.player_abilities.PlayerAbilities;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = PlayerAbilities.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AbilityKeyMappings {
    public static final String CATEGORY = "key.categories.player_abilities";
    public static final KeyMapping USE = new KeyMapping("key.player_abilities.use",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping CYCLE = new KeyMapping("key.player_abilities.cycle",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping WHEEL = new KeyMapping("key.player_abilities.wheel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping BOOK = new KeyMapping("key.player_abilities.book",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);

    private AbilityKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(USE);
        event.register(CYCLE);
        event.register(WHEEL);
        event.register(BOOK);
    }
}

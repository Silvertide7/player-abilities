package net.silvertide.player_abilities.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AbilityClientConfig {
    public enum HudDisplay {
        ALWAYS,
        CONTEXTUAL,
        MINIMIZE,
        NEVER
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue WHEEL_GROUP_BY_CATEGORY;
    public static final ModConfigSpec.EnumValue<HudDisplay> HUD_DISPLAY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        WHEEL_GROUP_BY_CATEGORY = builder
                .comment("Group the ability wheel's pages by ability category (true) or page through all abilities in chunks (false).")
                .define("wheel_group_by_category", true);
        HUD_DISPLAY = builder
                .comment("Ability HUD visibility:",
                        "ALWAYS - always shown in full (icon, name, cooldown/status).",
                        "CONTEXTUAL - full HUD shown for 15s after selecting or using an ability, and while on cooldown.",
                        "MINIMIZE - only the icon and its cooldown shading, always shown.",
                        "NEVER - hidden.")
                .defineEnum("hud_display", HudDisplay.ALWAYS);
        SPEC = builder.build();
    }

    private AbilityClientConfig() {
    }
}

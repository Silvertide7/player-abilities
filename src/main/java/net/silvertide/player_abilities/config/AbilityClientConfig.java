package net.silvertide.player_abilities.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AbilityClientConfig {
    public enum HudDisplay {
        ALWAYS,
        CONTEXTUAL,
        MINIMIZE,
        NEVER
    }

    public enum HudPosition {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        TOP_RIGHT;

        public boolean isRight() {
            return this == BOTTOM_RIGHT || this == TOP_RIGHT;
        }

        public boolean isTop() {
            return this == TOP_LEFT || this == TOP_RIGHT;
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue WHEEL_GROUP_BY_CATEGORY;
    public static final ForgeConfigSpec.EnumValue<HudDisplay> HUD_DISPLAY;
    public static final ForgeConfigSpec.EnumValue<HudPosition> HUD_POSITION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        WHEEL_GROUP_BY_CATEGORY = builder
                .comment("Group the ability wheel's pages by ability category (true) or page through all abilities in chunks (false).")
                .define("wheel_group_by_category", true);
        HUD_DISPLAY = builder
                .comment("Ability HUD visibility:",
                        "ALWAYS - always shown in full (icon, name, cooldown/status).",
                        "CONTEXTUAL - full HUD shown for 15s after selecting or using an ability, hidden otherwise.",
                        "MINIMIZE - full HUD for 15s after selecting or using an ability, then just the icon with its cooldown shading.",
                        "NEVER - hidden (the progress bar still shows while an ability is in use).")
                .defineEnum("hud_display", HudDisplay.ALWAYS);
        HUD_POSITION = builder
                .comment("Screen corner for the ability HUD (selected ability, active effects, ready notices).")
                .defineEnum("hud_position", HudPosition.BOTTOM_LEFT);
        SPEC = builder.build();
    }

    private AbilityClientConfig() {
    }
}

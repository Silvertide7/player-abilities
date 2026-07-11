package net.silvertide.player_abilities.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.silvertide.player_abilities.network.AbilityNetworking;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.api.PassiveAbility;
import net.silvertide.player_abilities.data.AbilityCapability;
import net.silvertide.player_abilities.network.UseAbilityPayload;
import net.silvertide.player_abilities.network.SelectAbilityPayload;
import net.silvertide.player_abilities.network.TogglePassivePayload;

public final class AbilityClientAPI {
    private AbilityClientAPI() {
    }

    public static void select(ActiveAbility ability) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        AbilityCapability.get(player).setSelected(ability);
        AbilityNetworking.sendToServer(new SelectAbilityPayload(ability.getId()));
    }

    public static void use() {
        AbilityNetworking.sendToServer(new UseAbilityPayload());
    }

    public static void togglePassive(PassiveAbility passive) {
        AbilityNetworking.sendToServer(new TogglePassivePayload(passive.getId()));
    }
}

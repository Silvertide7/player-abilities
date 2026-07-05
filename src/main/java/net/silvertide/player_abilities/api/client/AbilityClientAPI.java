package net.silvertide.player_abilities.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.player_abilities.api.ActiveAbility;
import net.silvertide.player_abilities.data.AbilityAttachments;
import net.silvertide.player_abilities.network.CastAbilityPayload;
import net.silvertide.player_abilities.network.SelectAbilityPayload;

public final class AbilityClientAPI {
    private AbilityClientAPI() {
    }

    public static void select(ActiveAbility ability) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getData(AbilityAttachments.ABILITY_DATA).setSelected(ability);
        PacketDistributor.sendToServer(new SelectAbilityPayload(ability.getId()));
    }

    public static void cast() {
        PacketDistributor.sendToServer(CastAbilityPayload.INSTANCE);
    }
}

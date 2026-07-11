package net.silvertide.player_abilities.dev;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.silvertide.player_abilities.api.AttributeGrant;
import net.silvertide.player_abilities.api.PassiveAbility;

import java.util.List;

public final class SwiftStepAbility extends PassiveAbility {
    private static final double SPEED_BONUS_PER_LEVEL = 0.2;

    @Override
    public List<AttributeGrant> getAttributeGrants(int level) {
        return List.of(new AttributeGrant(Attributes.MOVEMENT_SPEED,
                SPEED_BONUS_PER_LEVEL * level, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}

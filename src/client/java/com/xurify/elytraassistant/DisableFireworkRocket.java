package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

public class DisableFireworkRocket {
    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack heldItem = player.getStackInHand(hand);
            boolean isWearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
            boolean isHoldingFireworkRocket = heldItem.getItem() == Items.FIREWORK_ROCKET;

            if (!isHoldingFireworkRocket) {
                return ActionResult.PASS;
            }

            if (ElytraAssistant.CONFIG.fireworkRockets.disableDecorativeExplosionsWhileWearingElytra && isWearingElytra) {
                return ActionResult.FAIL;
            }

            if (ElytraAssistant.CONFIG.fireworkRockets.disableDecorativeExplosionsCompletely) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}

package com.xurify.elytraassistant;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DisableFireworkRocket {
  public static void init() {
    UseItemCallback.EVENT.register(
        (player, world, hand) ->
            shouldBlockFireworkUse(player, player.getItemInHand(hand))
                ? InteractionResult.FAIL
                : InteractionResult.PASS);

    UseBlockCallback.EVENT.register(
        (player, world, hand, hitResult) -> {
          ItemStack heldItem = player.getItemInHand(hand);

          if (heldItem.getItem() != Items.FIREWORK_ROCKET) {
            return InteractionResult.PASS;
          }

          return shouldBlockFireworkUse(player, heldItem)
              ? InteractionResult.FAIL
              : InteractionResult.PASS;
        });

    UseEntityCallback.EVENT.register(
        (player, world, hand, entity, hitResult) ->
            shouldBlockFireworkUse(player, player.getItemInHand(hand))
                ? InteractionResult.FAIL
                : InteractionResult.PASS);
  }

  private static boolean shouldBlockFireworkUse(
      net.minecraft.world.entity.player.Player player, ItemStack heldItem) {
    return heldItem.getItem() == Items.FIREWORK_ROCKET && shouldRestrictFireworkUse(player);
  }

  private static boolean shouldRestrictFireworkUse(
      net.minecraft.world.entity.player.Player player) {
    if (player.isFallFlying()) {
      return false;
    }

    return switch (ElytraAssistant.CONFIG.fireworkRockets.restriction) {
      case OFF -> false;
      case WHILE_WEARING_ELYTRA ->
          player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
      case FLIGHT_ONLY -> true;
    };
  }
}

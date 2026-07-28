package com.xurify.elytraassistant;

import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
// import net.minecraft.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class ElytraInfoTooltip {

  public static void init() {
    ItemTooltipCallback.EVENT.register(
        (stack, context, lines, list) -> {
          if (stack.getItem() == Items.ELYTRA
              && ElytraAssistant.CONFIG.displaySettings.showFlightTime) {
            addElytraTooltip(stack, list, context);
          }
        });
  }

  private static void addElytraTooltip(
      ItemStack itemStack, List<Component> tooltipList, Item.TooltipContext context) {
    int maxDurability = itemStack.getMaxDamage();
    int currentDurability = maxDurability - itemStack.getDamageValue();
    float durabilityPercentage = (float) currentDurability / maxDurability;

    var unbreakingEnchantment =
        context
            .registries()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.UNBREAKING);
    int unbreakingLevel =
        EnchantmentHelper.getItemEnchantmentLevel(unbreakingEnchantment, itemStack);

    float durabilityMultiplier = calculateUnbreakingMultiplier(unbreakingLevel);
    int estimatedSeconds = Math.round(currentDurability * durabilityMultiplier);
    String timeString = formatTime(estimatedSeconds);

    ChatFormatting timeColor = getColorForFlightTime(durabilityPercentage, unbreakingLevel);
    MutableComponent flightTimeText = Component.literal(timeString).withStyle(timeColor);
    tooltipList.add(
        Component.translatable("elytraassistant.tooltip.flightTime", flightTimeText)
            .withStyle(ChatFormatting.GRAY));
  }

  private static float calculateUnbreakingMultiplier(int level) {
    if (level <= 0) return 1.0f;
    return (float) (level + 1) / (float) (level * 0.25 + 0.75);
  }

  private static String formatTime(int totalSeconds) {
    int hours = totalSeconds / 3600;
    int minutes = (totalSeconds % 3600) / 60;
    int seconds = totalSeconds % 60;

    if (hours > 0) {
      return String.format("%d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format("%d:%02d", minutes, seconds);
    }
  }

  private static ChatFormatting getColorForFlightTime(
      float durabilityPercentage, int unbreakingLevel) {
    float effectiveDurability = durabilityPercentage * (1 + unbreakingLevel * 0.5f);

    if (effectiveDurability > 0.75f) return ChatFormatting.GREEN;
    if (effectiveDurability > 0.6f) return ChatFormatting.YELLOW;
    if (effectiveDurability > 0.4f) return ChatFormatting.GOLD;
    if (effectiveDurability > 0.25f) return ChatFormatting.RED;
    return ChatFormatting.DARK_RED;
  }
}
